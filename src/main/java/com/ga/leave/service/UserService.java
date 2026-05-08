package com.ga.leave.service;


import com.ga.leave.exception.InformationExistException;
import com.ga.leave.exception.InformationNotFoundException;
import com.ga.leave.mailing.AccountPasswordResetEmailContext;
import com.ga.leave.mailing.AccountVerificationEmailContext;
import com.ga.leave.mailing.EmailService;

import com.ga.leave.model.Role;
import com.ga.leave.model.RoleName;
import com.ga.leave.model.SecureToken;
import com.ga.leave.model.SubscriptionType;
import com.ga.leave.model.User;
import com.ga.leave.model.request.LoginRequest;
import com.ga.leave.model.request.NewPasswordRequest;
import com.ga.leave.model.request.RegisterRequest;
import com.ga.leave.model.response.LoginResponse;
import com.ga.leave.repository.RoleRepository;
import com.ga.leave.repository.UserRepository;
import com.ga.leave.security.JWTUtils;
import com.ga.leave.security.MyUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @Autowired
    EmailService emailService;

    @Autowired
    private SecureTokenService secureTokenService;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       @Lazy PasswordEncoder passwordEncoder, JWTUtils jwtUtils,
                       @Lazy AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public User createUser(RegisterRequest request) {
        if (userRepository.existsByEmailAddress(request.getEmailAddress())) {
            throw new InformationExistException("User already exist");
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new InformationNotFoundException("ROLE_USER does not exist"));

        User user = new User();
        user.setUserName(request.getUserName());
        user.setEmailAddress(request.getEmailAddress());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAccountVerified(false);
        user.setActive(true);
        user.setSubscriptionType(SubscriptionType.FREE);
        user.setSubscriptionDurationDays(0);
        user.setSubscriptionActive(false);
        user.setSubscriptionStartDate(LocalDateTime.now());
        user.setRoles(new HashSet<>());
        user.getRoles().add(userRole);

        User result = userRepository.save(user);
        sendConfirmationEmail(result);
        return result;
    }


    public void sendConfirmationEmail(User user) {
        SecureToken secureToken = secureTokenService.createToken();
        secureToken.setUser(user);
        secureTokenService.saveSecureToken(secureToken);
        AccountVerificationEmailContext context = new AccountVerificationEmailContext();
        context.init(user);
        context.setToken(secureToken.getToken());
        context.buildVerificationUrl("http://localhost:8080/", secureToken.getToken());

        emailService.sendMail(context);
    }
    public User findUserByEmailAddress(String email){
        return userRepository.findUserByEmailAddress(email);
    }

    public ResponseEntity<?> loginUser(LoginRequest loginRequest){
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(), loginRequest.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
            if (!userDetails.isActive()) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body(new LoginResponse("Account is deactivated"));
            }
            final String JWT = jwtUtils.generateJwtToken(userDetails);
            return ResponseEntity.ok(new LoginResponse(JWT));
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse("Error: Invalid email or password"));
        }
    }


    public void changePassword(String oldPassword, String newPassword) {
        // Get the current authenticated user from SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof MyUserDetails)) {
            throw new RuntimeException("User not authenticated");
        }
        MyUserDetails currentUserDetails = (MyUserDetails) authentication.getPrincipal();
        User user = currentUserDetails.getUser();

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void validate(String token) {
        SecureToken secureToken = secureTokenService.findByToken(token);
        if (secureToken == null) {
            throw new InformationNotFoundException("Invalid verification token");
        }
        if (secureToken.getExpireAt().isBefore(java.time.LocalDateTime.now())) {
            secureTokenService.removeToken(secureToken);
            throw new RuntimeException("Verification token has expired");
        }
        User user = secureToken.getUser();
        user.setAccountVerified(true);
        userRepository.save(user);
        // Invalidate token after successful use
        secureTokenService.removeToken(secureToken);
    }

    public void resetPassword(String emailAddress) {
        User user = userRepository.findUserByEmailAddress(emailAddress);
        if (user == null) {
            // Don't reveal if email exists - just silently return
            return;
        }
        SecureToken secureToken = secureTokenService.createToken();
        secureToken.setUser(user);
        secureTokenService.saveSecureToken(secureToken);
        AccountPasswordResetEmailContext context = new AccountPasswordResetEmailContext();
        context.init(user);
        context.setToken(secureToken.getToken());
        context.buildResetUrl("http://localhost:8080/", secureToken.getToken());

        emailService.sendMail(context);
    }

    @Transactional
    public void resetPasswordActivator(String token, NewPasswordRequest request) {
        SecureToken secureToken = secureTokenService.findByToken(token);
        if (secureToken == null) {
            throw new InformationNotFoundException("Invalid password reset token");
        }
        if (secureToken.getExpireAt().isBefore(java.time.LocalDateTime.now())) {
            secureTokenService.removeToken(secureToken);
            throw new RuntimeException("Password reset token has expired");
        }
        User user = secureToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        // Invalidate token after successful use
        secureTokenService.removeToken(secureToken);
    }

    public void softDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InformationNotFoundException("User not found"));

        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void promoteUserToAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InformationNotFoundException("User not found"));

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new InformationNotFoundException("ROLE_ADMIN does not exist"));

        boolean alreadyAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
        if (alreadyAdmin) {
            throw new InformationExistException("User is already an ADMIN");
        }

        user.getRoles().add(adminRole);
        userRepository.save(user);
    }

    @Transactional
    public void demoteUserFromAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InformationNotFoundException("User not found"));

        boolean wasAdmin = user.getRoles().removeIf(role -> role.getName() == RoleName.ROLE_ADMIN);
        if (!wasAdmin) {
            throw new InformationNotFoundException("User does not have the ADMIN role");
        }

        userRepository.save(user);
    }

}
