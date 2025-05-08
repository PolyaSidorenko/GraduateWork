package project.graduateWork.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.graduateWork.entity.Task;
import project.graduateWork.entity.User;
import project.graduateWork.repository.TaskRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserService userService;

    public Task createTask(Long telegramId, String title, LocalDateTime localDateTime) {
        User user = userService.getOrCreateUser(telegramId);
        Task task = Task.builder()
                .title(title)
                .localDateTime(localDateTime)
                .user(user)
                .completed(false)
                .build();

        return taskRepository.save(task);
    }

    public List<Task> getTaskByUser(Long telegramId) {
        return taskRepository.findAllByUserTelegramId(telegramId);
    }

//    public void deleteTaskById(Long taskId) {
//        taskRepository.deleteById(taskId);
//    }
}
