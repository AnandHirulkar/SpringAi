package io.github.anandhirulkar.supportiq.knowledge;

import java.util.List;

/** Strategy: document splitting is an interchangeable algorithm. */
public interface ChunkingStrategy {

    List<String> chunk(String text);
}
