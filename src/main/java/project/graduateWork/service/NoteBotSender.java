package project.graduateWork.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import project.graduateWork.bot.NoteBot;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteBotSender { //отвечает за отправку сообщений
    private final NoteBot noteBot;

    public void sendMessage(Long chatId, String message) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(message).build();
        try {
            noteBot.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("ошибка при отправке сообщения", e);
        }
    }
}
