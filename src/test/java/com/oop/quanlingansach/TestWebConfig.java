package com.oop.quanlingansach;

import com.oop.quanlingansach.Config.ActiveUserLoader;
import com.oop.quanlingansach.Config.SecurityConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.Optional;

/**
 * Cấu hình dùng chung cho các test controller (@WebMvcTest):
 *  - nạp SecurityConfig thật (CSRF bật, nên POST trong test phải .with(csrf()))
 *  - ActiveUserLoader trả nguyên user trong session (không cần database)
 */
@TestConfiguration
@Import(SecurityConfig.class)
public class TestWebConfig {

    @Bean
    public ActiveUserLoader activeUserLoader() {
        return Optional::of;
    }
}
