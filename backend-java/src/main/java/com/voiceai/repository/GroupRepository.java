package com.voiceai.repository;

import com.voiceai.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {
    Optional<Group> findByGroupName(String groupName);
    Optional<Group> findByEmail(String email);
    Optional<Group> findByPhoneNumber(String phoneNumber);
}
