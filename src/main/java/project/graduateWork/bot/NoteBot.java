package project.graduateWork.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
public class NoteBot extends TelegramLongPollingBot{

    @Value("${telegram.bot.username}")
    public String botUsername;
    @Value("${telegram.bot.token}")
    public String botToken;


    @Override
    public void onUpdateReceived(Update update) {

    }
    @Override
    public String getBotUsername() {
        return botUsername;
    }
    @Override
    public String getBotToken() {
        return botToken;
    }
}
