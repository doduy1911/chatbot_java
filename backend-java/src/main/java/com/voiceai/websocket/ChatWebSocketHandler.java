package com.voiceai.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceai.model.User;
import com.voiceai.redis.RedisQueueService;
import com.voiceai.repository.PromptRepository;
import com.voiceai.repository.SummaryPromptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final RedisQueueService redisQueueService;
    private final PromptRepository promptRepository;
    private final SummaryPromptRepository summaryPromptRepository;
    private final RedisMessageListenerContainer listenerContainer;

    // sessionId → WebSocketSession
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // userId (string) → WebSocketSession  (for Redis reply routing)
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ObjectMapper objectMapper,
                                RedisQueueService redisQueueService,
                                PromptRepository promptRepository,
                                SummaryPromptRepository summaryPromptRepository,
                                RedisMessageListenerContainer listenerContainer) {
        this.objectMapper = objectMapper;
        this.redisQueueService = redisQueueService;
        this.promptRepository = promptRepository;
        this.summaryPromptRepository = summaryPromptRepository;
        this.listenerContainer = listenerContainer;

        registerVoiceReadyListener();
    }

    // Subscribe pattern voice_ready:* once at startup, route replies to correct session
    private void registerVoiceReadyListener() {
        MessageListener listener = (Message message, byte[] pattern) -> {
            try {
                String channel = new String(message.getChannel());
                String userId = channel.substring("voice_ready:".length());
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(message.getBody(), Map.class);

                WebSocketSession target = userSessions.get(userId);
                if (target != null && target.isOpen()) {
                    sendJson(target, Map.of(
                            "type", "AI_VOICE_REPLY",
                            "text", data.getOrDefault("text", ""),
                            "audioUrl", data.getOrDefault("audioUrl", "")
                    ));
                }
            } catch (Exception e) {
                log.error("[WS] Error routing voice reply: {}", e.getMessage());
            }
        };
        listenerContainer.addMessageListener(listener, new PatternTopic("voice_ready:*"));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        User user = getUser(session);
        if (user == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        sessions.put(session.getId(), session);
        userSessions.put(user.getId().toString(), session);

        warmupPromptCache(user);
        log.info("[WS] {} connected ({})", user.getUsername(), user.getClientType());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        User user = getUser(session);
        if (user == null) return;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);

            Map<String, Object> task = Map.of(
                    "userId", user.getId().toString(),
                    "groupId", user.getGroupId().toString(),
                    "text", payload.getOrDefault("text", ""),
                    "voice", payload.getOrDefault("voice", ""),
                    "audio_format", "mp3",
                    "language", payload.getOrDefault("language", "vi"),
                    "timestamp", System.currentTimeMillis()
            );

            redisQueueService.pushTask(task);

            sendJson(session, Map.of("type", "STATUS", "content", "Hệ thống đang sinh giọng nói..."));

        } catch (Exception e) {
            log.error("[WS] Lỗi xử lý tin nhắn: {}", e.getMessage());
            sendJson(session, Map.of("type", "ERROR", "content", "Lỗi xử lý yêu cầu"));
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        // Robot client gửi raw PCM audio — hiện tại log và bỏ qua (Soniox SDK chỉ có cho Node.js)
        // Trong production: forward bytes tới Soniox via HTTP Streaming STT API
        log.debug("[WS] Received binary audio frame {} bytes", message.getPayloadLength());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        User user = getUser(session);
        sessions.remove(session.getId());
        if (user != null) {
            userSessions.remove(user.getId().toString());
            // Notify AI service user disconnected
            redisQueueService.pushTask(Map.of(
                    "userId", user.getId().toString(),
                    "text", "disconectuser",
                    "timestamp", System.currentTimeMillis()
            ));
            log.info("[WS] {} disconnected", user.getUsername());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("[WS] Transport error: {}", exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User getUser(WebSocketSession session) {
        Object attr = session.getAttributes().get("user");
        return (attr instanceof User u) ? u : null;
    }

    private void sendJson(WebSocketSession session, Object payload) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            }
        } catch (IOException e) {
            log.error("[WS] Failed to send message: {}", e.getMessage());
        }
    }

    private void warmupPromptCache(User user) {
        if (user.getGroupId() == null) return;
        String cacheKey = "group:" + user.getGroupId() + ":content";
        String summaryKey = "summary:" + user.getGroupId() + ":summary";

        if (redisQueueService.getCache(cacheKey) == null) {
            promptRepository.findByGroupId(user.getGroupId()).ifPresent(prompt -> {
                redisQueueService.setCache(cacheKey, prompt.getContent(), 3600);
                summaryPromptRepository.findByPromptId(prompt.getId()).ifPresent(sp -> {
                    redisQueueService.setCache(summaryKey, sp.getSummaryPrompt(), 3600);
                });
            });
        }
    }
}
