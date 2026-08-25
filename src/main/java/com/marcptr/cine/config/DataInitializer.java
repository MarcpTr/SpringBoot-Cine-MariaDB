package com.marcptr.cine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.marcptr.cine.model.AppSetting;
import com.marcptr.cine.model.User;
import com.marcptr.cine.model.enums.Role;
import com.marcptr.cine.repository.AppSettingRepository;
import com.marcptr.cine.repository.UserRepository;

@Configuration
public class DataInitializer {
    @Value("${admin.username}")
    private String username;
    @Value("${admin.email}")
    private String email;
    @Value("${admin.password}")
    private String password;
    @Value("${security.max_sessions}")
    private String maxSessions;

    @Bean
    CommandLineRunner initAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {

            boolean adminExists = userRepository.existsByRole(Role.ROLE_ADMIN);

            if (!adminExists) {
                User admin = User.builder()
                        .username(username)
                        .email(
                                email)
                        .password(
                                passwordEncoder.encode(password))
                        .role(
                                Role.ROLE_ADMIN)
                        .build();
                userRepository.save(admin);
            }
        };
    }

    @Bean
    CommandLineRunner initAppSetting(AppSettingRepository appSettingRepository) {
        return args -> {
            if (!appSettingRepository.existsByConfigKey("security.max_sessions")) {
                AppSetting setting = AppSetting.builder().configKey("security.max_sessions").configValue(maxSessions)
                        .build();
                appSettingRepository.save(setting);
            }
        };
    }
}