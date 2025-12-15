package aze.edu.itbrains.scraper;

import org.telegram.telegrambots.bots.TelegramLongPollingBot; //Telegram bot yaratmaq üçün əsas class.
import org.telegram.telegrambots.meta.api.methods.send.SendMessage; //İstifadəçiyə mesaj göndərmək üçün.
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;  //İstifadəçiyə şəkil göndərmək üçün.
import org.telegram.telegrambots.meta.api.objects.InputFile; //Şəkil göndərmək üçün lazım olan obyekt.
import org.telegram.telegrambots.meta.api.objects.Update; //İstifadəçinin göndərdiyi məlumat.
import org.telegram.telegrambots.meta.exceptions.TelegramApiException; //Telegram ilə işləyərkən yaranan xətalar.

import java.io.IOException; //Scraper işləyərkən ola biləcək səhvlər üçün.
import java.util.List; //Məhsul siyahısını (Product obyektlərini) saxlamaq üçün.

public class MyBot extends TelegramLongPollingBot { //Bot daim Telegram serverini yoxlayır

    @Override
    public String getBotUsername() {
        return "L7Scraping_bot";
    }

    @Override
    public String getBotToken() {
        return "8491490743:AAEITGW258nFvrgO6Cl2pOwMcw9LUsvhXwc";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String userMessage = update.getMessage().getText().trim();

            if (userMessage.equals("/start")) {
                sendMsg(chatId, "👋 Salam! Məhsulun adını yazın, mən də bütün səhifələrdə axtarıb onun haqqında məlumat göndərəcəm.");
            } else {
                // ✅ Əvvəlcə "axtarılır" mesajı göndər
                sendMsg(chatId, "🔍 Məhsul axtarılır, zəhmət olmasa gözləyin...");

                // ✅ Scraping-i ayrı Thread-də işə sal
                new Thread(() -> {
                    try {
                        List<Scraper.Product> products = Scraper.scrapeAllProducts();

                        Scraper.Product found = null;
                        for (Scraper.Product p : products) {
                            if (p.name.toLowerCase().contains(userMessage.toLowerCase())) {
                                found = p;
                                break;
                            }
                        }

                        if (found != null) {
                            String caption = "🔹 <b>" + found.name + "</b>\n" +
                                    "💲 Qiymət: " + found.price + "\n" +
                                    "🔗 <a href=\"" + found.url + "\">Məhsula keçid etmək üçün klikləyin</a>";

                            sendPhoto(chatId, found.image, caption);

                        } else {
                            sendMsg(chatId, "❌ Təəssüf ki, bu adda məhsul tapılmadı.");
                        }

                    } catch (IOException e) {
                        sendMsg(chatId, "❌ Xəta baş verdi: " + e.getMessage());
                    }
                }).start();
            }
        }
    }


    private void sendMsg(String chatId, String text) {
        SendMessage message = new SendMessage(chatId, text);
        message.enableHtml(true); //HTML formatı aktiv edir
        try {
            execute(message); //Telegram API-yə göndərir.
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendPhoto(String chatId, String photoUrl, String caption) {
        SendPhoto photo = new SendPhoto();
        photo.setChatId(chatId);

        // preferred: use InputFile
        photo.setPhoto(new InputFile(photoUrl));

        photo.setCaption(caption); //Şəkilin altında göstəriləcək açıqlama mətni.
        photo.setParseMode("HTML"); // və ya "MarkdownV2" seçsəm, uyğun escape edim
        try {
            execute(photo); //Bot Telegram serverinə “bu şəkli göndər” sorğusu atır.
        } catch (TelegramApiException e) {
            e.printStackTrace(); //Əgər şəkil göndərilərkən xəta çıxarsa, xəta detalları terminalda çap olunur.
            // fallback: göndərmə uğursuz olarsa, mətni göndərir
            sendMsg(chatId, "Şəkli göndərərkən xəta baş verdi. Zəhmət olmasa biraz sonra yenidən cəhd edin: " + e.getMessage());
        }
    }
}