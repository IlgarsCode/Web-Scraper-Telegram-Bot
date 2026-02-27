package aze.edu.itbrains.scraper;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.io.IOException;
import java.util.List;

public class MyBot extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {
        return "L7Scraping_bot";
    }

    @Override
    public String getBotToken() {
        return "";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String userMessage = update.getMessage().getText().trim();

            System.out.println("İstifadəçi ID: " + chatId + " yazdı: " + userMessage);

            if (userMessage.equals("/start")) {
                sendMsg(chatId, "👋 Salam! Məhsulun adını yazın, mən bütün səhifələrdə axtarıb məlumat göndərəcəm.\n\n📦 Bütün məhsulları görmək üçün /all yazın.");
                return;
            }

            if (userMessage.equals("/all")) {
                new Thread(() -> {
                    try {
                        List<Scraper.Product> allProducts = Scraper.scrapeAllProducts();
                        int batchSize = 94;

                        for (int start = 0; start < allProducts.size(); start += batchSize) {
                            int end = Math.min(start + batchSize, allProducts.size());
                            List<Scraper.Product> subList = allProducts.subList(start, end);

                            StringBuilder names = new StringBuilder();
                            for (int i = 0; i < subList.size(); i++) {
                                names.append((start + i + 1)).append(". ").append(subList.get(i).name).append("\n");
                            }

                            sendMsg(chatId, "📋 Məhsullar (" + (start + 1) + " - " + end + "):\n\n" + names);
                        }

                    } catch (IOException e) {
                        sendMsg(chatId, "❌ Məhsullar yüklənməsində xəta baş verdi. Zəhmət olmasa biraz sonra yenidən cəhd edin: " + e.getMessage());
                    }
                }).start();
                return;
            }

            sendMsg(chatId, "🔍 Məhsul axtarılır, zəhmət olmasa gözləyin...");

            new Thread(() -> {
                try {
                    List<Scraper.Product> allProducts = Scraper.scrapeAllProducts();
                    Scraper.Product found = null;

                    for (Scraper.Product p : allProducts) {
                        if (p.name.toLowerCase().contains(userMessage.toLowerCase())) {
                            String link = p.url.startsWith("http") ? p.url : "https://www.scrapingcourse.com" + p.url;
                            found = Scraper.scrapeProductFromPage(link);
                            break;
                        }
                    }
                    if (found != null) {

                        String caption = "🛍️ " + found.name + "\n" +
                                "📝 Haqqında: " + found.description + "\n" +
                                "🎨 Rənglər: " + (found.colors.isEmpty() ? "Məlumat yoxdur" : String.join(" • ", found.colors)) + "\n" +
                                "📐 Ölçülər: " + (found.sizes.isEmpty() ? "Məlumat yoxdur" : String.join(" • ", found.sizes)) + "\n" +
                                "💰 Qiymət: " + found.price + "\n" +
                                "🔗 <a href=\"" + found.url + "\">Məhsula keçid etmək üçün klikləyin</a>";

                        sendPhoto(chatId, found.image, caption);
                    } else {
                        sendMsg(chatId, "❌ Məhsul tapılmadı. Zəhmət olmasa məhsulun adını düzgün yazın.");
                    }

                } catch (IOException e) {
                    sendMsg(chatId, "❌ Xəta baş verdi: " + e.getMessage());
                }
            }).start();
        }
    }

    private void sendMsg(String chatId, String text) {
        SendMessage message = new SendMessage(chatId, text);
        message.enableHtml(true);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendPhoto(String chatId, String photoUrl, String caption) {
        SendPhoto photo = new SendPhoto();
        photo.setChatId(chatId);
        photo.setPhoto(new InputFile(photoUrl));
        photo.setCaption(caption);
        photo.setParseMode("HTML");
        try {
            execute(photo);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendMsg(chatId, "❌ Şəkli göndərərkən xəta baş verdi: " + e.getMessage());
        }
    }
}
