package com.ga.leave.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Activates @PreAuthorize evaluation inside @WebMvcTest slices.
 * Import this in controller tests that need to verify role-based access control.
 */
@TestConfiguration
@EnableMethodSecurity(prePostEnabled = true)
public class TestSecurityConfig {}
