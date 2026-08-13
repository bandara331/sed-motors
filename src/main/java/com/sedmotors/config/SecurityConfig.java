package com.sedmotors.config;

import com.sedmotors.security.CustomOAuth2UserService;
import com.sedmotors.security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/", "/index.html", "/admin-login.html", "/css/**", "/js/**",
                    "/api/parts", "/api/auth/register", "/api/auth/register-admin",
                    "/api/auth/login", "/api/auth/me").permitAll()
                // Public: customer inquiry submission
                .requestMatchers(HttpMethod.POST, "/api/inquiries").permitAll()
                // Customer chat functionality
                .requestMatchers(HttpMethod.GET, "/api/inquiries/my").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/inquiries/*/messages").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/inquiries/*/message").authenticated()
                // Admin-only: pages + all ERP APIs
                .requestMatchers(
                    "/admin.html",
                    "/api/parts/**",
                    "/api/work-orders/**",
                    "/api/invoices/**",
                    "/api/suppliers/**",
                    "/api/audit-logs/**",
                    "/api/inquiries/**"
                ).hasRole("ADMIN")
                // Booking: ADMIN can GET/PUT, authenticated users can POST
                .requestMatchers(org.springframework.http.HttpMethod.PUT,  "/api/bookings/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET,  "/api/bookings/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/bookings").authenticated()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/admin-login.html") // Redirect to admin login if unauthenticated
                .loginProcessingUrl("/api/auth/login") // Handled by Spring Security
                .successHandler((request, response, authentication) -> {
                    response.setStatus(200);
                    response.getWriter().write("""
                        {"status":"success"}
                        """);
                })
                .failureHandler((request, response, exception) -> {
                    response.setStatus(401);
                    response.getWriter().write("""
                        {"status":"error", "message":"Invalid credentials"}
                        """);
                })
            )
            /*
            .oauth2Login(oauth2 -> oauth2
                // When OAuth2 login succeeds, redirect to index
                .defaultSuccessUrl("/index.html", true)
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
            )
            */
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessUrl("/index.html")
                .permitAll()
            );
            
        http.authenticationProvider(authenticationProvider());

        return http.build();
    }
}
