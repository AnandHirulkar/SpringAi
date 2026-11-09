package io.github.anandhirulkar.supportiq.knowledge;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    Optional<KnowledgeDocument> findByContentHash(String contentHash);

    boolean existsByContentHash(String contentHash);

    Page<KnowledgeDocument> findByStatus(KnowledgeStatus status, Pageable pageable);
}
