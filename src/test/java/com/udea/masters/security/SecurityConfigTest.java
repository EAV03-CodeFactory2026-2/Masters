package com.udea.masters.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    @Test
    void corsConfigurationSourcePermiteOrigenFrontendYNormalizaSlashFinal() {
        SecurityConfig securityConfig = new SecurityConfig(mock(JwtAuthenticationFilter.class));
        ReflectionTestUtils.setField(
                securityConfig,
                "allowedOrigins",
                "https://frontend-self-ten-51.vercel.app/ , , http://localhost:*"
        );

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/v1/health"));

        assertNotNull(config);
        assertEquals(
                "https://frontend-self-ten-51.vercel.app",
                config.checkOrigin("https://frontend-self-ten-51.vercel.app")
        );
        assertEquals(
                "http://localhost:5173",
                config.checkOrigin("http://localhost:5173")
        );
        assertTrue(Boolean.TRUE.equals(config.getAllowCredentials()));
    }

    @Test
    void securityFilterChainSeConstruyeCorrectamente() throws Exception {
        SecurityConfig securityConfig = new SecurityConfig(mock(JwtAuthenticationFilter.class));
        org.springframework.security.config.annotation.web.builders.HttpSecurity http =
                mock(org.springframework.security.config.annotation.web.builders.HttpSecurity.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(http.build()).thenReturn(mock(org.springframework.security.web.DefaultSecurityFilterChain.class));

        org.springframework.security.web.SecurityFilterChain chain = securityConfig.securityFilterChain(http);
        assertNotNull(chain);
    }
}
