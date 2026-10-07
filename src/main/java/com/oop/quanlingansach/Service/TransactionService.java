package com.oop.quanlingansach.Service;

import com.oop.quanlingansach.Dto.TransactionForm;
import com.oop.quanlingansach.Model.Transaction;
import com.oop.quanlingansach.Model.TransactionParticipant;
import com.oop.quanlingansach.Model.User;

import java.util.List;
import java.util.Set;

/**
 * Giao dịch thu/chi của nhóm và quy trình đóng tiền:
 * user báo đã chuyển -> thủ quỹ (admin) xác nhận hoặc từ chối -> chỉ tiền đã xác nhận mới vào quỹ.
 */
public interface TransactionService {

    List<Transaction> findAll();

    Transaction getById(Long id);

    /**
     * Tạo giao dịch trong nhóm đang hoạt động. Khoản thu: tạo sẵn danh sách người phải đóng.
     * Khoản chi: chỉ cho phép khi quỹ (tiền đã xác nhận) đủ.
     */
    Transaction create(TransactionForm form, User creator);

    /** Sửa tiêu đề, mô tả, số tiền, hạn của giao dịch chưa hủy / khoản thu đang thu. */
    void update(Long id, TransactionForm form);

    /** Hủy: khoản thu ngừng thu phần còn lại; khoản chi được hoàn lại quỹ. Lịch sử được giữ. */
    void cancel(Long id);

    /** Chỉ xóa được khoản thu chưa có ai đóng; giao dịch đã phát sinh tiền phải dùng hủy. */
    void delete(Long id);

    // ==================== QUY TRÌNH ĐÓNG TIỀN ====================

    /** User báo đã chuyển tiền. */
    void reportPayment(Long transactionId, Long userId);

    /** Thủ quỹ xác nhận đã nhận tiền của một thành viên (kể cả đóng tiền mặt chưa báo). */
    void confirmPayment(Long transactionId, Long userId);

    /** Thủ quỹ không thấy tiền về: trả khoản đó về trạng thái chưa đóng. */
    void rejectPayment(Long transactionId, Long userId);

    // ==================== TRUY VẤN ====================

    /** Khoản thu user còn phải đóng (gồm cả khoản đã báo, đang chờ xác nhận). */
    List<Transaction> findPendingIncomeForUser(Long userId);

    /** Id các khoản thu user đã báo chuyển, đang chờ thủ quỹ xác nhận. */
    Set<Long> findWaitingConfirmationIds(Long userId);

    /** Khoản chi của các nhóm user tham gia. */
    List<Transaction> findExpensesForMember(Long userId);

    /** Mọi khoản user được yêu cầu đóng. */
    List<TransactionParticipant> findContributionsOfUser(Long userId);

    List<TransactionParticipant> findPaidContributionsOfUser(Long userId);

    long countAll();

    long countByType(String type);
}
