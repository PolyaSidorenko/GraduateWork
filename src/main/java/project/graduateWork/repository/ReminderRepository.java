package project.graduateWork.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.graduateWork.entity.Reminder;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
}
