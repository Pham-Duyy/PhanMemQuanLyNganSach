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
 * Trang thu chi cá nhân của user.
 */
@Import(TestWebConfig.class)
@WebMvcTest(PersonalFinanceController.class)
class PersonalFinanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private GroupService groupService;

    @Test
    void personalFinance_ShouldCountAllAndSumOnlyPaid() throws Exception {
        var member = TestData.member(2L);
        var group = TestData.group(10L, member);
        var paid = new TransactionParticipant(TestData.income(5L, group, "100000"), member, new BigDecimal("100000"));
        paid.confirmPaid(TestData.admin());
        var unpaid = new TransactionParticipant(TestData.income(6L, group, "40000"), member, new BigDecimal("40000"));
        when(transactionService.findContributionsOfUser(2L)).thenReturn(List.of(paid, unpaid));

        mockMvc.perform(get("/personal-finance").session(TestData.sessionOf(member)))
                .andExpect(status().isOk())
                .andExpect(view().name("user/personal-finance/index"))
                .andExpect(model().attribute("totalContributions", 2))
                .andExpect(model().attribute("totalAmount", new BigDecimal("100000")));
    }

    @Test
    void personalFinance_NotLoggedIn_ShouldGoToLogin() throws Exception {
        mockMvc.perform(get("/personal-finance")).andExpect(redirectedUrl("/login"));
    }
}
