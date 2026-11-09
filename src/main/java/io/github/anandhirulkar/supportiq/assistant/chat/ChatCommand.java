package io.github.anandhirulkar.supportiq.assistant.chat;

import java.util.List;

public record ChatCommand(String systemPrompt, List<ChatTurn> turns) {

    public ChatCommand {
        turns = List.copyOf(turns);
    }
}
