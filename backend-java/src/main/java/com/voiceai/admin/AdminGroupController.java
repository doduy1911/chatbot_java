package com.voiceai.admin;

import com.voiceai.model.Group;
import com.voiceai.model.Prompt;
import com.voiceai.repository.GroupRepository;
import com.voiceai.repository.PromptRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/group")
public class AdminGroupController {

    private final GroupRepository groupRepository;
    private final PromptRepository promptRepository;

    public AdminGroupController(GroupRepository groupRepository, PromptRepository promptRepository) {
        this.groupRepository = groupRepository;
        this.promptRepository = promptRepository;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateGroupRequest body) {
        try {
            if (groupRepository.findByGroupName(body.groupName()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Group đã tồn tại!"));
            }
            if (body.email() != null && groupRepository.findByEmail(body.email()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "email đã tồn tại!"));
            }
            if (body.phoneNumber() != null && groupRepository.findByPhoneNumber(body.phoneNumber()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "sdt đã tồn tại!"));
            }
            Group group = new Group();
            group.setGroupName(body.groupName());
            group.setEmail(body.email());
            group.setPhoneNumber(body.phoneNumber());
            Group saved = groupRepository.save(group);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", saved.getGroupId().toString());
            data.put("groupName", saved.getGroupName());
            data.put("email", saved.getEmail());
            data.put("phoneNumber", saved.getPhoneNumber());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Tao thanh cong group ",
                "data", data
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list() {
        try {
            var groups = groupRepository.findAll();
            return ResponseEntity.ok(Map.of("success", true, "count", groups.size(), "data", groups));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Khong lay duoc group"));
        }
    }

    @PostMapping("/prompt/create")
    public ResponseEntity<Map<String, Object>> createPrompt(@Valid @RequestBody CreatePromptRequest body) {
        try {
            if (promptRepository.findByGroupId(body.groupId()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "user này đã có prompt rồi "));
            }
            Prompt prompt = new Prompt();
            prompt.setGroupId(body.groupId());
            prompt.setPromptName(body.promptName());
            prompt.setContent(body.content());
            Prompt saved = promptRepository.save(prompt);
            return ResponseEntity.status(201).body(Map.of("success", true, "data", saved));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    record CreateGroupRequest(@NotBlank String groupName, String email, String phoneNumber) {}
    record CreatePromptRequest(@NotNull UUID groupId, @NotBlank String promptName, @NotBlank String content) {}
}
