package io.github.anandhirulkar.supportiq.conversation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AiConversationRepository extends MongoRepository<AiConversation, String> {

    Page<AiConversation> findByCustomerIdOrderByUpdatedAtDesc(Long customerId, Pageable pageable);
}
