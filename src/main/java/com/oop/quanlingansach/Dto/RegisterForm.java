package com.oop.quanlingansach.Dto;

/**
 * Dữ liệu form đăng ký. Không bind thẳng vào entity User để người dùng
 * không thể tự gửi thêm id/role và ghi đè tài khoản khác.
 */
public class RegisterForm {

    private String username;
    private String email;
    private String fullName;
    private String password;
    private String confirmPassword;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
