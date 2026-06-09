package com.voiceai.repository;

import com.voiceai.model.SummaryPrompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface SummaryPromptRepository extends JpaRepository<SummaryPrompt, UUID> {

    Optional<SummaryPrompt> findByPromptId(UUID promptId);

    @Query(value = """
            SELECT sp.*
            FROM "SummaryPrompts" sp
            JOIN prompts p ON sp."promptId" = p.id
            WHERE p."groupId" = :groupId
            LIMIT 1
            """, nativeQuery = true)
    Optional<SummaryPrompt> findByGroupId(UUID groupId);
}
