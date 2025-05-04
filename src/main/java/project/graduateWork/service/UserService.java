package project.graduateWork.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.graduateWork.entity.User;
import project.graduateWork.repository.UserRepository;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User getOrCreateUser(Long telegramId) {
        return userRepository.findById(telegramId)
                .orElseGet(() -> {
            User newUser = User.builder()
                    .telegramId(telegramId)
                    .createdAt(LocalDateTime.now())
                    .build();
            return userRepository.save(newUser);
        });
    }
}
