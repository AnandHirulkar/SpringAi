package io.github.anandhirulkar.supportiq.assistant.chat;

public record ChatAnswer(String text, String model, int promptTokens, int completionTokens) {

    public int totalTokens() {
        return promptTokens + completionTokens;
    }
}
