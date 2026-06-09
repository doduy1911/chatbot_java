package com.voiceai.admin;

import com.voiceai.model.User;
import com.voiceai.repository.GroupRepository;
import com.voiceai.repository.RoleRepository;
import com.voiceai.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(UserRepository userRepository,
                               RoleRepository roleRepository,
                               GroupRepository groupRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.groupRepository = groupRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list() {
        try {
            var users = userRepository.findAll().stream().map(user -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", user.getId().toString());
                item.put("username", user.getUsername());
                item.put("clientType", user.getClientType());
                item.put("Role", roleRepository.findById(user.getRoleid()).map(role -> Map.of("rolename", role.getRolename())).orElse(null));
                item.put("Group", groupRepository.findById(user.getGroupId()).map(group -> Map.of("groupName", group.getGroupName())).orElse(null));
                return item;
            }).toList();
            return ResponseEntity.ok(Map.of("success", true, "count", users.size(), "data", users));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateUserRequest body) {
        try {
            if (userRepository.findByUsername(body.username()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "username da ton tai"));
            }
            User user = new User();
            user.setRoleid(2);
            user.setGroupId(body.groupId());
            user.setUsername(body.username());
            user.setPassword(passwordEncoder.encode(body.password()));
            user.setClientType(body.clientType() == null ? User.ClientType.human : body.clientType());
            User saved = userRepository.save(user);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "them thanh cong user",
                "data", Map.of(
                    "id", saved.getId().toString(),
                    "roleid", saved.getRoleid(),
                    "groupId", saved.getGroupId().toString(),
                    "username", saved.getUsername()
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    record CreateUserRequest(
        @NotNull UUID groupId,
        @NotBlank String username,
        @NotBlank String password,
        User.ClientType clientType
    ) {}
}
