package com.oop.quanlingansach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Ứng dụng tự quản lý tài khoản (UserService) nên tắt tài khoản mặc định Spring Security tự sinh
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
