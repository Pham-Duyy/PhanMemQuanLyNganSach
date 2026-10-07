package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.TestWebConfig;
import org.springframework.context.annotation.Import;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import com.oop.quanlingansach.Service.BusinessException;
import com.oop.quanlingansach.Service.TransactionService;
import com.oop.quanlingansach.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * User xem khoản cần đóng và xác nhận đã chuyển tiền.
 * Các điều kiện xác nhận được test ở TransactionServiceImplTest.
 */
@Import(TestWebConfig.class)
@WebMvcTest(UserTransactionController.class)
class UserTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void list_ShouldRender() throws Exception {
        var group = TestData.group(10L, TestData.member(2L));
        when(transactionService.findPendingIncomeForUser(2L)).thenReturn(List.of(TestData.income(5L, group, "100000")));
        when(transactionService.findExpensesForMember(2L)).thenReturn(List.of(TestData.expense(6L, group, "30000")));

        mockMvc.perform(get("/user/finance/transactions").session(TestData.sessionOf(TestData.member(2L))))
                .andExpect(status().isOk())
                .andExpect(view().name("user/finance/transactions"));
    }

    @Test
    void list_ReportedPayment_ShouldShowWaitingMessage() throws Exception {
        var group = TestData.group(10L, TestData.member(2L));
        when(transactionService.findPendingIncomeForUser(2L)).thenReturn(List.of(TestData.income(5L, group, "100000")));
        when(transactionService.findWaitingConfirmationIds(2L)).thenReturn(java.util.Set.of(5L));

        mockMvc.perform(get("/user/finance/transactions").session(TestData.sessionOf(TestData.member(2L))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đang chờ thủ quỹ kiểm tra")));
    }

    @Test
    void confirm_Success_ShouldCallServiceWithLoggedInUser() throws Exception {
        mockMvc.perform(post("/user/finance/transactions/5/confirm").with(csrf()).param("reference", "FT999").session(TestData.sessionOf(TestData.member(2L))))
                .andExpect(redirectedUrl("/user/finance/transactions"))
                .andExpect(flash().attributeExists("success"));

        verify(transactionService).reportPayment(5L, 2L, "FT999");
    }

    @Test
    void confirm_Error_ShouldShowMessage() throws Exception {
        doThrow(new BusinessException("Bạn đã xác nhận giao dịch này rồi!")).when(transactionService).reportPayment(5L, 2L, "FT999");

        mockMvc.perform(post("/user/finance/transactions/5/confirm").with(csrf()).param("reference", "FT999").session(TestData.sessionOf(TestData.member(2L))))
                .andExpect(flash().attribute("error", "Bạn đã xác nhận giao dịch này rồi!"));
    }

    @Test
    void confirm_NotLoggedIn_ShouldBeBlocked() throws Exception {
        mockMvc.perform(post("/user/finance/transactions/5/confirm").with(csrf())).andExpect(redirectedUrl("/login"));

        verifyNoInteractions(transactionService);
    }
}
