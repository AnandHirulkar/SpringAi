package io.github.anandhirulkar.supportiq.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ParagraphAwareChunkingStrategyTest {

    private static final String PARAGRAPH =
            "SupportIQ answers customer questions using retrieval augmented generation. "
                    + "Documents are split into passages, embedded and stored in pgvector. "
                    + "The assistant retrieves the closest passages before calling the model. ";

    @Test
    void shortDocumentsStayInOneChunk() {
        var strategy = new ParagraphAwareChunkingStrategy(new KnowledgeProperties(400, 80, 5, 0.5));

        List<String> chunks = strategy.chunk("A short policy note.\n\nWith two paragraphs.");

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst()).contains("short policy note").contains("two paragraphs");
    }

    @Test
    void longDocumentsAreSplitWithinTheBudgetAndOverlap() {
        var strategy = new ParagraphAwareChunkingStrategy(new KnowledgeProperties(300, 60, 5, 0.5));
        String document = (PARAGRAPH.repeat(12)).strip();

        List<String> chunks = strategy.chunk(document);

        assertThat(chunks).hasSizeGreaterThan(2);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(300));
        assertThat(chunks.getFirst()).isNotEqualTo(chunks.getLast());
    }

    @Test
    void unbrokenTextIsHardSplit() {
        var strategy = new ParagraphAwareChunkingStrategy(new KnowledgeProperties(200, 0, 5, 0.5));

        List<String> chunks = strategy.chunk("x".repeat(450));

        assertThat(chunks).hasSize(3);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(200));
    }

    @Test
    void blankInputProducesNoChunks() {
        var strategy = new ParagraphAwareChunkingStrategy(new KnowledgeProperties(200, 0, 5, 0.5));

        assertThat(strategy.chunk("   \n\n ")).isEmpty();
        assertThat(strategy.chunk(null)).isEmpty();
    }
}
