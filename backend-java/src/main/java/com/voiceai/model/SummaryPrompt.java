package com.voiceai.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "summaryPrompts")
public class SummaryPrompt {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "promptId", nullable = false, unique = true)
    private UUID promptId;

    @Column(name = "summary_prompt", nullable = false, columnDefinition = "TEXT")
    private String summaryPrompt;

    @CreationTimestamp
    @Column(name = "createdAt", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt")
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPromptId() { return promptId; }
    public void setPromptId(UUID promptId) { this.promptId = promptId; }

    public String getSummaryPrompt() { return summaryPrompt; }
    public void setSummaryPrompt(String summaryPrompt) { this.summaryPrompt = summaryPrompt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
