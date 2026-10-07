package com.oop.quanlingansach.Service;

/**
 * Lỗi nghiệp vụ (dữ liệu không hợp lệ, không có quyền, không tìm thấy...).
 * Message được hiển thị thẳng cho người dùng nên phải viết dễ hiểu.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
