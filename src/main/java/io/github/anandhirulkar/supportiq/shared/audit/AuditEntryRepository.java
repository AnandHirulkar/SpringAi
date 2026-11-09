package io.github.anandhirulkar.supportiq.shared.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditEntryRepository extends MongoRepository<AuditEntry, String> {

    Page<AuditEntry> findByTypeOrderByOccurredAtDesc(String type, Pageable pageable);

    Page<AuditEntry> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
