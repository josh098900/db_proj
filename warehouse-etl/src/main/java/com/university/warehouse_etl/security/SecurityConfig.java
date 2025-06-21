package com.university.warehouse_etl.security;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enables @PreAuthorize on controller methods
public class SecurityConfig {

    // Bean #1: The Password Encoder (Correct as is)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Bean #2: The Database User Details Manager (Restored as a separate bean)
    // This is the bean that Spring looks for to disable the default security password.
    @Bean
    public UserDetailsManager userDetailsManager(@Qualifier("operationalDataSource") DataSource dataSource) {
        JdbcUserDetailsManager users = new JdbcUserDetailsManager(dataSource);
        
        // This query is updated for Oracle's NUMBER(1) boolean convention
        users.setUsersByUsernameQuery("SELECT username, password, enabled FROM app_user WHERE username=?");
        
        // This authorities query is standard SQL and works on both PostgreSQL and Oracle
        users.setAuthoritiesByUsernameQuery(
            "SELECT u.username, r.name FROM app_user u " +
            "JOIN user_roles ur ON u.user_id = ur.user_id " +
            "JOIN app_role r ON ur.role_id = r.role_id " +
            "WHERE u.username=?"
        );
        return users;
    }

    // Bean #3: The Security Filter Chain (Correct as is)
    // This configures the login page and URL permissions.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/css/**", "/js/**", "/login").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }
}