package com.oop.quanlingansach.Dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dữ liệu form tạo/sửa giao dịch (modal ở trang /admin/finance/transactions).
 * Khi sửa, groupId/type/targetUserId bị bỏ qua.
 */
public class TransactionForm {

    private Long groupId;
    private String type;
    private BigDecimal amount;
    private String title;
    private String description;
    private List<String> targetUserId; // id thành viên phải đóng, hoặc "ALL"; trống = tất cả
    private String dueDate;            // dạng yyyy-MM-ddTHH:mm từ input datetime-local

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTargetUserId() { return targetUserId; }
    public void setTargetUserId(List<String> targetUserId) { this.targetUserId = targetUserId; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
}
