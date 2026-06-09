package com.voiceai.auth;

import com.voiceai.model.User;
import com.voiceai.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Map<String, Object> register(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("User đã tồn tại!");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        User saved = userRepository.save(user);
        return Map.of(
            "success", true,
            "message", "Đăng ký thành công!",
            "data", Map.of("id", saved.getId().toString(), "username", saved.getUsername())
        );
    }

    public Map<String, Object> login(String username, String password) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalStateException("Người dùng không tồn tại!"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new SecurityException("Sai mật khẩu");
        }

        String token = jwtService.generateToken(user.getId(), user.getUsername());
        return Map.of(
            "success", true,
            "message", "Đăng nhập thành công!",
            "token", token
        );
    }
}
