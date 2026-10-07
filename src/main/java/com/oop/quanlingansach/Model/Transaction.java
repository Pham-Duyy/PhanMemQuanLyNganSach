package com.oop.quanlingansach.Model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Giao dịch của nhóm: khoản THU (thành viên phải đóng) hoặc khoản CHI (trừ vào quỹ).
 *
 * Khoản thu: ACTIVE (đang thu) --(mọi người đã đóng)--> COMPLETED
 *            ACTIVE --(admin hủy, ngừng thu phần còn lại)--> CANCELLED
 * Khoản chi: COMPLETED (đã chi, trừ vào quỹ) --(admin hủy)--> CANCELLED (hoàn lại quỹ)
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    public static final String TYPE_INCOME = "INCOME";
    public static final String TYPE_EXPENSE = "EXPENSE";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String type; // TYPE_INCOME / TYPE_EXPENSE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(nullable = false, length = 20)
    private String status; // STATUS_ACTIVE / STATUS_COMPLETED / STATUS_CANCELLED

    // Danh sách người phải đóng (chỉ có ở khoản thu)
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TransactionParticipant> participants = new ArrayList<>();

    public Transaction() {}

    public Transaction(String title, String description, BigDecimal amount,
                       String type, Group group, User createdBy, LocalDateTime dueDate) {
        this.title = title;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.group = group;
        this.createdBy = createdBy;
        this.dueDate = dueDate;
        this.createdDate = LocalDateTime.now();
        // Khoản chi được trừ vào quỹ ngay; khoản thu bắt đầu ở trạng thái đang thu
        this.status = TYPE_EXPENSE.equals(type) ? STATUS_COMPLETED : STATUS_ACTIVE;
    }

    public boolean isIncome() {
        return TYPE_INCOME.equalsIgnoreCase(type);
    }

    public boolean isExpense() {
        return TYPE_EXPENSE.equalsIgnoreCase(type);
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return STATUS_CANCELLED.equalsIgnoreCase(status);
    }

    /** Đã có ít nhất một khoản đóng góp được thủ quỹ xác nhận. */
    public boolean hasConfirmedPayments() {
        return participants.stream().anyMatch(TransactionParticipant::isPaid);
    }

    /** Còn người đã báo chuyển tiền nhưng thủ quỹ chưa xử lý. */
    public long countWaitingConfirmations() {
        return participants.stream().filter(TransactionParticipant::isWaitingConfirmation).count();
    }

    /** Khoản thu chuyển sang Hoàn thành khi mọi người phải đóng đều đã đóng. */
    public void completeIfFullyPaid() {
        if (isIncome() && isActive() && !participants.isEmpty()
                && participants.stream().allMatch(TransactionParticipant::isPaid)) {
            status = STATUS_COMPLETED;
        }
    }

    public void cancel() {
        status = STATUS_CANCELLED;
    }

    /** Bỏ một người khỏi danh sách phải đóng (vd. bị xóa khỏi nhóm), rồi xem đã thu đủ chưa. */
    public void removeParticipant(TransactionParticipant participant) {
        participants.remove(participant);
        completeIfFullyPaid();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    // Một số template dùng transaction.name
    public String getName() { return title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<TransactionParticipant> getParticipants() { return participants; }
    public void setParticipants(List<TransactionParticipant> participants) { this.participants = participants; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
