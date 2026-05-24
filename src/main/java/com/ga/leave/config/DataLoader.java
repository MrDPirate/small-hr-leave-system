package com.ga.leave.config;

import com.ga.leave.features.leavelogs.model.LeaveAction;
import com.ga.leave.features.leavelogs.model.LeaveLog;
import com.ga.leave.features.leavelogs.repository.LeaveLogRepository;
import com.ga.leave.features.leaverequest.model.LeavePeriod;
import com.ga.leave.features.leaverequest.model.LeaveRequest;
import com.ga.leave.features.leaverequest.model.LeaveStatus;
import com.ga.leave.features.leaverequest.repository.LeaveRequestRepository;
import com.ga.leave.features.leavetype.model.LeaveType;
import com.ga.leave.features.leavetype.repository.LeaveTypeRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Seeds the database with demo data on first startup.
 * Runs only when the admin user does not yet exist.
 * Leave types are seeded independently so they can be added to an existing database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveLogRepository leaveLogRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Entry point called by Spring Boot after context startup.
     * Skips user seeding if the admin account already exists;
     * seeds leave types independently if they are missing.
     *
     * @param args command-line arguments (not used)
     */
    @Override
    @Transactional
    public void run(String... args) {
        // Seed leave types regardless of user state (idempotent check inside)
        seedLeaveTypes();

        if (!userRepository.existsByEmailAddress("khalil.ak.bh1170@gmail.com")) {
            seedRoles();
            seedDemoData();
        } else {
            log.info("Demo data already exists, skipping user seed.");
        }
    }

    /**
     * Seeds all {@link RoleName} enum values into the roles table if they do not already exist.
     */
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

    /**
     * Seeds standard leave types (Annual Leave, Sick Leave) if none exist.
     */
    private void seedLeaveTypes() {
        if (leaveTypeRepository.count() > 0) {
            log.info("Leave types already seeded, skipping.");
            return;
        }

        LeaveType annual = new LeaveType();
        annual.setName("Annual Leave");
        annual.setDefaultDays(20);
        leaveTypeRepository.save(annual);

        LeaveType sick = new LeaveType();
        sick.setName("Sick Leave");
        sick.setDefaultDays(10);
        leaveTypeRepository.save(sick);

        LeaveType emergency = new LeaveType();
        emergency.setName("Emergency Leave");
        emergency.setDefaultDays(5);
        leaveTypeRepository.save(emergency);

        log.info("Seeded leave types: Annual Leave (20 days), Sick Leave (10 days), Emergency Leave (5 days)");
    }

    /**
     * Seeds three demo users (admin/HR, employee-manager, employee),
     * assigns manager relationships, and creates sample leave requests.
     */
    private void seedDemoData() {
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));

        // ========== PROFILES ==========

        // Khalil — HR admin; also acts as manager for other employees
        UserProfile adminProfile = new UserProfile();
        adminProfile.setFirstName("Khalil");
        adminProfile.setLastName("Admin");
        adminProfile.setPhoneNumber(5551234567L);
        adminProfile = userProfileRepository.save(adminProfile);

        // Hasan — employee whose manager is Khalil
        UserProfile hasanProfile = new UserProfile();
        hasanProfile.setFirstName("Hasan");
        hasanProfile.setLastName("User");
        hasanProfile.setPhoneNumber(5559876543L);
        hasanProfile.setManager(adminProfile);
        hasanProfile = userProfileRepository.save(hasanProfile);

        // Demo — employee whose manager is Khalil
        UserProfile freeProfile = new UserProfile();
        freeProfile.setFirstName("Demo");
        freeProfile.setLastName("User");
        freeProfile.setPhoneNumber(5555551212L);
        freeProfile.setManager(adminProfile);
        freeProfile = userProfileRepository.save(freeProfile);

        // ========== USERS ==========

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
        userRepository.save(hasan);
        log.info("Seeded user: umkhalil1170@gmail.com / Admin123!");

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
        userRepository.save(freeUser);
        log.info("Seeded user: demo@scantrak.com / Admin123!");

        // ========== SAMPLE LEAVE REQUESTS ==========

        LeaveType annualLeave = leaveTypeRepository.findByName("Annual Leave")
                .orElseThrow(() -> new RuntimeException("Annual Leave type not found"));
        LeaveType sickLeave = leaveTypeRepository.findByName("Sick Leave")
                .orElseThrow(() -> new RuntimeException("Sick Leave type not found"));
        LeaveType emergencyLeave = leaveTypeRepository.findByName("Emergency Leave")
                .orElseThrow(() -> new RuntimeException("Emergency Leave type not found"));

        // Pending request — Hasan requesting upcoming annual leave
        LeaveRequest pendingRequest = new LeaveRequest();
        pendingRequest.setProfile(hasanProfile);
        pendingRequest.setLeaveType(annualLeave);
        pendingRequest.setLeavePeriod(new LeavePeriod(
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(14)));
        pendingRequest.setReason("Family vacation");
        pendingRequest.setStatus(LeaveStatus.PENDING);
        pendingRequest = leaveRequestRepository.save(pendingRequest);

        LeaveLog submitLog = new LeaveLog();
        submitLog.setRequest(pendingRequest);
        submitLog.setActor(hasanProfile);
        submitLog.setAction(LeaveAction.SUBMITTED);
        submitLog.setDescription("Leave request submitted: Annual Leave");
        leaveLogRepository.save(submitLog);
        log.info("Seeded pending annual leave request for Hasan");

        // Approved request — Hasan's past sick leave
        LeaveRequest approvedRequest = new LeaveRequest();
        approvedRequest.setProfile(hasanProfile);
        approvedRequest.setLeaveType(sickLeave);
        approvedRequest.setLeavePeriod(new LeavePeriod(
                LocalDate.now().minusDays(14),
                LocalDate.now().minusDays(12)));
        approvedRequest.setReason("Flu recovery");
        approvedRequest.setStatus(LeaveStatus.APPROVED);
        approvedRequest = leaveRequestRepository.save(approvedRequest);

        LeaveLog submitLog2 = new LeaveLog();
        submitLog2.setRequest(approvedRequest);
        submitLog2.setActor(hasanProfile);
        submitLog2.setAction(LeaveAction.SUBMITTED);
        submitLog2.setDescription("Leave request submitted: Sick Leave");
        leaveLogRepository.save(submitLog2);

        LeaveLog approveLog = new LeaveLog();
        approveLog.setRequest(approvedRequest);
        approveLog.setActor(adminProfile);
        approveLog.setAction(LeaveAction.APPROVED);
        approveLog.setDescription("Leave request approved");
        leaveLogRepository.save(approveLog);
        log.info("Seeded approved sick leave request for Hasan");

        // Rejected request — Hasan's past annual leave that was turned down
        LeaveRequest rejectedRequest = new LeaveRequest();
        rejectedRequest.setProfile(hasanProfile);
        rejectedRequest.setLeaveType(annualLeave);
        rejectedRequest.setLeavePeriod(new LeavePeriod(
                LocalDate.now().minusDays(30),
                LocalDate.now().minusDays(28)));
        rejectedRequest.setReason("Personal travel");
        rejectedRequest.setStatus(LeaveStatus.REJECTED);
        rejectedRequest = leaveRequestRepository.save(rejectedRequest);

        LeaveLog submitLog3 = new LeaveLog();
        submitLog3.setRequest(rejectedRequest);
        submitLog3.setActor(hasanProfile);
        submitLog3.setAction(LeaveAction.SUBMITTED);
        submitLog3.setDescription("Leave request submitted: Annual Leave");
        leaveLogRepository.save(submitLog3);

        LeaveLog rejectLog = new LeaveLog();
        rejectLog.setRequest(rejectedRequest);
        rejectLog.setActor(adminProfile);
        rejectLog.setAction(LeaveAction.REJECTED);
        rejectLog.setDescription("Leave request rejected: team coverage unavailable");
        leaveLogRepository.save(rejectLog);
        log.info("Seeded rejected annual leave request for Hasan");

        // Cancelled request — Hasan submitted then withdrew an upcoming emergency leave
        LeaveRequest cancelledRequest = new LeaveRequest();
        cancelledRequest.setProfile(hasanProfile);
        cancelledRequest.setLeaveType(emergencyLeave);
        cancelledRequest.setLeavePeriod(new LeavePeriod(
                LocalDate.now().plusDays(20),
                LocalDate.now().plusDays(25)));
        cancelledRequest.setReason("Home repairs");
        cancelledRequest.setStatus(LeaveStatus.CANCELLED);
        cancelledRequest = leaveRequestRepository.save(cancelledRequest);

        LeaveLog submitLog4 = new LeaveLog();
        submitLog4.setRequest(cancelledRequest);
        submitLog4.setActor(hasanProfile);
        submitLog4.setAction(LeaveAction.SUBMITTED);
        submitLog4.setDescription("Leave request submitted: Emergency Leave");
        leaveLogRepository.save(submitLog4);

        LeaveLog cancelLog = new LeaveLog();
        cancelLog.setRequest(cancelledRequest);
        cancelLog.setActor(hasanProfile);
        cancelLog.setAction(LeaveAction.CANCELLED);
        cancelLog.setDescription("Leave request cancelled by employee");
        leaveLogRepository.save(cancelLog);
        log.info("Seeded cancelled emergency leave request for Hasan");

        // Pending request — Demo user requesting upcoming sick leave
        LeaveRequest demoPendingRequest = new LeaveRequest();
        demoPendingRequest.setProfile(freeProfile);
        demoPendingRequest.setLeaveType(sickLeave);
        demoPendingRequest.setLeavePeriod(new LeavePeriod(
                LocalDate.now().plusDays(3),
                LocalDate.now().plusDays(5)));
        demoPendingRequest.setReason("Doctor appointment and recovery");
        demoPendingRequest.setStatus(LeaveStatus.PENDING);
        demoPendingRequest = leaveRequestRepository.save(demoPendingRequest);

        LeaveLog demoSubmitLog = new LeaveLog();
        demoSubmitLog.setRequest(demoPendingRequest);
        demoSubmitLog.setActor(freeProfile);
        demoSubmitLog.setAction(LeaveAction.SUBMITTED);
        demoSubmitLog.setDescription("Leave request submitted: Sick Leave");
        leaveLogRepository.save(demoSubmitLog);
        log.info("Seeded pending sick leave request for Demo user");
    }
}
