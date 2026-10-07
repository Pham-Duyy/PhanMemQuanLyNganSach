package com.oop.quanlingansach.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

/**
 * Spring Security chỉ dùng để chống CSRF (và cung cấp BCrypt).
 * Đăng nhập do AuthController xử lý; phân quyền theo đường dẫn do RoleInterceptor (xem WebConfig).
 *
 * Mọi form POST dùng th:action sẽ tự có token _csrf; form/JS tự tạo phải thêm token bằng tay.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(Customizer.withDefaults())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())       // dùng POST /logout của AuthController
                .requestCache(cache -> cache.disable())
                .exceptionHandling(ex -> ex.accessDeniedHandler((request, response, denied) -> {
                    // Thường gặp khi phiên hết hạn làm token CSRF không còn hợp lệ.
                    // Filter bảo mật chạy trước DispatcherServlet nên phải tự lưu flash message vào session.
                    FlashMap flashMap = new FlashMap();
                    flashMap.put("error", "Phiên làm việc đã hết hạn hoặc yêu cầu không hợp lệ. Vui lòng thử lại.");
                    flashMap.setTargetRequestPath(request.getContextPath() + "/login");
                    new SessionFlashMapManager().saveOutputFlashMap(flashMap, request, response);
                    response.sendRedirect(request.getContextPath() + "/login");
                }));
        return http.build();
    }
}
