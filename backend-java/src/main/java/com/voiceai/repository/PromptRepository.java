package com.voiceai.repository;

import com.voiceai.model.Prompt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PromptRepository extends JpaRepository<Prompt, UUID> {
    Optional<Prompt> findByGroupId(UUID groupId);
}
