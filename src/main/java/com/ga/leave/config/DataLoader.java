package com.ga.leave.config;

import com.ga.leave.model.*;

import com.ga.leave.repository.RoleRepository;
import com.ga.leave.repository.UserProfileRepository;
import com.ga.leave.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (!userRepository.existsByEmailAddress("khalil.ak.bh1170@gmail.com")) {
            seedRoles();
            seedDemoData();
        } else {
            log.info("Demo data already exists, skipping seed.");
        }
    }

    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (!roleRepository.existsByName(roleName)) {
                Role role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
                log.info("Seeded role: {}", roleName);
            }
        }
        log.info("Role seeding complete. Total roles: {}", roleRepository.count());
    }

    private void seedDemoData() {
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));

        // ========== USERS ==========

        // Admin user - Khalil
        UserProfile adminProfile = new UserProfile();
        adminProfile.setFirstName("Khalil");
        adminProfile.setLastName("Admin");
        adminProfile.setPhoneNumber(5551234567L);
        adminProfile = userProfileRepository.save(adminProfile);

        User admin = new User();
        admin.setUserName("khalil");
        admin.setEmailAddress("khalil.ak.bh1170@gmail.com");
        admin.setPassword(passwordEncoder.encode("Admin123!"));
        admin.setAccountVerified(true);
        admin.setActive(true);
        admin.setSubscriptionType(SubscriptionType.ENTERPRISE);
        admin.setSubscriptionDurationDays(365);
        admin.setSubscriptionActive(true);
        admin.setSubscriptionStartDate(LocalDateTime.now());
        admin.setSubscriptionEndDate(LocalDateTime.now().plusDays(365));
        admin.setRoles(Set.of(adminRole, userRole));
        admin.setUserProfile(adminProfile);
        admin = userRepository.save(admin);
        log.info("Seeded admin user: khalil.ak.bh1170@gmail.com / Admin123!");

        // Regular user - Hasan
        UserProfile hasanProfile = new UserProfile();
        hasanProfile.setFirstName("Hasan");
        hasanProfile.setLastName("User");
        hasanProfile.setPhoneNumber(5559876543L);
        hasanProfile = userProfileRepository.save(hasanProfile);

        User hasan = new User();
        hasan.setUserName("hasan");
        hasan.setEmailAddress("umkhalil1170@gmail.com");
        hasan.setPassword(passwordEncoder.encode("Admin123!"));
        hasan.setAccountVerified(true);
        hasan.setActive(true);
        hasan.setSubscriptionType(SubscriptionType.PREMIUM);
        hasan.setSubscriptionDurationDays(30);
        hasan.setSubscriptionActive(true);
        hasan.setSubscriptionStartDate(LocalDateTime.now());
        hasan.setSubscriptionEndDate(LocalDateTime.now().plusDays(30));
        hasan.setRoles(Set.of(userRole));
        hasan.setUserProfile(hasanProfile);
        hasan = userRepository.save(hasan);
        log.info("Seeded user: umkhalil1170@gmail.com / Admin123!");

        // Free user
        UserProfile freeProfile = new UserProfile();
        freeProfile.setFirstName("Demo");
        freeProfile.setLastName("User");
        freeProfile.setPhoneNumber(5555551212L);
        freeProfile = userProfileRepository.save(freeProfile);

        User freeUser = new User();
        freeUser.setUserName("demouser");
        freeUser.setEmailAddress("demo@scantrak.com");
        freeUser.setPassword(passwordEncoder.encode("Admin123!"));
        freeUser.setAccountVerified(true);
        freeUser.setActive(true);
        freeUser.setSubscriptionType(SubscriptionType.FREE);
        freeUser.setSubscriptionDurationDays(0);
        freeUser.setSubscriptionActive(false);
        freeUser.setSubscriptionStartDate(LocalDateTime.now());
        freeUser.setRoles(Set.of(userRole));
        freeUser.setUserProfile(freeProfile);
        freeUser = userRepository.save(freeUser);
        log.info("Seeded user: demo@scantrak.com / Admin123!");

    }
}
