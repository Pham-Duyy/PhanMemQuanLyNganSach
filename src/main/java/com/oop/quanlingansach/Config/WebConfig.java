package com.oop.quanlingansach.Config;

import com.oop.quanlingansach.Model.User;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Phân quyền theo đường dẫn:
 *  - /admin/**                     : chỉ ADMIN
 *  - /user/**, /personal-finance   : chỉ USER
 *  - /auth/profile, đổi mật khẩu   : ai đã đăng nhập cũng được
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ActiveUserLoader activeUserLoader;

    public WebConfig(ActiveUserLoader activeUserLoader) {
        this.activeUserLoader = activeUserLoader;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RoleInterceptor(User.Role.ADMIN, activeUserLoader))
                .addPathPatterns("/admin", "/admin/**");

        registry.addInterceptor(new RoleInterceptor(User.Role.USER, activeUserLoader))
                .addPathPatterns("/user", "/user/**", "/personal-finance");

        registry.addInterceptor(new RoleInterceptor(null, activeUserLoader))
                .addPathPatterns("/auth/profile", "/auth/change-password");
    }
}
