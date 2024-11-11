package com.javarush.telegram;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TutorAI extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = "ai_tutor_master_bot";
    public static final String TELEGRAM_BOT_TOKEN = "7435825583:AAHoXN8PePfH6HLLBBbmpTenUMNVcZ-twOM";
    public static final String OPEN_AI_TOKEN = "gpt:6MZuruLWYMt7BFAYy33hJFkblB3TrOQSkF7WUgsEFs26dToB";

    private final ChatGPTService chatGPT = new ChatGPTService(OPEN_AI_TOKEN);
    private DialogMode currentMode = null;

    public TutorAI() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        String message = getMessageText();

        try {
            if (message.equals("/start")) {
                currentMode = DialogMode.MAIN;
                sendPhotoMessage("tutorAI_mainPhoto");
                String startMessage = loadMessage("start_message");
                sendHtmlMessage(startMessage);
                showMainMenu("Start", "/start",
                        "Menu", "/menu");
                return;
            }

            if (message.equals("/menu")) {
                currentMode = DialogMode.GPT;
                sendPhotoMessage("break_or_continue_tutorAI");
                sendTextButtonsMessage("Do you want to continue or take a break", "Continue", "start", "Break", "stop");
                return;
            }


            // Handle continuous dialogue in GPT mode
            if (currentMode == DialogMode.GPT) {
                String query = getCallbackQueryButtonKey();

                if (query.equals("stop")) {
                    sendHtmlMessage("Okay, let's take a break. You can type /menu to resume learning anytime.");
                    currentMode = DialogMode.MAIN; // Reset mode to MAIN
                    return;
                }
                if (query.equals("start")){
                    String prompt = loadPrompt("promptChatGPT");
                    chatGPT.setPrompt(prompt);
                    String learnMessage = loadMessage("startLearningMessage");
                    sendHtmlMessage(learnMessage);
                    return;
                }



                String answer = chatGPT.addMessage(message);

                // Split the response into chunks if it exceeds Telegram's character limit
                int maxLength = 4096;
                if (answer.length() > maxLength) {
                    for (int start = 0; start < answer.length(); start += maxLength) {
                        int end = Math.min(start + maxLength, answer.length());
                        String part = answer.substring(start, end);
                        sendHtmlMessage(sanitizeMessage(part));
                    }
                }
                else {
                    sendHtmlMessage(sanitizeMessage(answer));
                }
            }
        }
        catch (Exception e) {
            sendTextMessage("An error occurred: " + e.getMessage());
        }

    }
    // Helper method to sanitize message for HTML
    private String sanitizeMessage(String message) {
        if (message == null) return null;
        return message.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }


    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TutorAI());
    }
}
