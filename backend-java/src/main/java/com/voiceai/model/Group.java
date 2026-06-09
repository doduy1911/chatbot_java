package com.voiceai.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "groups")
public class Group {

    @Id
    @UuidGenerator
    @Column(name = "groupId", updatable = false, nullable = false)
    private UUID groupId;

    @Column(name = "groupName", nullable = false, unique = true)
    private String groupName;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "phoneNumber", length = 15, unique = true)
    private String phoneNumber;

    @CreationTimestamp
    @Column(name = "createdAt", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt")
    private OffsetDateTime updatedAt;

    public UUID getGroupId() { return groupId; }
    public void setGroupId(UUID groupId) { this.groupId = groupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
