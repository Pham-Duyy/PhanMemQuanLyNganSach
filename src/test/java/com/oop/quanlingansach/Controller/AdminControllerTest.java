package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.TestWebConfig;
import org.springframework.context.annotation.Import;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import com.oop.quanlingansach.Service.GroupService;
import com.oop.quanlingansach.Service.TransactionService;
import com.oop.quanlingansach.Service.UserService;
import com.oop.quanlingansach.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Dashboard admin và phân quyền khu vực /admin.
 */
@Import(TestWebConfig.class)
@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private GroupService groupService;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void dashboard_Admin_ShouldRenderWithStatistics() throws Exception {
        when(userService.countNormalUsers()).thenReturn(5L);

        mockMvc.perform(get("/admin/dashboard").session(TestData.sessionOf(TestData.admin())))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("totalUsers", 5L));
    }

    @Test
    void dashboard_NotLoggedIn_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void dashboard_NormalUser_ShouldBeSentToHome() throws Exception {
        mockMvc.perform(get("/admin/dashboard").session(TestData.sessionOf(TestData.member(2L))))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void adminRoot_ShouldRedirectToDashboard() throws Exception {
        mockMvc.perform(get("/admin").session(TestData.sessionOf(TestData.admin())))
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    void oldTransactionsPath_ShouldRedirectToFinancePage() throws Exception {
        mockMvc.perform(get("/admin/transactions").session(TestData.sessionOf(TestData.admin())))
                .andExpect(redirectedUrl("/admin/finance/transactions"));
    }
}
