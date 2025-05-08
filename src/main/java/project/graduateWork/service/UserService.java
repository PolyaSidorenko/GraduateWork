package project.graduateWork.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.graduateWork.entity.User;
import project.graduateWork.repository.UserRepository;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User getOrCreateUser(Long telegramId) {
        Optional<User> userOpt = userRepository.findById(telegramId);
        if (userOpt.isPresent()) {
            return userOpt.get();
        } else {
            User newUser = new User(telegramId);
            return userRepository.save(newUser);
        }
    }
}
