package com.bustrackpro.config;

import com.bustrackpro.config.jwt.JwtAuthenticationEntryPoint;
import com.bustrackpro.config.jwt.JwtAuthenticationFilter;
import com.bustrackpro.config.jwt.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

import static com.bustrackpro.constants.SystemConstants.ROLE_ADMIN;
import static com.bustrackpro.constants.SystemConstants.ROLE_AUTHORITY;
import static com.bustrackpro.constants.SystemConstants.ROLE_DRIVER;
import static com.bustrackpro.constants.SystemConstants.ROLE_OWNER;

/**
 * Security configuration.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    /**
     * Role hierarchy.
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        String hierarchy = ROLE_ADMIN + " > " + ROLE_DRIVER + "\n" +
                           ROLE_ADMIN + " > " + ROLE_OWNER + "\n" +
                           ROLE_ADMIN + " > " + ROLE_AUTHORITY;
        roleHierarchy.setHierarchy(hierarchy);
        return roleHierarchy;
    }

    /**
     * Authentication jwt token filter.
     */
    @Bean
    public JwtAuthenticationFilter authenticationJwtTokenFilter() {
        return new JwtAuthenticationFilter();
    }

    /**
     * Authentication provider.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
       
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
   
        return authProvider;
    }

    /**
     * Authentication manager.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Password encoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security filter chain.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .exceptionHandling(exception ->
                    exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> 
                auth.requestMatchers("/auth/**").permitAll()
                    .requestMatchers("/test/**").permitAll()
                    .requestMatchers("/routes/**").permitAll()
                    .requestMatchers("/tracking/**").permitAll()
                    .requestMatchers("/admin/**").hasAuthority(ROLE_ADMIN)
                    .requestMatchers("/driver/**").hasAuthority(ROLE_DRIVER)
                    .requestMatchers("/owner/**").hasAuthority(ROLE_OWNER)
                    .requestMatchers("/authority/**").hasAuthority(ROLE_AUTHORITY)
                    .requestMatchers("/users/*/status").hasAnyAuthority(ROLE_ADMIN, ROLE_DRIVER)
                    .requestMatchers("/users/**").hasAuthority(ROLE_ADMIN)
                    .requestMatchers("/schedules/assignments/**").hasAnyAuthority(ROLE_ADMIN, ROLE_AUTHORITY, ROLE_DRIVER, ROLE_OWNER)
                    .requestMatchers("/schedules/**").hasAnyAuthority(ROLE_ADMIN, ROLE_AUTHORITY)
                    // Finance: fine-grained access is enforced via @PreAuthorize in the controller
                    .requestMatchers("/finance/buses/*/daily-net").hasAnyAuthority(ROLE_ADMIN, ROLE_OWNER, ROLE_DRIVER)
                    .requestMatchers("/finance/**").hasAnyAuthority(ROLE_ADMIN, ROLE_OWNER)
                    .requestMatchers("/approvals/request").hasAnyAuthority(ROLE_ADMIN, ROLE_DRIVER, ROLE_OWNER)
                    .requestMatchers("/approvals/user/**").hasAnyAuthority(ROLE_ADMIN, ROLE_DRIVER, ROLE_OWNER)
                    .requestMatchers("/approvals/admin").hasAnyAuthority(ROLE_ADMIN, ROLE_AUTHORITY)
                    .requestMatchers("/approvals/*/status").hasAnyAuthority(ROLE_ADMIN, ROLE_AUTHORITY)
                    .anyRequest().authenticated()
            );
        
        http.authenticationProvider(authenticationProvider());

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:8081,http://localhost:4173,http://localhost:19006}") String origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
