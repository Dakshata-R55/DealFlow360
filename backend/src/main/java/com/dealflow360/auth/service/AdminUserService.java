package com.dealflow360.auth.service;

import com.dealflow360.auth.dto.CreateTeamUserRequest;
import com.dealflow360.auth.dto.TeamUserResponse;
import com.dealflow360.auth.model.User;
import com.dealflow360.auth.model.UserRole;
import com.dealflow360.auth.repository.UserRepository;
import com.dealflow360.shared.exception.BadRequestException;
import com.dealflow360.shared.exception.ConflictException;
import com.dealflow360.shared.exception.NotFoundException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private static final Set<UserRole> CREATABLE_ROLES =
            EnumSet.of(UserRole.SALES_REP, UserRole.SALES_MANAGER, UserRole.FINANCE_OPS);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<TeamUserResponse> list(long companyId) {
        return userRepository.findInternalByCompany(companyId).stream().map(AdminUserService::toResponse).toList();
    }

    @Transactional
    public TeamUserResponse create(long companyId, CreateTeamUserRequest request) {
        if (!CREATABLE_ROLES.contains(request.role())) {
            throw new BadRequestException("Role must be SALES_REP, SALES_MANAGER, or FINANCE_OPS");
        }
        String email = AuthService.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists");
        }
        String name = request.name().trim();
        String passwordHash = passwordEncoder.encode(request.password());
        User user = userRepository.insert(companyId, name, email, passwordHash, request.role(), true);
        return toResponse(user);
    }

    @Transactional
    public TeamUserResponse updateActive(long companyId, long adminUserId, long targetUserId, boolean active) {
        if (adminUserId == targetUserId) {
            throw new BadRequestException("Cannot change your own account");
        }
        User user = userRepository
                .findByIdAndCompany(targetUserId, companyId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.role() == UserRole.ADMIN) {
            throw new BadRequestException("Cannot modify admin users");
        }
        userRepository.updateActive(targetUserId, companyId, active);
        return toResponse(userRepository.findById(targetUserId).orElseThrow());
    }

    private static TeamUserResponse toResponse(User user) {
        return new TeamUserResponse(user.id(), user.name(), user.email(), user.role(), user.active());
    }
}
