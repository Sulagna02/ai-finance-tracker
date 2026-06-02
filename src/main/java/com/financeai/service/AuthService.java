package com.financeai.service;

import com.financeai.dto.AuthResponse;
import com.financeai.dto.LoginRequest;
import com.financeai.dto.RegisterRequest;
import com.financeai.entity.User;
import com.financeai.exception.InvalidCredentialsException;
import com.financeai.exception.UserAlreadyExistsException;
import com.financeai.repository.UserRepository;
import com.financeai.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles user registration and login.
 * Passwords are always hashed with BCrypt before persistence.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Registers a new user. Throws {@link UserAlreadyExistsException} if the email is taken.
     *
     * @param request validated registration payload
     * @return JWT token and user info
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User saved = userRepository.save(user);
        String token = jwtUtil.generateToken(saved.getEmail());

        log.info("Registered new user: {}", saved.getEmail());
        return AuthResponse.of(token, saved.getId(), saved.getName(), saved.getEmail());
    }

    /**
     * Authenticates a user by email and password.
     * Throws {@link InvalidCredentialsException} on any mismatch to avoid user enumeration.
     *
     * @param request validated login payload
     * @return JWT token and user info
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtUtil.generateToken(user.getEmail());
        log.info("User logged in: {}", user.getEmail());
        return AuthResponse.of(token, user.getId(), user.getName(), user.getEmail());
    }
}
