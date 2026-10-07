package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Service.BusinessException;
import com.oop.quanlingansach.Service.UserService;
import com.oop.quanlingansach.TestData;
import com.oop.quanlingansach.TestWebConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Trang quản lý người dùng: chỉ ban quản lý vào được; bổ nhiệm/thu hồi thủ quỹ, khóa/mở khóa.
 * Các quy tắc an toàn được test ở UserServiceImplTest.
 */
@Import(TestWebConfig.class)
@WebMvcTest(UserAdminController.class)
class UserAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void list_SystemAdmin_ShouldRender() throws Exception {
        when(userService.findAllUsers()).thenReturn(List.of(TestData.systemAdmin(), TestData.admin(), TestData.member(2L)));

        mockMvc.perform(get("/admin/users").session(TestData.sessionOf(TestData.systemAdmin())))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/index"))
                .andExpect(content().string(containsString("Bổ nhiệm thủ quỹ")))
                .andExpect(content().string(containsString("Thu hồi thủ quỹ")));
    }

    @Test
    void list_Treasurer_ShouldBeSentToHome() throws Exception {
        mockMvc.perform(get("/admin/users").session(TestData.sessionOf(TestData.admin())))
                .andExpect(redirectedUrl("/"));

        verifyNoInteractions(userService);
    }

    @Test
    void promote_ShouldCallServiceWithRole() throws Exception {
        mockMvc.perform(post("/admin/users/2/role").with(csrf()).param("role", "ADMIN")
                        .session(TestData.sessionOf(TestData.systemAdmin())))
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("success", "Đã bổ nhiệm thủ quỹ!"));

        verify(userService).changeRole(eq(2L), eq(User.Role.ADMIN), argThat(User::isSystemAdmin));
    }

    @Test
    void demote_RuleViolation_ShouldShowMessage() throws Exception {
        doThrow(new BusinessException("Hãy bàn giao các nhóm trước khi thu hồi quyền."))
                .when(userService).changeRole(eq(1L), eq(User.Role.USER), any());

        mockMvc.perform(post("/admin/users/1/role").with(csrf()).param("role", "USER")
                        .session(TestData.sessionOf(TestData.systemAdmin())))
                .andExpect(flash().attribute("error", "Hãy bàn giao các nhóm trước khi thu hồi quyền."));
    }

    @Test
    void lockAndUnlock_ShouldCallService() throws Exception {
        var session = TestData.sessionOf(TestData.systemAdmin());
        mockMvc.perform(post("/admin/users/2/lock").with(csrf()).session(session)).andExpect(redirectedUrl("/admin/users"));
        mockMvc.perform(post("/admin/users/2/unlock").with(csrf()).session(session)).andExpect(redirectedUrl("/admin/users"));

        verify(userService).setActive(eq(2L), eq(false), any());
        verify(userService).setActive(eq(2L), eq(true), any());
    }
}
