package com.voiceai.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.function.BiConsumer;

@Service
public class RedisQueueService {

    private static final String AI_TASKS = "ai_tasks";
    private static final String AI_TASKS_ROBOT = "ai_tasks_robot";
    private static final String AI_TASKS_ROBO_MINH = "ai_tasks_robo_minh";
    private static final String EMBEDDING_TASKS = "embedding_tasks";
    private static final String VOICE_READY_PATTERN = "voice_ready:*";
    private static final String CHAT_RESPONSE_CHANNEL = "chat-respone";

    private final StringRedisTemplate redis;
    private final RedisMessageListenerContainer listenerContainer;
    private final ObjectMapper objectMapper;

    public RedisQueueService(StringRedisTemplate redis,
                             RedisMessageListenerContainer listenerContainer,
                             ObjectMapper objectMapper) {
        this.redis = redis;
        this.listenerContainer = listenerContainer;
        this.objectMapper = objectMapper;
    }

    public void pushTask(Map<String, Object> task) {
        push(AI_TASKS, task);
    }

    public void pushTaskRobot(Map<String, Object> task) {
        push(AI_TASKS_ROBOT, task);
    }

    public void pushTaskRoboMinh(Map<String, Object> task) {
        push(AI_TASKS_ROBO_MINH, task);
    }

    public void pushEmbeddingTask(Map<String, Object> task) {
        push(EMBEDDING_TASKS, task);
    }

    private void push(String queue, Map<String, Object> task) {
        try {
            redis.opsForList().leftPush(queue, objectMapper.writeValueAsString(task));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis push serialize error", e);
        }
    }

    public String getCache(String key) {
        return redis.opsForValue().get(key);
    }

    public void setCache(String key, String value, long ttlSeconds) {
        redis.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
    }

    public void setCache(String key, String value) {
        setCache(key, value, 3600);
    }

    public void delCache(String key) {
        redis.delete(key);
    }

    /**
     * Đăng ký lắng nghe voice_ready:* pattern subscription.
     * Callback nhận (userId, data map).
     */
    public void listenForVoiceResponses(BiConsumer<String, Map<String, Object>> callback) {
        listenerContainer.addMessageListener(
            (message, pattern) -> {
                try {
                    String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
                    String userId = channel.split(":")[1];
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = objectMapper.readValue(message.getBody(), Map.class);
                    callback.accept(userId, data);
                } catch (Exception e) {
                    System.err.println("[Redis] Lỗi parse JSON voice_ready: " + e.getMessage());
                }
            },
            new PatternTopic(VOICE_READY_PATTERN)
        );
    }

    /**
     * Đăng ký lắng nghe channel chat-respone.
     * Callback nhận (userId, data map).
     */
    public void listenForChatResponses(BiConsumer<String, Map<String, Object>> callback) {
        listenerContainer.addMessageListener(
            (message, pattern) -> {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = objectMapper.readValue(message.getBody(), Map.class);
                    String userId = (String) data.get("userId");
                    callback.accept(userId, data);
                } catch (Exception e) {
                    System.err.println("[Redis] Lỗi parse JSON chat-respone: " + e.getMessage());
                }
            },
            new ChannelTopic(CHAT_RESPONSE_CHANNEL)
        );
    }
}
