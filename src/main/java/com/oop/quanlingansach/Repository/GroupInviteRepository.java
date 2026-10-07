package com.oop.quanlingansach.Repository;

import com.oop.quanlingansach.Model.GroupInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupInviteRepository extends JpaRepository<GroupInvite, Long> {

    List<GroupInvite> findByUser_IdAndStatus(Long userId, String status);

    boolean existsByGroup_IdAndUser_IdAndStatus(Long groupId, Long userId, String status);
}
