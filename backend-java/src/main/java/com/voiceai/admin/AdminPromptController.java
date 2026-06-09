package com.voiceai.admin;

import com.voiceai.model.Group;
import com.voiceai.model.Prompt;
import com.voiceai.model.SummaryPrompt;
import com.voiceai.redis.RedisQueueService;
import com.voiceai.repository.GroupRepository;
import com.voiceai.repository.PromptRepository;
import com.voiceai.repository.SummaryPromptRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/prompt")
public class AdminPromptController {

    private final GroupRepository groupRepository;
    private final PromptRepository promptRepository;
    private final SummaryPromptRepository summaryPromptRepository;
    private final RedisQueueService redisQueueService;

    public AdminPromptController(GroupRepository groupRepository,
                                 PromptRepository promptRepository,
                                 SummaryPromptRepository summaryPromptRepository,
                                 RedisQueueService redisQueueService) {
        this.groupRepository = groupRepository;
        this.promptRepository = promptRepository;
        this.summaryPromptRepository = summaryPromptRepository;
        this.redisQueueService = redisQueueService;
    }

    @PatchMapping("/edit/{id}")
    public ResponseEntity<Map<String, Object>> edit(@PathVariable UUID id, @RequestBody EditPromptRequest body) {
        try {
            Prompt prompt = promptRepository.findById(id).orElse(null);
            if (prompt == null) {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "Khong ton tai prompt nay"));
            }
            prompt.setContent(body.content());
            Prompt saved = promptRepository.save(prompt);
            redisQueueService.delCache("group:" + saved.getGroupId() + ":content");
            return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật thành công!", "data", saved));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/group")
    public ResponseEntity<Object> group() {
        try {
            var result = groupRepository.findAll().stream().map(group -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("groupId", group.getGroupId().toString());
                item.put("groupName", group.getGroupName());
                item.put("prompt", promptRepository.findByGroupId(group.getGroupId()).orElse(null));
                return item;
            }).toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/SummaryPrompt/create")
    public ResponseEntity<Map<String, Object>> createSummary(@Valid @RequestBody CreateSummaryPromptRequest body) {
        try {
            if (promptRepository.findById(body.promptId()).isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("message", "Prompt gốc không tồn tại!"));
            }
            SummaryPrompt summaryPrompt = new SummaryPrompt();
            summaryPrompt.setPromptId(body.promptId());
            summaryPrompt.setSummaryPrompt(body.summary_prompt());
            SummaryPrompt saved = summaryPromptRepository.save(summaryPrompt);
            return ResponseEntity.ok(Map.of("message", "Thêm thành công Summary Prompt ", "data", saved));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/SummaryPrompt/edit/{promptId}")
    public ResponseEntity<Map<String, Object>> editSummary(@PathVariable UUID promptId, @RequestBody EditSummaryPromptRequest body) {
        try {
            SummaryPrompt summaryPrompt = summaryPromptRepository.findByPromptId(promptId).orElse(null);
            if (summaryPrompt == null) {
                return ResponseEntity.status(404).body(Map.of("message", "Không tồn tại promptId"));
            }
            summaryPrompt.setSummaryPrompt(body.summary_prompt());
            summaryPromptRepository.save(summaryPrompt);
            redisQueueService.delCache("summary:" + promptId + ":summary");
            return ResponseEntity.ok(Map.of("message", "Update thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/SummaryPrompt/list")
    public ResponseEntity<Map<String, Object>> listSummary() {
        try {
            var data = summaryPromptRepository.findAll();
            return ResponseEntity.ok(Map.of("message", "Lấy data thành công ", "count", data.size(), "data", data));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    record EditPromptRequest(@NotBlank String content) {}
    record CreateSummaryPromptRequest(@NotNull UUID promptId, @NotBlank String summary_prompt) {}
    record EditSummaryPromptRequest(@NotBlank String summary_prompt) {}
}
