package com.voiceai.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "Users")
public class User {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "roleid", nullable = false)
    private Integer roleid;

    @Column(name = "groupId", nullable = false)
    private UUID groupId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "clientType", nullable = false)
    @Enumerated(EnumType.STRING)
    private ClientType clientType = ClientType.human;

    @CreationTimestamp
    @Column(name = "createdAt", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt")
    private OffsetDateTime updatedAt;

    public enum ClientType { human, robot }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Integer getRoleid() { return roleid; }
    public void setRoleid(Integer roleid) { this.roleid = roleid; }

    public UUID getGroupId() { return groupId; }
    public void setGroupId(UUID groupId) { this.groupId = groupId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public ClientType getClientType() { return clientType; }
    public void setClientType(ClientType clientType) { this.clientType = clientType; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
