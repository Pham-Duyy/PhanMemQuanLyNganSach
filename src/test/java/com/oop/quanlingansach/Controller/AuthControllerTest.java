package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.TestWebConfig;
import org.springframework.context.annotation.Import;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import com.oop.quanlingansach.Config.SessionKeys;
import com.oop.quanlingansach.Dto.RegisterForm;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Service.BusinessException;
import com.oop.quanlingansach.Service.UserService;
import com.oop.quanlingansach.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Trang chủ, đăng nhập, đăng ký, đăng xuất, thông tin cá nhân.
 */
@Import(TestWebConfig.class)
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // ===================== TRANG CHỦ =====================
    @Test
    void home_NotLoggedIn_ShouldGoToLogin() throws Exception {
        mockMvc.perform(get("/")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void home_ShouldGoToDashboardOfRole() throws Exception {
        mockMvc.perform(get("/").session(TestData.sessionOf(TestData.admin())))
                .andExpect(redirectedUrl("/admin/dashboard"));
        mockMvc.perform(get("/").session(TestData.sessionOf(TestData.member(2L))))
                .andExpect(redirectedUrl("/user/dashboard"));
    }

    // ===================== ĐĂNG NHẬP =====================
    @Test
    void loginForm_NotLoggedIn_ShouldRender() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void loginForm_AlreadyLoggedIn_ShouldGoHome() throws Exception {
        mockMvc.perform(get("/login").session(TestData.sessionOf(TestData.member(2L))))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void login_User_ShouldStoreUserInSession() throws Exception {
        User member = TestData.member(2L);
        when(userService.authenticate("member2", "123456")).thenReturn(Optional.of(member));
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/login").with(csrf()).session(session).param("username", "member2").param("password", "123456"))
                .andExpect(redirectedUrl("/user/dashboard"));

        assertEquals(member, session.getAttribute(SessionKeys.CURRENT_USER));
    }

    @Test
    void login_Admin_ShouldGoToAdminDashboard() throws Exception {
        when(userService.authenticate("admin", "123456")).thenReturn(Optional.of(TestData.admin()));

        mockMvc.perform(post("/login").with(csrf()).param("username", "admin").param("password", "123456").param("userType", "admin"))
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    void login_WrongPassword_ShouldShowError() throws Exception {
        when(userService.authenticate("member2", "wrong")).thenReturn(Optional.empty());

        mockMvc.perform(post("/login").with(csrf()).param("username", "member2").param("password", "wrong"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void login_UserChoosingAdminType_ShouldBeRejected() throws Exception {
        when(userService.authenticate("member2", "123456")).thenReturn(Optional.of(TestData.member(2L)));
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/login").with(csrf()).session(session)
                        .param("username", "member2").param("password", "123456").param("userType", "admin"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("error"));

        assertNull(session.getAttribute(SessionKeys.CURRENT_USER));
    }

    // ===================== ĐĂNG KÝ =====================
    @Test
    void registerForm_ShouldRender() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    void register_Success_ShouldPassFormToService() throws Exception {
        mockMvc.perform(post("/auth/register").with(csrf())
                        .param("username", "newuser").param("email", "new@example.com").param("fullName", "Người Mới")
                        .param("password", "123456").param("confirmPassword", "123456")
                        .param("id", "1").param("role", "ADMIN")) // tham số lạ phải bị bỏ qua
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("success"));

        verify(userService).register(argThat((RegisterForm f) ->
                "newuser".equals(f.getUsername()) && "123456".equals(f.getConfirmPassword())));
    }

    @Test
    void register_ServiceError_ShouldShowMessage() throws Exception {
        when(userService.register(any())).thenThrow(new BusinessException("Tên đăng nhập đã tồn tại!"));

        mockMvc.perform(post("/auth/register").with(csrf()).param("username", "dup"))
                .andExpect(redirectedUrl("/auth/register"))
                .andExpect(flash().attribute("error", "Tên đăng nhập đã tồn tại!"));
    }

    // ===================== ĐĂNG XUẤT =====================
    @Test
    void logout_ShouldInvalidateSession() throws Exception {
        MockHttpSession session = TestData.sessionOf(TestData.member(2L));

        mockMvc.perform(get("/logout").session(session)).andExpect(redirectedUrl("/login"));

        assertTrue(session.isInvalid());
    }

    // ===================== THÔNG TIN CÁ NHÂN =====================
    @Test
    void profile_NotLoggedIn_ShouldGoToLogin() throws Exception {
        mockMvc.perform(get("/auth/profile")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void profile_ShouldRenderForUserAndAdmin() throws Exception {
        when(userService.getById(2L)).thenReturn(TestData.member(2L));
        when(userService.getById(1L)).thenReturn(TestData.admin());

        mockMvc.perform(get("/auth/profile").session(TestData.sessionOf(TestData.member(2L))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đổi mật khẩu")));
        mockMvc.perform(get("/auth/profile").session(TestData.sessionOf(TestData.admin())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Thủ quỹ")));
    }

    @Test
    void updateProfile_ShouldRefreshSessionUser() throws Exception {
        User updated = TestData.member(2L);
        updated.setFullName("Tên Mới");
        when(userService.updateProfile(2L, "Tên Mới", "new@example.com")).thenReturn(updated);
        MockHttpSession session = TestData.sessionOf(TestData.member(2L));

        mockMvc.perform(post("/auth/profile").with(csrf()).session(session).param("fullName", "Tên Mới").param("email", "new@example.com"))
                .andExpect(redirectedUrl("/auth/profile"))
                .andExpect(flash().attributeExists("success"));

        assertEquals("Tên Mới", ((User) session.getAttribute(SessionKeys.CURRENT_USER)).getFullName());
    }

    @Test
    void changePassword_Success_ShouldLogout() throws Exception {
        MockHttpSession session = TestData.sessionOf(TestData.member(2L));

        mockMvc.perform(post("/auth/change-password").with(csrf()).session(session)
                        .param("oldPassword", "old123").param("newPassword", "new456").param("confirmPassword", "new456"))
                .andExpect(redirectedUrl("/login"));

        verify(userService).changePassword(2L, "old123", "new456", "new456");
        assertTrue(session.isInvalid());
    }

    @Test
    void changePassword_Error_ShouldStayOnProfile() throws Exception {
        doThrow(new BusinessException("Mật khẩu cũ không đúng!"))
                .when(userService).changePassword(anyLong(), anyString(), anyString(), anyString());

        mockMvc.perform(post("/auth/change-password").with(csrf()).session(TestData.sessionOf(TestData.member(2L)))
                        .param("oldPassword", "x").param("newPassword", "new456").param("confirmPassword", "new456"))
                .andExpect(redirectedUrl("/auth/profile"))
                .andExpect(flash().attribute("error", "Mật khẩu cũ không đúng!"));
    }
}
