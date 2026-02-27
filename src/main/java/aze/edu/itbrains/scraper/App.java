package aze.edu.itbrains.scraper;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class App {
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new MyBot());

            System.out.println("Bot işləməyə başladı...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
