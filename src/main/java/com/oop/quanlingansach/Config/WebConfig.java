package com.oop.quanlingansach.Config;

import com.oop.quanlingansach.Model.User.Role;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Set;

/**
 * Phân quyền theo đường dẫn (lớp ngoài). Quyền trên từng nhóm cụ thể được kiểm tra thêm ở tầng service.
 *  - /admin/users/**               : chỉ ban quản lý (SYSTEM_ADMIN)
 *  - /admin/**                     : thủ quỹ (ADMIN) và ban quản lý
 *  - /user/**, /personal-finance   : thành viên (USER)
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
        registry.addInterceptor(new RoleInterceptor(Set.of(Role.ADMIN, Role.SYSTEM_ADMIN), activeUserLoader))
                .addPathPatterns("/admin", "/admin/**");

        registry.addInterceptor(new RoleInterceptor(Set.of(Role.SYSTEM_ADMIN), activeUserLoader))
                .addPathPatterns("/admin/users", "/admin/users/**");

        registry.addInterceptor(new RoleInterceptor(Set.of(Role.USER), activeUserLoader))
                .addPathPatterns("/user", "/user/**", "/personal-finance");

        registry.addInterceptor(new RoleInterceptor(Set.of(), activeUserLoader))
                .addPathPatterns("/auth/profile", "/auth/change-password");
    }
}
