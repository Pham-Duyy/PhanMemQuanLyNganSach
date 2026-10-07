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

    /** Các tài khoản USER (không gồm ADMIN). */
    List<User> findNormalUsers();

    long countNormalUsers();
}
