package com.oop.quanlingansach.Service;

import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Model.GroupInvite;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Repository.GroupInviteRepository;
import com.oop.quanlingansach.Repository.GroupRepository;
import com.oop.quanlingansach.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupInviteServiceImpl implements GroupInviteService {

    private final GroupInviteRepository inviteRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupInviteServiceImpl(GroupInviteRepository inviteRepository,
                                  GroupRepository groupRepository,
                                  UserRepository userRepository) {
        this.inviteRepository = inviteRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<GroupInvite> findPendingInvites(Long userId) {
        return inviteRepository.findByUser_IdAndStatus(userId, GroupInvite.STATUS_PENDING);
    }

    @Override
    public GroupInvite getPendingInviteOfUser(Long inviteId, Long userId) {
        GroupInvite invite = inviteRepository.findById(inviteId)
                .filter(i -> i.getUser().getId().equals(userId))
                .orElseThrow(() -> new BusinessException("Lời mời không tồn tại!"));
        if (!invite.isPending()) {
            throw new BusinessException("Lời mời đã được xử lý trước đó!");
        }
        return invite;
    }

    @Override
    @Transactional
    public void accept(Long inviteId, Long userId) {
        GroupInvite invite = getPendingInviteOfUser(inviteId, userId);
        if (!invite.getGroup().isActive()) {
            throw new BusinessException("Nhóm đã đóng, không thể tham gia!");
        }
        invite.accept();
        inviteRepository.save(invite);
        groupRepository.save(invite.getGroup());
    }

    @Override
    public void decline(Long inviteId, Long userId) {
        GroupInvite invite = getPendingInviteOfUser(inviteId, userId);
        invite.decline();
        inviteRepository.save(invite);
    }

    @Override
    public User invite(Long groupId, Long userId, User actor) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("Nhóm không tồn tại!"));
        AdminScope.requireManages(group, actor);
        if (!group.isActive()) {
            throw new BusinessException("Nhóm đã đóng, không thể mời thêm thành viên!");
        }
        User user = userRepository.findById(userId)
                .filter(u -> u.getRole() == User.Role.USER)
                .orElseThrow(() -> new BusinessException("Chỉ có thể mời tài khoản người dùng thường!"));
        if (group.hasMember(userId)) {
            throw new BusinessException(user.getFullName() + " đã là thành viên của nhóm!");
        }
        if (hasPendingInvite(groupId, userId)) {
            throw new BusinessException(user.getFullName() + " đang có lời mời chờ chấp nhận!");
        }
        inviteRepository.save(new GroupInvite(group, user));
        return user;
    }

    @Override
    public List<User> findInvitableUsers(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("Nhóm không tồn tại!"));
        return userRepository.findByRole(User.Role.USER).stream()
                .filter(user -> !group.hasMember(user.getId()))
                .filter(user -> !hasPendingInvite(groupId, user.getId()))
                .toList();
    }

    private boolean hasPendingInvite(Long groupId, Long userId) {
        return inviteRepository.existsByGroup_IdAndUser_IdAndStatus(groupId, userId, GroupInvite.STATUS_PENDING);
    }
}
