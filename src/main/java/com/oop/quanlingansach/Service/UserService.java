package com.oop.quanlingansach.Service;

import com.oop.quanlingansach.Dto.RegisterForm;
import com.oop.quanlingansach.Model.User;

import java.util.List;
import java.util.Optional;

/**
 * Tài khoản: đăng nhập, đăng ký, hồ sơ, mật khẩu.
 * Các hàm báo lỗi bằng BusinessException với thông báo cho người dùng.
 */
public interface UserService {

    /** Trả về user nếu đúng tên đăng nhập và mật khẩu; tài khoản bị khóa thì báo BusinessException. */
    Optional<User> authenticate(String username, String password);

    /** Đăng ký tài khoản USER mới. */
    User register(RegisterForm form);

    User getById(Long id);

    User updateProfile(Long id, String fullName, String email);

    void changePassword(Long id, String oldPassword, String newPassword, String confirmPassword);

    /** Các tài khoản thành viên (USER), dùng để mời vào nhóm. */
    List<User> findNormalUsers();

    // ==================== BAN QUẢN LÝ ====================

    /** Mọi tài khoản, sắp theo vai trò rồi tên đăng nhập. */
    List<User> findAllUsers();

    /** Thủ quỹ đang hoạt động (ADMIN, SYSTEM_ADMIN), dùng khi bàn giao nhóm. */
    List<User> findActiveTreasurers();

    /** Bổ nhiệm (USER -> ADMIN) hoặc thu hồi (ADMIN -> USER) thủ quỹ. Chỉ ban quản lý được làm. */
    void changeRole(Long targetId, User.Role newRole, User actor);

    /** Khóa hoặc mở khóa tài khoản. Chỉ ban quản lý được làm. */
    void setActive(Long targetId, boolean active, User actor);
}
