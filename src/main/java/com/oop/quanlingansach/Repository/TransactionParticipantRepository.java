package com.oop.quanlingansach.Repository;

import com.oop.quanlingansach.Model.TransactionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TransactionParticipantRepository extends JpaRepository<TransactionParticipant, Long> {

    List<TransactionParticipant> findByTransaction_Id(Long transactionId);

    Optional<TransactionParticipant> findByTransaction_IdAndUser_Id(Long transactionId, Long userId);

    List<TransactionParticipant> findByUser_Id(Long userId);

    List<TransactionParticipant> findByUser_IdAndPaidTrue(Long userId);

    // Khoản user đã báo chuyển nhưng thủ quỹ chưa xác nhận
    List<TransactionParticipant> findByUser_IdAndPaidFalseAndReportedDateIsNotNull(Long userId);

    // Khoản user còn nợ trong một nhóm, ở các khoản thu có trạng thái cho trước
    List<TransactionParticipant> findByUser_IdAndPaidFalseAndTransaction_Group_IdAndTransaction_Status(
            Long userId, Long groupId, String status);

    List<TransactionParticipant> findByTransaction_Group_Id(Long groupId);

    List<TransactionParticipant> findByTransaction_Group_IdAndPaidTrue(Long groupId);

    // ==================== SỐ LIỆU BÁO CÁO ====================

    long countByPaidTrue();

    // Khoản còn phải thu = chưa đóng, thuộc khoản thu đang thu (khoản thu đã hủy thì không còn phải đóng)
    @Query("SELECT COUNT(tp) FROM TransactionParticipant tp WHERE tp.paid = false AND tp.transaction.status = 'ACTIVE' " +
           "AND tp.reportedDate IS NULL")
    long countUnpaid();

    @Query("SELECT COUNT(tp) FROM TransactionParticipant tp WHERE tp.paid = false AND tp.transaction.status = 'ACTIVE' " +
           "AND tp.reportedDate IS NOT NULL")
    long countWaitingConfirmation();

    // Tiền đã thu = mọi khoản thủ quỹ đã xác nhận
    @Query("SELECT COALESCE(SUM(tp.amount), 0) FROM TransactionParticipant tp WHERE tp.paid = true")
    BigDecimal sumAllPaid();

    // Tiền còn phải thu = khoản chưa xác nhận của các khoản thu đang thu
    @Query("SELECT COALESCE(SUM(tp.amount), 0) FROM TransactionParticipant tp WHERE tp.paid = false AND tp.transaction.status = 'ACTIVE'")
    BigDecimal sumOutstanding();

    @Query("SELECT COALESCE(SUM(tp.amount), 0) FROM TransactionParticipant tp WHERE tp.transaction.group.id = :groupId AND tp.paid = true")
    BigDecimal sumPaidAmountByGroup(@Param("groupId") Long groupId);

    // Tiền đã thu theo từng nhóm, một truy vấn cho cả danh sách: [groupId, tổng]
    @Query("SELECT tp.transaction.group.id, SUM(tp.amount) FROM TransactionParticipant tp " +
           "WHERE tp.transaction.group.id IN :groupIds AND tp.paid = true GROUP BY tp.transaction.group.id")
    List<Object[]> sumPaidAmountByGroups(@Param("groupIds") Collection<Long> groupIds);
}
