package com.oop.quanlingansach.Service;

import com.oop.quanlingansach.Dto.TransactionForm;
import com.oop.quanlingansach.Model.Transaction;
import com.oop.quanlingansach.Model.TransactionParticipant;
import com.oop.quanlingansach.Model.User;

import java.util.List;
import java.util.Set;

/**
 * Giao dịch thu/chi của nhóm và quy trình đóng tiền:
 * thành viên báo đã chuyển -> thủ quỹ xác nhận hoặc từ chối -> chỉ tiền đã xác nhận mới vào quỹ.
 * Các hàm có tham số actor chỉ cho thủ quỹ của nhóm hoặc ban quản lý thực hiện.
 */
public interface TransactionService {

    // ==================== PHÍA QUẢN TRỊ ====================

    /** Giao dịch của các nhóm trong phạm vi quản lý, mới nhất trước. */
    List<Transaction> findManaged(User actor);

    Transaction getManagedTransaction(Long id, User actor);

    /**
     * Tạo giao dịch trong nhóm đang hoạt động. Khoản thu: nhóm phải có tài khoản nhận tiền,
     * tạo sẵn danh sách người phải đóng. Khoản chi: chỉ cho phép khi quỹ (tiền đã xác nhận) đủ.
     */
    Transaction create(TransactionForm form, User actor);

    /** Sửa tiêu đề, mô tả, số tiền, hạn của giao dịch chưa hủy / khoản thu đang thu. */
    void update(Long id, TransactionForm form, User actor);

    /** Hủy: khoản thu ngừng thu phần còn lại; khoản chi được hoàn lại quỹ. Lịch sử được giữ. */
    void cancel(Long id, User actor);

    /** Chỉ xóa được khoản thu chưa có ai đóng; giao dịch đã phát sinh tiền phải dùng hủy. */
    void delete(Long id, User actor);

    /** Thủ quỹ xác nhận đã nhận tiền của một thành viên (kể cả đóng tiền mặt chưa báo). */
    void confirmPayment(Long transactionId, Long userId, User actor);

    /** Thủ quỹ không thấy tiền về: trả khoản đó về trạng thái chưa đóng. */
    void rejectPayment(Long transactionId, Long userId, User actor);

    long countManagedByType(User actor, String type);

    // ==================== PHÍA THÀNH VIÊN ====================

    /** Thành viên báo đã chuyển tiền, kèm mã giao dịch / ghi chú (có thể trống). */
    void reportPayment(Long transactionId, Long userId, String reference);

    /** Khoản thu user còn phải đóng (gồm cả khoản đã báo, đang chờ xác nhận). */
    List<Transaction> findPendingIncomeForUser(Long userId);

    /** Id các khoản thu user đã báo chuyển, đang chờ thủ quỹ xác nhận. */
    Set<Long> findWaitingConfirmationIds(Long userId);

    /** Khoản chi của các nhóm user tham gia. */
    List<Transaction> findExpensesForMember(Long userId);

    /** Mọi khoản user được yêu cầu đóng. */
    List<TransactionParticipant> findContributionsOfUser(Long userId);

    List<TransactionParticipant> findPaidContributionsOfUser(Long userId);
}
