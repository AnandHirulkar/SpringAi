package io.github.anandhirulkar.supportiq.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import io.github.anandhirulkar.supportiq.knowledge.RetrievedChunk;
import org.junit.jupiter.api.Test;

class RagPromptBuilderTest {

    @Test
    void numbersSourcesSoAnswersCanCiteThem() {
        var chunks = List.of(
                new RetrievedChunk("10", "Export guide", "help/export.md", "Chunk exports into batches.", 0.91),
                new RetrievedChunk("11", "Rate limits", "help/limits.md", "Exports are limited to 10k rows.", 0.84));

        String prompt = RagPromptBuilder.build("Why does my export fail?", chunks);

        assertThat(prompt).contains("[1] Export guide (help/export.md)");
        assertThat(prompt).contains("[2] Rate limits (help/limits.md)");
        assertThat(prompt).contains("Chunk exports into batches.");
        assertThat(prompt).contains("Customer question:\nWhy does my export fail?");
    }

    @Test
    void statesWhenNoContextWasFound() {
        String prompt = RagPromptBuilder.build("Why does my export fail?", List.of());

        assertThat(prompt).contains("No knowledge-base context was found");
        assertThat(prompt).contains("Why does my export fail?");
    }
}
