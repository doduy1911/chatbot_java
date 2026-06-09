package com.voiceai.client;

import com.voiceai.model.User;
import com.voiceai.redis.RedisQueueService;
import com.voiceai.repository.PromptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private static final long TIMEOUT_SECONDS = 30;

    private final RedisQueueService redisQueueService;
    private final PromptRepository promptRepository;

    // pending futures keyed by userId — resolved when Redis publishes chat-respone
    private final ConcurrentHashMap<String, CompletableFuture<Map<String, Object>>> pendingReplies =
            new ConcurrentHashMap<>();

    public ChatController(RedisQueueService redisQueueService, PromptRepository promptRepository) {
        this.redisQueueService = redisQueueService;
        this.promptRepository = promptRepository;

        // Register listener once — routes chat-respone publish to the right pending future
        this.redisQueueService.listenForChatResponses((userId, data) -> {
            CompletableFuture<Map<String, Object>> future = pendingReplies.remove(userId);
            if (future != null) future.complete(data);
        });
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> body, Authentication auth) {
        User user = (User) auth.getPrincipal();
        String userId = user.getId().toString();
        String groupId = user.getGroupId() != null ? user.getGroupId().toString() : null;

        // Warm prompt cache if needed
        String cacheKey = "group:" + groupId + ":content";
        if (redisQueueService.getCache(cacheKey) == null && groupId != null) {
            promptRepository.findByGroupId(user.getGroupId()).ifPresent(p ->
                    redisQueueService.setCache(cacheKey, p.getContent(), 3600));
        }

        Map<String, Object> job = Map.of(
                "userId", userId,
                "groupId", groupId != null ? groupId : "",
                "text", body.getOrDefault("text", ""),
                "service", "chat"
        );

        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        pendingReplies.put(userId, future);
        redisQueueService.pushTask(job);

        try {
            Map<String, Object> reply = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return ResponseEntity.ok(Map.of("success", true, "reply", reply.getOrDefault("reply", "")));
        } catch (TimeoutException e) {
            pendingReplies.remove(userId);
            return ResponseEntity.status(504).body(Map.of("success", false, "message", "AI timeout"));
        } catch (Exception e) {
            pendingReplies.remove(userId);
            log.error("[Chat] Error: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Internal Server Error"));
        }
    }
}
