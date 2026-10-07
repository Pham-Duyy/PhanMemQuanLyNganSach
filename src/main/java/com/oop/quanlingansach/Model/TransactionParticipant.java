package com.oop.quanlingansach.Model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Một thành viên phải đóng một khoản thu.
 *
 * Trạng thái:  Chưa đóng --(user báo đã chuyển)--> Chờ xác nhận --(thủ quỹ xác nhận)--> Đã đóng
 *                  ^                                     |
 *                  └────────(thủ quỹ từ chối)────────────┘
 * Thủ quỹ cũng có thể xác nhận thẳng từ "Chưa đóng" (đóng tiền mặt).
 * Chỉ khoản "Đã đóng" mới được cộng vào quỹ.
 */
@Entity
// Mỗi người chỉ có một dòng "phải đóng" cho mỗi khoản thu
@Table(name = "transaction_participants",
        uniqueConstraints = @UniqueConstraint(name = "uk_participant_transaction_user",
                columnNames = {"transaction_id", "user_id"}))
public class TransactionParticipant {

    public static final String STATUS_PAID = "PAID";          // thủ quỹ đã xác nhận
    public static final String STATUS_PENDING = "PENDING";    // user đã báo chuyển, chờ xác nhận
    public static final String STATUS_UNPAID = "UNPAID";      // chưa đóng
    public static final String STATUS_CANCELLED = "CANCELLED"; // khoản thu đã hủy, không còn phải đóng

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    // true = thủ quỹ đã xác nhận nhận được tiền
    @Column(nullable = false)
    private boolean paid = false;

    // Thời điểm thủ quỹ xác nhận
    private LocalDateTime paidDate;

    // Thời điểm user báo đã chuyển tiền (null = chưa báo)
    @Column(name = "reported_date")
    private LocalDateTime reportedDate;

    // Mã giao dịch / ghi chú user khai khi báo chuyển, để thủ quỹ đối chiếu sao kê ngân hàng
    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    // Người đã xác nhận nhận tiền (phục vụ đối soát)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    public TransactionParticipant() {}

    public TransactionParticipant(Transaction transaction, User user, BigDecimal amount) {
        this.transaction = transaction;
        this.user = user;
        this.amount = amount;
    }

    /** Tổng số tiền của một danh sách đóng góp. */
    public static BigDecimal totalAmount(Collection<TransactionParticipant> participants) {
        return participants.stream()
                .map(TransactionParticipant::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** User báo đã chuyển tiền (kèm mã giao dịch nếu có), chờ thủ quỹ kiểm tra. */
    public void reportPaid(String reference) {
        requireNotPaid();
        this.reportedDate = LocalDateTime.now();
        this.paymentReference = (reference == null || reference.isBlank()) ? null : reference.trim();
    }

    /** Thủ quỹ xác nhận đã nhận tiền (chuyển khoản hoặc tiền mặt); ghi lại người xác nhận. */
    public void confirmPaid(User confirmer) {
        requireNotPaid();
        this.paid = true;
        this.paidDate = LocalDateTime.now();
        this.confirmedBy = confirmer;
    }

    /** Thủ quỹ không thấy tiền về: trả lại trạng thái "Chưa đóng" để user chuyển lại. */
    public void rejectReport() {
        requireNotPaid();
        this.reportedDate = null;
        this.paymentReference = null;
    }

    /** User đã báo chuyển nhưng thủ quỹ chưa xác nhận. */
    public boolean isWaitingConfirmation() {
        return !paid && reportedDate != null;
    }

    /** Mã trạng thái thống nhất: STATUS_PAID / STATUS_PENDING / STATUS_UNPAID / STATUS_CANCELLED. */
    public String getStatusCode() {
        if (paid) return STATUS_PAID;
        if (transaction != null && transaction.isCancelled()) return STATUS_CANCELLED;
        if (reportedDate != null) return STATUS_PENDING;
        return STATUS_UNPAID;
    }

    // Trạng thái hiển thị trên giao diện
    public String getPaidStatus() {
        return switch (getStatusCode()) {
            case STATUS_PAID -> "Đã đóng";
            case STATUS_CANCELLED -> "Đã hủy";
            case STATUS_PENDING -> "Chờ xác nhận";
            default -> "Chưa đóng";
        };
    }

    private void requireNotPaid() {
        if (paid) {
            throw new IllegalStateException("Khoản đóng góp đã được xác nhận");
        }
    }

    public Long getId() { return id; }

    public Transaction getTransaction() { return transaction; }

    public User getUser() { return user; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public boolean isPaid() { return paid; }

    public LocalDateTime getPaidDate() { return paidDate; }

    public LocalDateTime getReportedDate() { return reportedDate; }

    public String getPaymentReference() { return paymentReference; }

    public User getConfirmedBy() { return confirmedBy; }
}
