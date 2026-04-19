package com.metrohub.config;

import com.metrohub.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for REST API
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Stateless session management (JWT)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // ============================================
                // PUBLIC ENDPOINTS (No authentication required)
                // ============================================
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/health", "/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // ============================================
                // DOCUMENT ENDPOINTS
                // ============================================
                // Upload - Only SUPER_ADMIN and DEPARTMENT_UPLOAD_ADMIN
                .requestMatchers(HttpMethod.POST, "/documents/upload").hasAnyRole(
                    "SUPER_ADMIN", "DEPARTMENT_UPLOAD_ADMIN"
                )
                
                // Acknowledge - Only DEPARTMENT_USER
                .requestMatchers(HttpMethod.POST, "/documents/*/acknowledge").hasRole(
                    "DEPARTMENT_USER"
                )
                
                // View documents - All authenticated users
                .requestMatchers(HttpMethod.GET, "/documents/**").authenticated()
                
                // Delete documents - SUPER_ADMIN, DEPARTMENT_ADMIN, DEPARTMENT_UPLOAD_ADMIN
                .requestMatchers(HttpMethod.DELETE, "/documents/**").hasAnyRole(
                    "SUPER_ADMIN", "DEPARTMENT_ADMIN", "DEPARTMENT_UPLOAD_ADMIN"
                )
                
                // ============================================
                // USER MANAGEMENT ENDPOINTS
                // ============================================
                // Global user management - Only SUPER_ADMIN
                .requestMatchers("/users/all", "/users/create-admin").hasRole("SUPER_ADMIN")
                
                // Department user management - SUPER_ADMIN, DEPARTMENT_ADMIN and DEPARTMENT_UPLOAD_ADMIN
                .requestMatchers("/users/department/**").hasAnyRole(
                    "SUPER_ADMIN", "DEPARTMENT_ADMIN", "DEPARTMENT_UPLOAD_ADMIN"
                )
                
                // User profile - All authenticated users
                .requestMatchers(HttpMethod.GET, "/users/me", "/users/profile").authenticated()
                .requestMatchers(HttpMethod.PUT, "/users/me/**").authenticated()
                
                // ============================================
                // DASHBOARD ENDPOINTS
                // ============================================
                // Admin dashboard - SUPER_ADMIN only
                .requestMatchers("/dashboard/admin", "/dashboard/global").hasRole("SUPER_ADMIN")
                
                // Department dashboard - All department roles
                .requestMatchers("/dashboard/department/**").hasAnyRole(
                    "SUPER_ADMIN", "DEPARTMENT_UPLOAD_ADMIN", "DEPARTMENT_ADMIN", "DEPARTMENT_USER"
                )
                
                // ============================================
                // ALERT ENDPOINTS
                // ============================================
                .requestMatchers("/alerts/**").authenticated()
                
                // ============================================
                // SEARCH ENDPOINTS
                // ============================================
                .requestMatchers("/search/**").authenticated()
                
                // ============================================
                // PHASE 8: REPORT ENDPOINTS (READ-ONLY)
                // ============================================
                // All report endpoints - SUPER_ADMIN and DEPARTMENT_ADMIN only
                // Note: Method-level security (@PreAuthorize) provides additional control
                .requestMatchers(HttpMethod.GET, "/reports/**").hasAnyRole(
                    "SUPER_ADMIN", "DEPARTMENT_ADMIN"
                )
                // Block all non-GET methods for reports (enforce read-only)
                .requestMatchers(HttpMethod.POST, "/reports/**").denyAll()
                .requestMatchers(HttpMethod.PUT, "/reports/**").denyAll()
                .requestMatchers(HttpMethod.DELETE, "/reports/**").denyAll()
                .requestMatchers(HttpMethod.PATCH, "/reports/**").denyAll()
                
                // ============================================
                // PHASE 10: ANALYTICS ENDPOINTS (READ-ONLY)
                // ============================================
                // ============================================
                // ANALYTICS ENDPOINTS (READ-ONLY)
                // ============================================
                // All analytics GET endpoints - SUPER_ADMIN and DEPARTMENT_ADMIN only
                .requestMatchers(HttpMethod.GET, "/analytics/**").hasAnyRole(
                    "SUPER_ADMIN", "DEPARTMENT_ADMIN"
                )
                // Risk calculation trigger - SUPER_ADMIN only
                .requestMatchers(HttpMethod.POST, "/analytics/risk/calculate").hasRole("SUPER_ADMIN")
                // Block all other modification methods for analytics (enforce read-only)
                .requestMatchers(HttpMethod.PUT, "/analytics/**").denyAll()
                .requestMatchers(HttpMethod.DELETE, "/analytics/**").denyAll()
                .requestMatchers(HttpMethod.PATCH, "/analytics/**").denyAll()

                // ============================================
                // POLICY ENDPOINTS - GET endpoints are public, Create/Update require auth
                // ============================================
                .requestMatchers(HttpMethod.GET, "/policies").permitAll()
                .requestMatchers(HttpMethod.GET, "/policies/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/policies").hasAnyRole(
                    "DEPARTMENT_ADMIN", "DEPARTMENT_UPLOAD_ADMIN", "SUPER_ADMIN"
                )
                .requestMatchers(HttpMethod.PUT, "/policies/**").hasAnyRole(
                    "DEPARTMENT_ADMIN", "DEPARTMENT_UPLOAD_ADMIN", "SUPER_ADMIN"
                )

                // ============================================
                // ALL OTHER ENDPOINTS - Require authentication
                // ============================================
                .anyRequest().authenticated()
            )
            
            // Add JWT filter before UsernamePasswordAuthenticationFilter
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow React development server and production origins
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",  // Vite default
            "http://localhost:3000",  // CRA default
            "http://localhost:3001",  // CRA fallback
            "http://localhost:8080"   // Backend (for testing)
        ));
        
        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        
        // Allow all headers
        configuration.setAllowedHeaders(List.of("*"));
        
        // Expose Authorization header for JWT
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", "Content-Disposition"
        ));
        
        // Allow credentials (cookies, auth headers)
        configuration.setAllowCredentials(true);
        
        // How long the browser should cache CORS response
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
