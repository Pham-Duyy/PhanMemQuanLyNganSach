package com.oop.quanlingansach.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Tài khoản đăng nhập. Quỹ do một tổ chức quản lý theo 3 cấp:
 *  - SYSTEM_ADMIN: ban quản lý tổ chức — bổ nhiệm thủ quỹ, khóa tài khoản, giám sát mọi nhóm
 *  - ADMIN       : thủ quỹ — chỉ quản lý các nhóm được giao (group.adminId)
 *  - USER        : thành viên — tham gia nhóm và đóng quỹ
 */
@Entity
@Table(name = "users")
public class User {

    public enum Role { SYSTEM_ADMIN, ADMIN, USER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // Mật khẩu đã băm BCrypt; không bao giờ trả ra JSON
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    // Lưu dạng VARCHAR (không dùng kiểu ENUM của MySQL) để thêm vai trò mới không cần sửa cấu trúc bảng
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "is_active")
    private boolean isActive;

    public User() {}

    /** Được vào khu vực quản trị (/admin): thủ quỹ hoặc ban quản lý. */
    public boolean isAdmin() {
        return role == Role.ADMIN || role == Role.SYSTEM_ADMIN;
    }

    public boolean isSystemAdmin() {
        return role == Role.SYSTEM_ADMIN;
    }

    /** Tên vai trò hiển thị trên giao diện. */
    public String getRoleLabel() {
        return switch (role) {
            case SYSTEM_ADMIN -> "Ban quản lý";
            case ADMIN -> "Thủ quỹ";
            case USER -> "Thành viên";
        };
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
