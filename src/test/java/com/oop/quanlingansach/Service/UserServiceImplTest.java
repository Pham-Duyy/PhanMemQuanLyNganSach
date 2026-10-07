package com.oop.quanlingansach.Service;

import com.oop.quanlingansach.Dto.RegisterForm;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Repository.GroupRepository;
import com.oop.quanlingansach.Repository.UserRepository;
import com.oop.quanlingansach.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Đăng ký, đăng nhập (BCrypt + tương thích mật khẩu thô cũ), đổi mật khẩu, sửa hồ sơ.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupRepository groupRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private User userWithPassword(String password) {
        User user = TestData.member(2L);
        user.setPassword(password);
        return user;
    }

    private RegisterForm registerForm(String password, String confirm) {
        RegisterForm form = new RegisterForm();
        form.setUsername("duy");
        form.setEmail("duy@example.com");
        form.setFullName("Phạm Duy");
        form.setPassword(password);
        form.setConfirmPassword(confirm);
        return form;
    }

    // ===================== ĐĂNG KÝ =====================
    @Test
    void register_ShouldCreateUserWithHashedPasswordAndUserRole() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = userService.register(registerForm("123456", "123456"));

        assertEquals(User.Role.USER, user.getRole());
        assertNotNull(user.getCreatedDate());
        assertTrue(user.isActive());
        assertTrue(encoder.matches("123456", user.getPassword()));
        assertNull(user.getId());
    }

    @Test
    void register_PasswordMismatch_ShouldFail() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> userService.register(registerForm("123456", "654321")));
        assertEquals("Mật khẩu xác nhận không khớp!", e.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_ShortPassword_ShouldFail() {
        assertThrows(BusinessException.class, () -> userService.register(registerForm("123", "123")));
    }

    @Test
    void register_DuplicateUsername_ShouldFail() {
        when(userRepository.existsByUsername("duy")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.register(registerForm("123456", "123456")));
        verify(userRepository, never()).save(any());
    }

    // ===================== ĐĂNG NHẬP =====================
    @Test
    void authenticate_HashedPassword_Correct() {
        when(userRepository.findByUsername("member2")).thenReturn(Optional.of(userWithPassword(encoder.encode("123456"))));

        assertTrue(userService.authenticate("member2", "123456").isPresent());
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticate_HashedPassword_Wrong() {
        when(userRepository.findByUsername("member2")).thenReturn(Optional.of(userWithPassword(encoder.encode("123456"))));

        assertTrue(userService.authenticate("member2", "wrong").isEmpty());
    }

    @Test
    void authenticate_LegacyPlainPassword_ShouldLoginAndUpgradeToHash() {
        User legacy = userWithPassword("123456");
        when(userRepository.findByUsername("member2")).thenReturn(Optional.of(legacy));

        assertTrue(userService.authenticate("member2", "123456").isPresent());
        assertTrue(encoder.matches("123456", legacy.getPassword()));
        verify(userRepository).save(legacy);
    }

    @Test
    void authenticate_LegacyPlainPassword_Wrong() {
        when(userRepository.findByUsername("member2")).thenReturn(Optional.of(userWithPassword("123456")));

        assertTrue(userService.authenticate("member2", "wrong").isEmpty());
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticate_LockedAccountWithCorrectPassword_ShouldFail() {
        User locked = userWithPassword(encoder.encode("123456"));
        locked.setActive(false);
        when(userRepository.findByUsername("member2")).thenReturn(Optional.of(locked));

        BusinessException e = assertThrows(BusinessException.class, () -> userService.authenticate("member2", "123456"));
        assertTrue(e.getMessage().contains("bị khóa"));
    }

    @Test
    void authenticate_LockedAccountWithWrongPassword_ShouldNotRevealLock() {
        User locked = userWithPassword(encoder.encode("123456"));
        locked.setActive(false);
        when(userRepository.findByUsername("member2")).thenReturn(Optional.of(locked));

        assertTrue(userService.authenticate("member2", "wrong").isEmpty());
    }

    @Test
    void register_InvalidEmailOrUsername_ShouldFail() {
        RegisterForm badEmail = registerForm("123456", "123456");
        badEmail.setEmail("khong-phai-email");
        RegisterForm badUsername = registerForm("123456", "123456");
        badUsername.setUsername("có dấu cách");

        assertThrows(BusinessException.class, () -> userService.register(badEmail));
        assertThrows(BusinessException.class, () -> userService.register(badUsername));
        verify(userRepository, never()).save(any());
    }

    // ===================== BAN QUẢN LÝ: QUYỀN & KHÓA TÀI KHOẢN =====================

    @Test
    void changeRole_PromoteMemberWithoutGroups_ShouldBecomeTreasurer() {
        User member = TestData.member(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));
        when(groupRepository.findByMembers_Id(2L)).thenReturn(List.of());

        userService.changeRole(2L, User.Role.ADMIN, TestData.systemAdmin());

        assertEquals(User.Role.ADMIN, member.getRole());
        verify(userRepository).save(member);
    }

    @Test
    void changeRole_PromoteMemberStillInGroup_ShouldFail() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(TestData.member(2L)));
        when(groupRepository.findByMembers_Id(2L)).thenReturn(List.of(TestData.group(10L)));

        assertThrows(BusinessException.class, () -> userService.changeRole(2L, User.Role.ADMIN, TestData.systemAdmin()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeRole_DemoteTreasurerOwningGroups_ShouldFail() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestData.admin()));
        when(groupRepository.countByAdminId(1L)).thenReturn(2L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> userService.changeRole(1L, User.Role.USER, TestData.systemAdmin()));
        assertTrue(e.getMessage().contains("bàn giao"));
    }

    @Test
    void changeRole_ToSystemAdmin_ShouldBeRejected() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(TestData.member(2L)));

        assertThrows(BusinessException.class, () -> userService.changeRole(2L, User.Role.SYSTEM_ADMIN, TestData.systemAdmin()));
    }

    @Test
    void accountManagement_ShouldProtectSelfAndOtherSystemAdmins() {
        User otherSystemAdmin = TestData.user(91L, "bql2", User.Role.SYSTEM_ADMIN);
        when(userRepository.findById(91L)).thenReturn(Optional.of(otherSystemAdmin));

        assertThrows(BusinessException.class, () -> userService.setActive(90L, false, TestData.systemAdmin())); // chính mình
        assertThrows(BusinessException.class, () -> userService.setActive(91L, false, TestData.systemAdmin())); // BQL khác
        assertThrows(BusinessException.class, () -> userService.setActive(2L, false, TestData.admin()));        // thủ quỹ
        verify(userRepository, never()).save(any());
    }

    @Test
    void setActive_LockMember_ShouldDeactivate() {
        User member = TestData.member(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));

        userService.setActive(2L, false, TestData.systemAdmin());

        assertFalse(member.isActive());
    }

    // ===================== ĐỔI MẬT KHẨU =====================
    @Test
    void changePassword_ShouldStoreHashedNewPassword() {
        User user = userWithPassword(encoder.encode("old123"));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        userService.changePassword(2L, "old123", "new456", "new456");

        assertTrue(encoder.matches("new456", user.getPassword()));
    }

    @Test
    void changePassword_WrongOldPassword_ShouldFail() {
        String hash = encoder.encode("old123");
        User user = userWithPassword(hash);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class, () -> userService.changePassword(2L, "wrong", "new456", "new456"));
        assertEquals(hash, user.getPassword());
    }

    // ===================== HỒ SƠ =====================
    @Test
    void updateProfile_EmailUsedByOther_ShouldFail() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(TestData.member(2L)));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.updateProfile(2L, "Tên mới", "taken@example.com"));
        verify(userRepository, never()).save(any());
    }
}
