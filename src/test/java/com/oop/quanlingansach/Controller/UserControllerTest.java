package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.TestWebConfig;
import org.springframework.context.annotation.Import;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import com.oop.quanlingansach.Model.TransactionParticipant;
import com.oop.quanlingansach.Service.GroupService;
import com.oop.quanlingansach.Service.TransactionService;
import com.oop.quanlingansach.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Dashboard user và phân quyền khu vực /user.
 */
@Import(TestWebConfig.class)
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroupService groupService;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void dashboard_ShouldShowPaidTotals() throws Exception {
        var group = TestData.group(10L, TestData.member(2L));
        var paid = new TransactionParticipant(TestData.income(5L, group, "100000"), TestData.member(2L), new BigDecimal("100000"));
        paid.confirmPaid(TestData.admin());
        when(transactionService.findPaidContributionsOfUser(2L)).thenReturn(List.of(paid));
        when(groupService.findGroupsOfMember(2L)).thenReturn(List.of(group));

        mockMvc.perform(get("/user/dashboard").session(TestData.sessionOf(TestData.member(2L))))
                .andExpect(status().isOk())
                .andExpect(view().name("user/dashboard"))
                .andExpect(model().attribute("joinedGroups", 1))
                .andExpect(model().attribute("totalContributions", 1))
                .andExpect(model().attribute("totalPaidAmount", new BigDecimal("100000")));
    }

    @Test
    void dashboard_NotLoggedIn_ShouldGoToLoginWithMessage() throws Exception {
        mockMvc.perform(get("/user/dashboard"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void dashboard_Admin_ShouldBeSentToHome() throws Exception {
        mockMvc.perform(get("/user/dashboard").session(TestData.sessionOf(TestData.admin())))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void profileAndSettings_ShouldUseSharedProfilePage() throws Exception {
        var session = TestData.sessionOf(TestData.member(2L));
        mockMvc.perform(get("/user/profile").session(session)).andExpect(redirectedUrl("/auth/profile"));
        mockMvc.perform(get("/user/settings").session(session)).andExpect(redirectedUrl("/auth/profile#doi-mat-khau"));
    }
}
