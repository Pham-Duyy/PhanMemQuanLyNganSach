package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.Config.ActiveUserLoader;
import com.oop.quanlingansach.Config.SecurityConfig;
import com.oop.quanlingansach.Config.SessionKeys;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Service.BusinessException;
import com.oop.quanlingansach.Service.GroupService;
import com.oop.quanlingansach.Service.TransactionService;
import com.oop.quanlingansach.Service.UserService;
import com.oop.quanlingansach.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Chống CSRF, khóa tài khoản / đổi quyền giữa phiên, và đổi session ID khi đăng nhập.
 * Dùng ActiveUserLoader giả để mô phỏng thay đổi trong database.
 */
@Import(SecurityConfig.class)
@WebMvcTest({AdminController.class, AuthController.class})
class SecurityAndSessionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private ActiveUserLoader activeUserLoader;
    @MockitoBean private UserService userService;
    @MockitoBean private GroupService groupService;
    @MockitoBean private TransactionService transactionService;

    // ===================== CSRF =====================

    @Test
    void post_WithoutCsrfToken_ShouldBeRejected() throws Exception {
        // Bị chặn ở filter bảo mật (trước Spring MVC) nên thông báo được lưu thẳng vào session
        mockMvc.perform(post("/auth/register").param("username", "attacker"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(request().sessionAttribute(
                        org.springframework.web.servlet.support.SessionFlashMapManager.class.getName() + ".FLASH_MAPS",
                        org.hamcrest.Matchers.notNullValue()));

        verify(userService, never()).register(any());
    }

    @Test
    void post_WithInvalidCsrfToken_ShouldBeRejected() throws Exception {
        mockMvc.perform(post("/auth/register").with(csrf().useInvalidToken()).param("username", "attacker"))
                .andExpect(redirectedUrl("/login"));

        verify(userService, never()).register(any());
    }

    @Test
    void loginPage_ShouldContainCsrfToken() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    // ===================== PHIÊN ĐĂNG NHẬP =====================

    @Test
    void accountLockedDuringSession_ShouldBeLoggedOutOnNextRequest() throws Exception {
        MockHttpSession session = TestData.sessionOf(TestData.admin());
        when(activeUserLoader.refresh(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("error", "Tài khoản đã bị khóa hoặc không còn tồn tại."));

        assertTrue(session.isInvalid());
    }

    @Test
    void roleRevokedDuringSession_ShouldTakeEffectImmediately() throws Exception {
        User sessionAdmin = TestData.admin();
        User nowNormalUser = TestData.user(1L, "admin", User.Role.USER); // quyền đã bị hạ trong DB
        MockHttpSession session = TestData.sessionOf(sessionAdmin);
        when(activeUserLoader.refresh(any())).thenReturn(Optional.of(nowNormalUser));

        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(redirectedUrl("/"));

        assertEquals(User.Role.USER, ((User) session.getAttribute(SessionKeys.CURRENT_USER)).getRole());
    }

    @Test
    void login_ShouldIssueNewSessionId() throws Exception {
        when(userService.authenticate("member2", "123456")).thenReturn(Optional.of(TestData.member(2L)));
        MockHttpSession session = new MockHttpSession();
        String idBeforeLogin = session.getId();

        mockMvc.perform(post("/login").with(csrf()).session(session)
                        .param("username", "member2").param("password", "123456"))
                .andExpect(redirectedUrl("/user/dashboard"));

        assertNotEquals(idBeforeLogin, session.getId(), "Phải cấp session ID mới sau khi đăng nhập");
    }

    @Test
    void login_LockedAccount_ShouldShowMessage() throws Exception {
        when(userService.authenticate("member2", "123456"))
                .thenThrow(new BusinessException("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên."));

        mockMvc.perform(post("/login").with(csrf()).param("username", "member2").param("password", "123456"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("error", "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên."));
    }
}
