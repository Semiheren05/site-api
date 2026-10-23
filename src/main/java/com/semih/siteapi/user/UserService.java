package com.semih.siteapi.user;

import com.semih.siteapi.common.exception.ResourceNotFoundException;
import com.semih.siteapi.user.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;

    public UserService(UserRepository userRepository, UserMapper mapper) {
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    /** The user behind a valid token; 404 if the account was deleted after the token was issued. */
    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User no longer exists"));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(mapper::toResponse);
    }

    // --- For the auth package only. These return the entity (it carries the password hash),
    // --- so they must never be returned from a controller.

    @Transactional(readOnly = true)
    public Optional<User> findAccount(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmail(email);
    }

    /** @param passwordHash already hashed with BCrypt; this class never sees plain passwords */
    @Transactional
    public User createAccount(String email, String passwordHash, String displayName, Role role) {
        return userRepository.save(new User(email, passwordHash, displayName, role));
    }
}
