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
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // The PasswordEncoder 
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // SecurityFilterChain configuration.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, @Qualifier("operationalDataSource") DataSource dataSource) throws Exception {
        
        //  define UserDetailsService here
        JdbcUserDetailsManager userDetailsManager = new JdbcUserDetailsManager(dataSource);
        userDetailsManager.setUsersByUsernameQuery("SELECT username, password, enabled FROM app_user WHERE username=?");
        userDetailsManager.setAuthoritiesByUsernameQuery(
            "SELECT u.username, r.name FROM app_user u " +
            "JOIN user_roles ur ON u.user_id = ur.user_id " +
            "JOIN app_role r ON ur.role_id = r.role_id " +
            "WHERE u.username=?"
        );

        //  build the security chain using the UserDetailsService 
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/css/**", "/js/**", "/login").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true) // Go to the main page on success
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            // explicitly tell HttpSecurity to use manager
            .userDetailsService(userDetailsManager);

        return http.build();
    }
}