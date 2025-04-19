package project.graduateWork.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import project.graduateWork.entity.Reminder;
import project.graduateWork.entity.Task;
import project.graduateWork.repository.ReminderRepository;
import project.graduateWork.repository.TaskRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderService {
    private final ReminderRepository reminderRepository;
    private final NoteBotSender noteBotSender;

    @Scheduled(fixedRate = 60000)   //проверка напоминания каждую минуту
    public void checkAndSendReminder() {
        LocalDateTime now = LocalDateTime.now();
        List<Reminder> reminders = reminderRepository.findByReminderDateAfterAndSendReminderFalse(now);

        for (Reminder reminder : reminders) {
            try {
                Task task = reminder.getTask();
                Long chatId = task.getUser().getTelegramId();

                String message = "Напоминаю о задаче: " + task.getTitle();
                noteBotSender.sendMessage(chatId, message);

                reminder.setSendReminder(true);  //если sendReminder становится true, то напоминания больше не отправляются
                reminderRepository.save(reminder);
                log.info("Напоминание отправлено", chatId);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }
}
