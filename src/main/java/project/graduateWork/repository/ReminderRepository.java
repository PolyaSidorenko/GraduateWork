package project.graduateWork.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.graduateWork.entity.Reminder;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findByReminderDateBeforeAndSendReminderFalse(LocalDateTime now);
}
