package com.voiceai.client;

import com.voiceai.model.User;
import com.voiceai.redis.RedisQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class ClientRagController {

    private static final Logger log = LoggerFactory.getLogger(ClientRagController.class);

    private final RedisQueueService redisQueueService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public ClientRagController(RedisQueueService redisQueueService) {
        this.redisQueueService = redisQueueService;
    }

    @PostMapping("/uploadfile")
    public ResponseEntity<?> uploadFile(@RequestParam("files") List<MultipartFile> files,
                                        Authentication auth) {
        User user = (User) auth.getPrincipal();
        String userId = user.getId().toString();

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Chưa chọn file nào!"));
        }

        try {
            List<Map<String, Object>> saved = saveFiles(files, userId);

            Map<String, Object> job = Map.of(
                    "userId", userId,
                    "groupId", user.getGroupId() != null ? user.getGroupId().toString() : "",
                    "base", "no"
            );
            redisQueueService.pushEmbeddingTask(job);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "upload thanh cong base , dang bat dau huan luyen",
                    "userId", userId,
                    "files", saved
            ));
        } catch (IOException e) {
            log.error("[RAG] Upload error: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Internal Server Error", "error", e.getMessage()));
        }
    }

    private List<Map<String, Object>> saveFiles(List<MultipartFile> files, String userId) throws IOException {
        Path userDir = Paths.get(uploadDir, userId);
        Files.createDirectories(userDir);

        List<Map<String, Object>> result = new ArrayList<>();
        for (MultipartFile file : files) {
            String original = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
            String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
            String baseName = original.contains(".") ? original.substring(0, original.lastIndexOf('.')) : original;
            String slug = slugify(baseName);
            String filename = System.currentTimeMillis() + "-" + slug + ext;

            Path dest = userDir.resolve(filename);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

            result.add(Map.of("filename", filename, "path", dest.toString(), "size", file.getSize()));
        }
        return result;
    }

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[đĐ]", "d")
                .replaceAll("[^a-zA-Z0-9\\s-]", "")
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }
}
