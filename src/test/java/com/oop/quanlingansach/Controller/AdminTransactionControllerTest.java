package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.TestWebConfig;
import org.springframework.context.annotation.Import;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Model.Transaction;
import com.oop.quanlingansach.Model.TransactionParticipant;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Service.BusinessException;
import com.oop.quanlingansach.Service.GroupService;
import com.oop.quanlingansach.Service.TransactionService;
import com.oop.quanlingansach.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Admin quản lý giao dịch: danh sách, chi tiết, tạo, sửa, xóa.
 * Nghiệp vụ (kiểm tra số dư, tạo người phải đóng...) được test ở TransactionServiceImplTest.
 */
@Import(TestWebConfig.class)
@WebMvcTest(AdminTransactionController.class)
class AdminTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private GroupService groupService;

    private MockHttpSession adminSession;
    private Group group;

    @BeforeEach
    void setUp() {
        adminSession = TestData.sessionOf(TestData.admin());
        group = TestData.group(10L, TestData.member(2L), TestData.member(3L));
    }

    @Test
    void list_ShouldRender() throws Exception {
        when(transactionService.findAll()).thenReturn(List.of(TestData.income(5L, group, "100000")));
        when(groupService.findAll()).thenReturn(List.of(group));

        mockMvc.perform(get("/admin/finance/transactions").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/finance/transactions"));
    }

    @Test
    void detail_ShouldCountPaidAndUnpaidParticipants() throws Exception {
        Transaction tx = TestData.income(5L, group, "100000");
        TransactionParticipant paid = new TransactionParticipant(tx, group.getMembers().get(0), new BigDecimal("100000"));
        paid.confirmPaid();
        TransactionParticipant waiting = new TransactionParticipant(tx, group.getMembers().get(1), new BigDecimal("100000"));
        waiting.reportPaid();
        tx.setParticipants(List.of(paid, waiting));
        when(transactionService.getById(5L)).thenReturn(tx);

        mockMvc.perform(get("/admin/finance/transactions/detail/5").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(model().attribute("paidCount", 1L))
                .andExpect(model().attribute("unpaidCount", 1L))
                .andExpect(model().attribute("waitingCount", 1L))
                .andExpect(model().attribute("paidPercentage", 50))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Từ chối")));
    }

    @Test
    void detail_NotFound_ShouldGoBackToList() throws Exception {
        when(transactionService.getById(99L)).thenThrow(new BusinessException("Giao dịch không tồn tại!"));

        mockMvc.perform(get("/admin/finance/transactions/detail/99").session(adminSession))
                .andExpect(redirectedUrl("/admin/finance/transactions"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void create_ShouldBindFormAndUseLoggedInAdmin() throws Exception {
        when(transactionService.create(any(), any())).thenReturn(TestData.income(5L, group, "100000"));

        mockMvc.perform(post("/admin/finance/transactions/create").with(csrf()).session(adminSession)
                        .param("groupId", "10").param("type", "INCOME").param("amount", "100000")
                        .param("title", "Quỹ tháng 10").param("targetUserId", "2", "3"))
                .andExpect(redirectedUrl("/admin/finance/transactions"))
                .andExpect(flash().attributeExists("success"));

        verify(transactionService).create(
                argThat(form -> form.getGroupId() == 10L && form.getTargetUserId().equals(List.of("2", "3"))),
                argThat((User creator) -> creator.getId() == 1L));
    }

    @Test
    void create_ServiceError_ShouldShowMessage() throws Exception {
        when(transactionService.create(any(), any())).thenThrow(new BusinessException("Quỹ nhóm không đủ để chi!"));

        mockMvc.perform(post("/admin/finance/transactions/create").with(csrf()).session(adminSession)
                        .param("groupId", "10").param("type", "EXPENSE").param("amount", "999999").param("title", "Chi"))
                .andExpect(flash().attribute("error", "Quỹ nhóm không đủ để chi!"));
    }

    @Test
    void update_ShouldCallService() throws Exception {
        mockMvc.perform(post("/admin/finance/transactions/5/update").with(csrf()).session(adminSession)
                        .param("amount", "150000").param("title", "Quỹ (sửa)"))
                .andExpect(flash().attributeExists("success"));

        verify(transactionService).update(eq(5L), argThat(form -> "Quỹ (sửa)".equals(form.getTitle())));
    }

    @Test
    void confirmParticipant_ShouldReturnToDetail() throws Exception {
        mockMvc.perform(post("/admin/finance/transactions/5/participants/2/confirm").with(csrf()).session(adminSession))
                .andExpect(redirectedUrl("/admin/finance/transactions/detail/5"))
                .andExpect(flash().attributeExists("success"));

        verify(transactionService).confirmPayment(5L, 2L);
    }

    @Test
    void rejectParticipant_Error_ShouldShowMessage() throws Exception {
        doThrow(new BusinessException("Thành viên này chưa báo chuyển tiền!")).when(transactionService).rejectPayment(5L, 2L);

        mockMvc.perform(post("/admin/finance/transactions/5/participants/2/reject").with(csrf()).session(adminSession))
                .andExpect(redirectedUrl("/admin/finance/transactions/detail/5"))
                .andExpect(flash().attribute("error", "Thành viên này chưa báo chuyển tiền!"));
    }

    @Test
    void cancel_ShouldCallService() throws Exception {
        mockMvc.perform(post("/admin/finance/transactions/5/cancel").with(csrf()).session(adminSession))
                .andExpect(redirectedUrl("/admin/finance/transactions"))
                .andExpect(flash().attributeExists("success"));

        verify(transactionService).cancel(5L);
    }

    @Test
    void delete_NotAllowed_ShouldShowMessage() throws Exception {
        doThrow(new BusinessException("Giao dịch đã phát sinh tiền nên không thể xóa.")).when(transactionService).delete(5L);

        mockMvc.perform(post("/admin/finance/transactions/5/delete").with(csrf()).session(adminSession))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void delete_ShouldCallService() throws Exception {
        mockMvc.perform(post("/admin/finance/transactions/5/delete").with(csrf()).session(adminSession))
                .andExpect(redirectedUrl("/admin/finance/transactions"));

        verify(transactionService).delete(5L);
    }

    @Test
    void create_NotLoggedIn_ShouldBeBlocked() throws Exception {
        mockMvc.perform(post("/admin/finance/transactions/create").with(csrf()).param("groupId", "10"))
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(transactionService);
    }

    @Test
    void groupMembersApi_ShouldNotExposePasswords() throws Exception {
        group.getMembers().get(0).setPassword("secret123");
        when(groupService.getById(10L)).thenReturn(group);

        mockMvc.perform(get("/admin/finance/transactions/group/10/members").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("member2"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }
}
