package com.oop.quanlingansach.Config;

import com.oop.quanlingansach.Model.User;

import java.util.Optional;

/**
 * Đọc lại user đang đăng nhập từ database ở mỗi request,
 * để việc khóa tài khoản / đổi quyền có hiệu lực ngay cả với phiên đang mở.
 */
public interface ActiveUserLoader {

    /** Bản mới nhất của user trong session; rỗng nếu tài khoản đã bị xóa hoặc bị khóa. */
    Optional<User> refresh(User sessionUser);
}
