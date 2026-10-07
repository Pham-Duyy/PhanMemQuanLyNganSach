package com.oop.quanlingansach.Repository;

import com.oop.quanlingansach.Model.Group;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Các truy vấn có tham số adminId: null = ban quản lý (mọi nhóm), có giá trị = chỉ nhóm của thủ quỹ đó.
 */
public interface GroupRepository extends JpaRepository<Group, Long> {

    // Nhóm trong phạm vi quản lý, lọc theo tên (keyword null = không lọc), mới nhất trước
    @Query("SELECT g FROM Group g WHERE (:adminId IS NULL OR g.adminId = :adminId) " +
           "AND (:keyword IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY g.createdDate DESC")
    List<Group> findManaged(@Param("adminId") Long adminId, @Param("keyword") String keyword);

    @Query("SELECT COUNT(g) FROM Group g WHERE (:adminId IS NULL OR g.adminId = :adminId)")
    long countManaged(@Param("adminId") Long adminId);

    long countByAdminId(Long adminId);

    // Số thành viên (không trùng) của các nhóm trong phạm vi quản lý
    @Query("SELECT COUNT(DISTINCT m.id) FROM Group g JOIN g.members m WHERE (:adminId IS NULL OR g.adminId = :adminId)")
    long countMembersOfManaged(@Param("adminId") Long adminId);

    @Query("SELECT COALESCE(SUM(g.fundAmount), 0) FROM Group g WHERE (:adminId IS NULL OR g.adminId = :adminId)")
    BigDecimal sumInitialFunds(@Param("adminId") Long adminId);

    // Các nhóm mà user là thành viên
    List<Group> findByMembers_Id(Long userId);

    // SELECT ... FOR UPDATE: giữ khóa dòng của nhóm tới khi transaction kết thúc
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Group g WHERE g.id = :id")
    Optional<Group> findByIdForUpdate(@Param("id") Long id);
}
