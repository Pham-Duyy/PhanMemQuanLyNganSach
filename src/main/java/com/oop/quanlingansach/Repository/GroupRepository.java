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

public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByNameContainingIgnoreCase(String keyword);

    // Các nhóm mà user là thành viên
    List<Group> findByMembers_Id(Long userId);

    @Query("SELECT COALESCE(SUM(g.fundAmount), 0) FROM Group g")
    BigDecimal sumInitialFunds();

    // SELECT ... FOR UPDATE: giữ khóa dòng của nhóm tới khi transaction kết thúc
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Group g WHERE g.id = :id")
    Optional<Group> findByIdForUpdate(@Param("id") Long id);
}
