package project.graduateWork.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {
    @Id
    private Long telegramId;
    private LocalDateTime createdAt;

    public User(Long telegramId) {this.telegramId = telegramId;}
}
