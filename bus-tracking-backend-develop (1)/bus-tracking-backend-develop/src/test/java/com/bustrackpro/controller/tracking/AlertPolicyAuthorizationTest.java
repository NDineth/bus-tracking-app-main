package com.bustrackpro.controller.tracking;

import com.bustrackpro.modal.tracking.AlertPolicy;
import com.bustrackpro.service.tracking.AlertPolicyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(AlertPolicyAuthorizationTest.Config.class)
class AlertPolicyAuthorizationTest {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean AlertPolicyService service() { return mock(AlertPolicyService.class); }
        @Bean AlertPolicyController controller(AlertPolicyService service) { return new AlertPolicyController(service); }
    }
    @Autowired AlertPolicyController controller;
    @Autowired AlertPolicyService service;
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); reset(service); }

    private void authenticate(String authority) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "test-user", "", List.of(new SimpleGrantedAuthority(authority))));
    }
    @Test void ownerCannotChangeFleetPolicy() {
        authenticate("BUS_OWNER");
        assertThrows(AccessDeniedException.class, () -> controller.updatePolicy(new AlertPolicy()));
        verifyNoInteractions(service);
    }
    @Test void adminCanChangeFleetPolicy() {
        authenticate("ADMIN");
        AlertPolicy policy = new AlertPolicy();
        when(service.updatePolicy(policy)).thenReturn(policy);
        assertSame(policy, controller.updatePolicy(policy));
        verify(service).updatePolicy(policy);
    }
}
