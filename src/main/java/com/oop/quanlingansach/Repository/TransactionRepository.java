package com.oop.quanlingansach.Repository;

import com.oop.quanlingansach.Model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * Các truy vấn có tham số adminId: null = ban quản lý (mọi nhóm), có giá trị = chỉ nhóm của thủ quỹ đó.
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByGroup_Id(Long groupId);

    // ==================== PHẠM VI QUẢN LÝ ====================

    @Query("SELECT t FROM Transaction t WHERE (:adminId IS NULL OR t.group.adminId = :adminId) ORDER BY t.createdDate DESC")
    List<Transaction> findManaged(@Param("adminId") Long adminId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE (:adminId IS NULL OR t.group.adminId = :adminId)")
    long countManaged(@Param("adminId") Long adminId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.type = :type AND (:adminId IS NULL OR t.group.adminId = :adminId)")
    long countManagedByType(@Param("adminId") Long adminId, @Param("type") String type);

    // Tổng chi thực tế (khoản chi đã hủy được hoàn lại quỹ nên không tính)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = 'EXPENSE' AND t.status <> 'CANCELLED' " +
           "AND (:adminId IS NULL OR t.group.adminId = :adminId)")
    BigDecimal sumManagedExpenses(@Param("adminId") Long adminId);

    // ==================== THEO NHÓM ====================

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.group.id = :groupId AND t.type = 'EXPENSE' AND t.status <> 'CANCELLED'")
    BigDecimal sumExpenseByGroup(@Param("groupId") Long groupId);

    // Tổng chi thực tế theo từng nhóm, một truy vấn cho cả danh sách: [groupId, tổng]
    @Query("SELECT t.group.id, SUM(t.amount) FROM Transaction t " +
           "WHERE t.group.id IN :groupIds AND t.type = 'EXPENSE' AND t.status <> 'CANCELLED' GROUP BY t.group.id")
    List<Object[]> sumExpenseByGroups(@Param("groupIds") Collection<Long> groupIds);

    // Lịch sử chi thực tế của nhóm (không gồm khoản đã hủy), mới nhất trước
    @Query("SELECT t FROM Transaction t WHERE t.group.id = :groupId AND t.type = 'EXPENSE' AND t.status <> 'CANCELLED' " +
           "ORDER BY t.createdDate DESC")
    List<Transaction> findExpensesByGroup(@Param("groupId") Long groupId);

    // ==================== PHÍA THÀNH VIÊN ====================

    // Khoản thu user còn phải đóng: được chọn đóng, chưa đóng, giao dịch còn mở
    @Query("SELECT tp.transaction FROM TransactionParticipant tp WHERE tp.user.id = :userId AND tp.paid = false " +
           "AND tp.transaction.type = 'INCOME' AND tp.transaction.status = 'ACTIVE' ORDER BY tp.transaction.createdDate DESC")
    List<Transaction> findPendingIncomeForUser(@Param("userId") Long userId);

    // Khoản chi (chưa hủy) của các nhóm user tham gia, để thông báo cho thành viên
    @Query("SELECT t FROM Transaction t JOIN t.group g JOIN g.members m WHERE m.id = :userId AND t.type = 'EXPENSE' " +
           "AND t.status <> 'CANCELLED' ORDER BY t.createdDate DESC")
    List<Transaction> findExpensesForMember(@Param("userId") Long userId);
}
