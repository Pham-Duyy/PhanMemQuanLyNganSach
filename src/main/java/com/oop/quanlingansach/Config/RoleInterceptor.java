package com.oop.quanlingansach.Config;

import com.oop.quanlingansach.Model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.Optional;

/**
 * Chặn request nếu chưa đăng nhập, tài khoản đã bị khóa/xóa, hoặc sai vai trò.
 * User trong session được làm mới từ database ở mỗi request, nên controller luôn nhận dữ liệu mới nhất.
 */
public class RoleInterceptor implements HandlerInterceptor {

    private final User.Role requiredRole; // null = chỉ cần đăng nhập
    private final ActiveUserLoader activeUserLoader;

    public RoleInterceptor(User.Role requiredRole, ActiveUserLoader activeUserLoader) {
        this.requiredRole = requiredRole;
        this.activeUserLoader = activeUserLoader;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        User sessionUser = session != null ? (User) session.getAttribute(SessionKeys.CURRENT_USER) : null;
        if (sessionUser == null) {
            redirectToLogin(request, response, "Bạn cần đăng nhập để truy cập trang này!");
            return false;
        }

        Optional<User> current = activeUserLoader.refresh(sessionUser);
        if (current.isEmpty()) {
            session.invalidate();
            redirectToLogin(request, response, "Tài khoản đã bị khóa hoặc không còn tồn tại.");
            return false;
        }
        User user = current.get();
        session.setAttribute(SessionKeys.CURRENT_USER, user);

        if (requiredRole != null && user.getRole() != requiredRole) {
            // Sai khu vực: về trang chủ, trang chủ sẽ chuyển tới dashboard đúng vai trò
            response.sendRedirect(request.getContextPath() + "/");
            return false;
        }
        return true;
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response, String message) throws Exception {
        FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
        flashMap.put("error", message);
        RequestContextUtils.saveOutputFlashMap("/login", request, response);
        response.sendRedirect(request.getContextPath() + "/login");
    }
}
