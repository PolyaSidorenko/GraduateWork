package project.graduateWork.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import project.graduateWork.bot.NoteBot;

@Configuration
@RequiredArgsConstructor
public class BotConfig {
    @Lazy
    private final NoteBot noteBot;


    @PostConstruct
    public void registerBot() throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(noteBot);
        System.out.println("Бот зарегистрирован в TelegramBotsApi");
    }
}
