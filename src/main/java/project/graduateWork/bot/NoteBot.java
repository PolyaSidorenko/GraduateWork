package project.graduateWork.bot;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import project.graduateWork.entity.Note;
import project.graduateWork.entity.ReminderNotification;
import project.graduateWork.entity.Task;
import project.graduateWork.service.NoteService;
import project.graduateWork.service.ReminderService;
import project.graduateWork.service.TaskService;
import project.graduateWork.service.UserService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoteBot extends TelegramLongPollingBot{


    private final UserService userService;
    private final TaskService taskService;
    private final ReminderService reminderService;
    private final Dotenv dotenv;
    private final Map<Long, BotState> userStates = new HashMap<>();
    private final NoteService noteService;

    @Override
    public String getBotUsername() {
        return dotenv.get("TELEGRAM_BOT_USERNAME");
    }

    @Override
    public String getBotToken() {
        return dotenv.get("TELEGRAM_BOT_TOKEN");
    }

    private void handleCommand(Long chatId, User telegramUser, String message) {
        switch (message) {
            case "/start" -> handleStart(chatId, telegramUser);
            case "/addTask" -> handleAddTask(chatId);
            case "/addReminder" -> handleAddReminder(chatId);
            case "/addNote" -> handleAddNote(chatId);
            default -> sendMessage(chatId, "Неизвестная команда. Попробуй /addTask, /addReminder или /addNote.");
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            User telegramUser = update.getMessage().getFrom();

            BotState state = getUserState(chatId);

            switch (state) {
                case WAITING_TASK,WAITING_TASK_DEADLINE-> handleTaskInput(chatId, message, state);
                case WAITING_REMINDER -> handleReminderInput(chatId, message);
                case WAITING_NOTE -> handleNoteInput(chatId, message);
                default -> handleCommand(chatId, telegramUser, message);
            }

        }
        System.out.println("Пришло сообщение: " + update.getMessage().getText());
    }

    public void sendMessage(Long chatId, String message) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(message).build();
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("ошибка при отправке сообщения", e);
        }
    }

    private void handleStart (Long chatId, User telegramUser) {
        userService.getOrCreateUser(
                telegramUser.getId()
        );
        sendMessage(chatId, "Привет! Я бот для задач и напоминаний \nНапиши /addtask , " +
                "чтобы добавить задачу");
    }

    private void handleAddTask(Long chatId) {
        sendMessage(chatId, "Введите название задачи");
        setUserState(chatId, BotState.WAITING_TASK);
    }

    private void handleAddReminder(Long chatId) {
        sendMessage(chatId, "Введите напоминание в формате:\n" +
                "Текст; время в формате dd.MM.yyyy HH:mm");
        setUserState(chatId, BotState.WAITING_REMINDER);
    }

    private void handleAddNote(Long chatId) {
        sendMessage(chatId, "Введите текст заметки:");
        setUserState(chatId, BotState.WAITING_NOTE);
    }

    private final Map<Long, String> taskNames = new HashMap<>();
    private void handleTaskInput(Long chatId, String text, BotState state) {
        switch (state) {
            case WAITING_TASK -> {
                taskNames.put(chatId, text);
                userStates.put(chatId, BotState.WAITING_TASK_DEADLINE);
                sendMessage(chatId, "Введи дедлайн в виде: dd.MM.yyyy HH:mm");
            }
            case WAITING_TASK_DEADLINE -> {
                try {
                    String taskName = taskNames.get(chatId);
                    LocalDateTime deadline = LocalDateTime.parse(text.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

                    Task task = taskService.createTask(chatId, taskName, deadline);
                    taskNames.remove(chatId);
                    userStates.put(chatId, BotState.DEFAULT);

                    reminderService.createReminder(task, chatId, deadline.minusMinutes(10));
                    sendMessage(chatId, "Задача создана и напоминание установлено :)");
                } catch (DateTimeParseException e) {
                    sendMessage(chatId, "Неверный формат даты, перепроверь пожалуйста вводимые данные");
                }
            }
        }
    }

    private void handleReminderInput(Long chatId, String text) {
        try {
            String[] parts = text.split(";");
            if (parts.length != 2) {
                sendMessage(chatId, "Неверный формат :( Введи напоминание в формате:\nТекст; дата и время (dd.MM.yyyy HH:mm)");
                return;
            }

            String reminderText = parts[0].trim();
            LocalDateTime reminderDateTime = LocalDateTime.parse(parts[1].trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

            Task tempTask = taskService.createTask(chatId, reminderText, reminderDateTime);
            reminderService.createReminder(tempTask, chatId, reminderDateTime.minusMinutes(10));

            sendMessage(chatId, "Напоминание успешно добавлено :)");
        } catch (DateTimeParseException e) {
            sendMessage(chatId, "Неверный формат даты, используй: dd.MM.yyyy HH:mm");
        } finally {
            userStates.put(chatId, BotState.DEFAULT);
        }
    }

    private void handleNoteInput(Long chatId, String text) {
        try {
            if (text.trim().isEmpty()) {
                sendMessage(chatId, "Текст заметки не может быть пустым");
                return;
            }

            Note note = noteService.createNote(chatId, text);

            sendMessage(chatId, "Заметка сохранена :)");
        } catch (Exception e) {
            sendMessage(chatId, "Произошла ошибка при добавлении заметки...");
        } finally {
            userStates.put(chatId, BotState.DEFAULT);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void sendScheduledReminders() {
        List<ReminderNotification> reminders = reminderService.checkAndCollectReminders();
        for (ReminderNotification r : reminders) {
            sendMessage(r.telegramId(), r.message());
        }
    }

    private void setUserState(Long chatId, BotState state) {
        userStates.put(chatId, state);
    }

    private BotState getUserState(Long chatId) {
        return userStates.getOrDefault(chatId, BotState.DEFAULT);
    }
}
