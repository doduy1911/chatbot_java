package com.voiceai.auth;

import com.voiceai.model.Role;
import com.voiceai.model.User;
import com.voiceai.repository.RoleRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AdminAuthorization {

    private final RoleRepository roleRepository;

    public AdminAuthorization(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public boolean isAdmin(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return false;
        }
        return roleRepository.findById(user.getRoleid())
            .map(Role::getRolename)
            .map("admin"::equals)
            .orElse(false);
    }
}
