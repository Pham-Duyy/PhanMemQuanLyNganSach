package com.oop.quanlingansach.Service;

import com.oop.quanlingansach.Model.GroupInvite;
import com.oop.quanlingansach.Model.User;

import java.util.List;

/**
 * Lời mời vào nhóm: admin gửi, user chấp nhận hoặc từ chối.
 */
public interface GroupInviteService {

    /** Lời mời đang chờ của user. */
    List<GroupInvite> findPendingInvites(Long userId);

    /** Lấy lời mời còn đang chờ của chính user này (lời mời đã xử lý không còn hiệu lực). */
    GroupInvite getPendingInviteOfUser(Long inviteId, Long userId);

    /** User chấp nhận lời mời -> trở thành thành viên nhóm. */
    void accept(Long inviteId, Long userId);

    void decline(Long inviteId, Long userId);

    /** Thủ quỹ (hoặc ban quản lý) mời một tài khoản USER vào nhóm. Trả về user được mời. */
    User invite(Long groupId, Long userId, User actor);

    /** Tài khoản USER chưa là thành viên và chưa có lời mời đang chờ. */
    List<User> findInvitableUsers(Long groupId);
}
