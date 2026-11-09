package io.github.anandhirulkar.supportiq.conversation;

/** A source the assistant used, stored with the turn so answers remain auditable later. */
public record Citation(String documentId, String title, String source, String snippet, double score) {
}
