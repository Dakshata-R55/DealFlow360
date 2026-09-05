package com.dealflow360.auth.service;

import com.dealflow360.auth.dto.AuthSessionResponse;
import com.dealflow360.auth.dto.AuthUserResponse;
import com.dealflow360.auth.dto.LoginRequest;
import com.dealflow360.auth.dto.SignupRequest;
import com.dealflow360.auth.model.User;
import com.dealflow360.auth.model.UserRole;
import com.dealflow360.auth.repository.UserRepository;
import com.dealflow360.auth.security.AuthPrincipal;
import com.dealflow360.auth.security.JwtService;
import com.dealflow360.company.model.Company;
import com.dealflow360.company.repository.CompanyRepository;
import com.dealflow360.shared.exception.ConflictException;
import com.dealflow360.shared.exception.UnauthorizedException;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid credentials";
    private static final int COMPANY_CODE_MAX = 64;

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            CompanyRepository companyRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthSessionResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        String companyName = request.companyName().trim();
        String name = request.name().trim();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists");
        }
        Company company = companyRepository.insert(companyName, allocateCompanyCode(companyName));
        String passwordHash = passwordEncoder.encode(request.password());
        User user = userRepository.insert(company.id(), name, email, passwordHash, UserRole.ADMIN, true);
        return toSession(user, company);
    }

    public AuthSessionResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));
        if (!user.active()) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        Company company = companyRepository
                .findById(user.companyId())
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));
        if (!company.active()) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        return toSession(user, company);
    }

    public AuthUserResponse currentUser(AuthPrincipal principal) {
        User user = userRepository
                .findById(principal.userId())
                .orElseThrow(() -> new UnauthorizedException("Unauthorized"));
        if (!user.active()) {
            throw new UnauthorizedException("Unauthorized");
        }
        Company company = companyRepository
                .findById(user.companyId())
                .orElseThrow(() -> new UnauthorizedException("Unauthorized"));
        return toUserResponse(user, company);
    }

    private AuthSessionResponse toSession(User user, Company company) {
        return new AuthSessionResponse(
                jwtService.createToken(user), "Bearer", jwtService.ttlSeconds(), toUserResponse(user, company));
    }

    private static AuthUserResponse toUserResponse(User user, Company company) {
        return new AuthUserResponse(
                user.id(), user.name(), user.email(), user.role(), company.id(), company.name());
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String allocateCompanyCode(String companyName) {
        String base = slug(companyName);
        String candidate = base;
        int n = 2;
        while (companyRepository.existsByCode(candidate)) {
            String suffix = "-" + n;
            int maxBase = COMPANY_CODE_MAX - suffix.length();
            String trimmed = base.length() > maxBase ? base.substring(0, maxBase) : base;
            candidate = trimmed + suffix;
            n++;
        }
        return candidate;
    }

    private static String slug(String companyName) {
        String slug = companyName
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        if (slug.isBlank()) {
            slug = "COMPANY";
        }
        if (slug.length() > COMPANY_CODE_MAX) {
            slug = slug.substring(0, COMPANY_CODE_MAX);
        }
        return slug;
    }
}