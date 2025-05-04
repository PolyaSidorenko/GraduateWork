package project.graduateWork.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import project.graduateWork.entity.Reminder;
import project.graduateWork.entity.ReminderNotification;
import project.graduateWork.entity.Task;
import project.graduateWork.repository.ReminderRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderService {
    private final ReminderRepository reminderRepository;

    public Reminder createReminder(Task task, Long telegramId, LocalDateTime dateTime) {
        Reminder reminder = Reminder.builder()
                .task(task)
                .reminderDate(dateTime)
                .sendReminder(false)
                .build();
        return reminderRepository.save(reminder);
    }

    public List<ReminderNotification> checkAndCollectReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Reminder> reminders = reminderRepository.findByReminderDateBeforeAndSendReminderFalse(now);
        List<ReminderNotification> notifications = new ArrayList<>();

        for (Reminder reminder : reminders) {
            Task task = reminder.getTask();
            Long chatId = task.getUser().getTelegramId();
            String message = "Напоминаю о задаче: " + task.getTitle();

            notifications.add(new ReminderNotification(chatId, message));

            reminder.setSendReminder(true);
            reminderRepository.save(reminder);
        }
        return notifications;
    }
}