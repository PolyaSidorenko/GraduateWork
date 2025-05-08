package project.graduateWork.bot;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import project.graduateWork.entity.ReminderNotification;
import project.graduateWork.entity.Task;
import project.graduateWork.service.NoteService;
import project.graduateWork.service.ReminderService;
import project.graduateWork.service.TaskService;
import project.graduateWork.service.UserService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
            case "/start" -> {
                handleStart(telegramUser);
                sendMainMenu(chatId);
            }
            case "Добавить задачу" -> handleAddTask(chatId);
            case "Добавить напоминание" -> handleAddReminder(chatId);
            case "Добавить заметку" -> handleAddNote(chatId);
            default -> sendMessage(chatId, "Неизвестная команда :(");
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            User telegramUser = update.getMessage().getFrom();

            BotState state = getUserState(chatId);

            if (message.equals("Мои задачи")) {
                handleAllTasks(chatId);
                return;
            } else if (message.equals("Мои заметки")) {
                handleAllNotes(chatId);
                return;
            }

            switch (state) {
                case WAITING_TASK,WAITING_TASK_DEADLINE-> handleTaskInput(chatId, message, state);
                case WAITING_REMINDER -> handleReminderInput(chatId, message);
                case WAITING_NOTE -> handleNoteInput(chatId, message);
                default -> handleCommand(chatId, telegramUser, message);
            }

        }
        System.out.println("Пришло сообщение: " + update.getMessage().getText()); //для проверки правильности работы
    }

    public void sendMessage(Long chatId, String message) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(message).build();
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения", e);
        }
    }

    private void handleStart (User telegramUser) {
        userService.getOrCreateUser(
                telegramUser.getId()
        );
    }

    private void handleAddTask(Long chatId) {
        sendMessage(chatId, "Введи название задачи");
        setUserState(chatId, BotState.WAITING_TASK);
    }

    private void handleAddReminder(Long chatId) {
        sendMessage(chatId, "Введи напоминание в формате:\n" +
                "Текст; время в формате дд.мм.гггг чч:мм (например: 26.04.2025 15:00)");
        setUserState(chatId, BotState.WAITING_REMINDER);
    }

    private void handleAddNote(Long chatId) {
        sendMessage(chatId, "Введи текст заметки:");
        setUserState(chatId, BotState.WAITING_NOTE);
    }

    private final Map<Long, String> taskNames = new HashMap<>();
    private void handleTaskInput(Long chatId, String text, BotState state) {
        switch (state) {
            case WAITING_TASK -> {
                taskNames.put(chatId, text);
                userStates.put(chatId, BotState.WAITING_TASK_DEADLINE);
                sendMessage(chatId, "Введи дедлайн в виде: дд.мм.гггг чч:мм (например: 26.04.2025 15:00)");
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
                sendMessage(chatId, "Неверный формат :( Введи напоминание в формате:\nТекст; дата и время (например 26.04.2025 15:00)");
                return;
            }

            String reminderText = parts[0].trim();
            LocalDateTime reminderDateTime = LocalDateTime.parse(parts[1].trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

            Task tempTask = taskService.createTask(chatId, reminderText, reminderDateTime);
            reminderService.createReminder(tempTask, chatId, reminderDateTime.minusMinutes(10));

            sendMessage(chatId, "Напоминание успешно добавлено :)");
        } catch (DateTimeParseException e) {
            sendMessage(chatId, "Неверный формат даты, перепроверь пожалуйста вводимые данные");
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
            noteService.createNote(chatId, text);
            sendMessage(chatId, "Заметка сохранена :)");
        } catch (Exception e) {
            sendMessage(chatId, "Произошла ошибка при добавлении заметки...");
        } finally {
            userStates.put(chatId, BotState.DEFAULT);
        }
    }

    private void handleAllTasks(Long chatId) {
        List<Task> tasks = taskService.getTaskByUser(chatId);

        if (tasks.isEmpty()) {
            sendMessage(chatId, "У тебя пока нет задач");
            return;
        }

        StringBuilder sb = new StringBuilder("Твои задачи:\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        for (Task task : tasks) {
            sb.append(" - ").append(task.getTitle())
                    .append(" / ").append(task.getLocalDateTime().format(formatter)).append("\n");
        }
        sendMessage(chatId, sb.toString());
    }

    private void handleAllNotes(Long chatId) {
        List<project.graduateWork.entity.Note> notes = noteService.getNotesByUserId(chatId);
        if (notes.isEmpty()) {
            sendMessage(chatId, "У тебя пока нет заметок");
            return;
        }
        StringBuilder sb = new StringBuilder("Твои заметки:\n");
        for (project.graduateWork.entity.Note note : notes) {
            sb.append(" - ").append(note.getTitle()).append("\n");
        }
        sendMessage(chatId, sb.toString());
    }

    public void sendMainMenu(Long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = getKeyboardRows();

        keyboardMarkup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выбери действие из списка ниже");
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    private static List<KeyboardRow> getKeyboardRows() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Добавить задачу");
        row1.add("Мои задачи и напоминания");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Добавить заметку");
        row2.add("Мои заметки");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Добавить напоминание");
        row3.add("Настройки");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        return keyboard;
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
