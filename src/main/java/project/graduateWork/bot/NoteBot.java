package project.graduateWork.bot;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
public class NoteBot extends TelegramLongPollingBot{

    public String botUsername;
    public String botToken;

    public NoteBot(Dotenv dotenv) {
        this.botUsername = dotenv.get("TELEGRAM_BOT_USERNAME");
        this.botToken = dotenv.get("TELEGRAM_BOT_TOKEN");
    }
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
