package com.oop.quanlingansach.Service;

import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Model.GroupInvite;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Repository.GroupInviteRepository;
import com.oop.quanlingansach.Repository.GroupRepository;
import com.oop.quanlingansach.Repository.UserRepository;
import com.oop.quanlingansach.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Gửi lời mời (chỉ USER, không trùng), chấp nhận và từ chối lời mời.
 */
@ExtendWith(MockitoExtension.class)
class GroupInviteServiceImplTest {

    @Mock
    private GroupInviteRepository inviteRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GroupInviteServiceImpl inviteService;

    private GroupInvite pendingInvite(Long id, Group group, User user) {
        GroupInvite invite = new GroupInvite(group, user);
        ReflectionTestUtils.setField(invite, "id", id);
        return invite;
    }

    // ===================== GỬI LỜI MỜI =====================
    @Test
    void invite_NormalUser_ShouldSaveInvite() {
        Group group = TestData.group(10L);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(userRepository.findById(2L)).thenReturn(Optional.of(TestData.member(2L)));

        User invited = inviteService.invite(10L, 2L);

        assertEquals(2L, invited.getId());
        verify(inviteRepository).save(any(GroupInvite.class));
    }

    @Test
    void invite_ClosedGroup_ShouldFail() {
        Group closed = TestData.group(10L);
        closed.setActive(false);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(closed));

        assertThrows(BusinessException.class, () -> inviteService.invite(10L, 2L));
        verify(inviteRepository, never()).save(any());
    }

    @Test
    void accept_ClosedGroup_ShouldFail() {
        Group closed = TestData.group(10L);
        closed.setActive(false);
        GroupInvite invite = pendingInvite(7L, closed, TestData.member(2L));
        when(inviteRepository.findById(7L)).thenReturn(Optional.of(invite));

        assertThrows(BusinessException.class, () -> inviteService.accept(7L, 2L));
        assertFalse(closed.hasMember(2L));
    }

    @Test
    void invite_AdminAccount_ShouldFail() {
        when(groupRepository.findById(10L)).thenReturn(Optional.of(TestData.group(10L)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestData.admin()));

        assertThrows(BusinessException.class, () -> inviteService.invite(10L, 1L));
        verify(inviteRepository, never()).save(any());
    }

    @Test
    void invite_AlreadyMember_ShouldFail() {
        User member = TestData.member(2L);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(TestData.group(10L, member)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));

        assertThrows(BusinessException.class, () -> inviteService.invite(10L, 2L));
        verify(inviteRepository, never()).save(any());
    }

    @Test
    void invite_PendingInviteExists_ShouldFail() {
        when(groupRepository.findById(10L)).thenReturn(Optional.of(TestData.group(10L)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(TestData.member(2L)));
        when(inviteRepository.existsByGroup_IdAndUser_IdAndStatus(10L, 2L, GroupInvite.STATUS_PENDING)).thenReturn(true);

        assertThrows(BusinessException.class, () -> inviteService.invite(10L, 2L));
        verify(inviteRepository, never()).save(any());
    }

    @Test
    void findInvitableUsers_ShouldExcludeMembersAndPendingInvites() {
        User member = TestData.member(2L);
        User invited = TestData.member(3L);
        User free = TestData.member(4L);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(TestData.group(10L, member)));
        when(userRepository.findByRole(User.Role.USER)).thenReturn(List.of(member, invited, free));
        when(inviteRepository.existsByGroup_IdAndUser_IdAndStatus(10L, 3L, GroupInvite.STATUS_PENDING)).thenReturn(true);

        assertEquals(List.of(free), inviteService.findInvitableUsers(10L));
    }

    // ===================== CHẤP NHẬN / TỪ CHỐI =====================
    @Test
    void accept_ShouldAddUserToGroup() {
        User user = TestData.member(2L);
        Group group = TestData.group(10L);
        GroupInvite invite = pendingInvite(7L, group, user);
        when(inviteRepository.findById(7L)).thenReturn(Optional.of(invite));

        inviteService.accept(7L, 2L);

        assertEquals(GroupInvite.STATUS_ACCEPTED, invite.getStatus());
        assertTrue(group.hasMember(2L));
        verify(groupRepository).save(group);
    }

    @Test
    void accept_InviteOfAnotherUser_ShouldFail() {
        GroupInvite invite = pendingInvite(7L, TestData.group(10L), TestData.member(2L));
        when(inviteRepository.findById(7L)).thenReturn(Optional.of(invite));

        assertThrows(BusinessException.class, () -> inviteService.accept(7L, 3L));
        assertTrue(invite.isPending());
    }

    @Test
    void getPendingInvite_DeclinedInvite_ShouldNotGiveAccessToGroup() {
        GroupInvite invite = pendingInvite(7L, TestData.group(10L), TestData.member(2L));
        invite.decline();
        when(inviteRepository.findById(7L)).thenReturn(Optional.of(invite));

        assertThrows(BusinessException.class, () -> inviteService.getPendingInviteOfUser(7L, 2L));
    }

    @Test
    void decline_AlreadyAccepted_ShouldFail() {
        GroupInvite invite = pendingInvite(7L, TestData.group(10L), TestData.member(2L));
        invite.setStatus(GroupInvite.STATUS_ACCEPTED);
        when(inviteRepository.findById(7L)).thenReturn(Optional.of(invite));

        assertThrows(BusinessException.class, () -> inviteService.decline(7L, 2L));
        assertEquals(GroupInvite.STATUS_ACCEPTED, invite.getStatus());
    }
}
