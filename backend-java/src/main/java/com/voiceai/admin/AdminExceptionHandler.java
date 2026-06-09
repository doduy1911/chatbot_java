package com.voiceai.admin;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = {
    AdminUserController.class,
    AdminGroupController.class,
    AdminPromptController.class
})
public class AdminExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> accessDenied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Hãy liên hệ với admin"));
    }
}
