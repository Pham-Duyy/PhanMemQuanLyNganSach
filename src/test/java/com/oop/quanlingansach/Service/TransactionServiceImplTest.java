package com.oop.quanlingansach.Service;

import com.oop.quanlingansach.Dto.TransactionForm;
import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Model.Transaction;
import com.oop.quanlingansach.Model.TransactionParticipant;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Repository.TransactionParticipantRepository;
import com.oop.quanlingansach.Repository.TransactionRepository;
import com.oop.quanlingansach.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tạo/sửa giao dịch, kiểm tra số dư quỹ, xác nhận đóng tiền.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionParticipantRepository participantRepository;

    @Mock
    private GroupService groupService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User member2;
    private User member3;
    private Group group;

    @BeforeEach
    void setUp() {
        member2 = TestData.member(2L);
        member3 = TestData.member(3L);
        group = TestData.group(10L, member2, member3);
    }

    private TransactionForm form(String type, String amount, String... targetUserIds) {
        TransactionForm form = new TransactionForm();
        form.setGroupId(10L);
        form.setType(type);
        form.setAmount(new BigDecimal(amount));
        form.setTitle("Quỹ tháng 10");
        form.setTargetUserId(targetUserIds.length > 0 ? List.of(targetUserIds) : null);
        return form;
    }

    // ===================== TẠO KHOẢN THU =====================
    @Test
    void createIncome_NoTargetSelected_ShouldCreateUnpaidParticipantForEveryMember() {
        when(groupService.lockForUpdate(10L)).thenReturn(group);

        transactionService.create(form("INCOME", "100000"), TestData.admin());

        ArgumentCaptor<TransactionParticipant> captor = ArgumentCaptor.forClass(TransactionParticipant.class);
        verify(participantRepository, times(2)).save(captor.capture());
        assertTrue(captor.getAllValues().stream().noneMatch(TransactionParticipant::isPaid));
        assertTrue(captor.getAllValues().stream().allMatch(p -> p.getAmount().compareTo(new BigDecimal("100000")) == 0));
    }

    @Test
    void createIncome_SelectedMember_ShouldOnlyCreateParticipantForThatMember() {
        when(groupService.lockForUpdate(10L)).thenReturn(group);

        transactionService.create(form("INCOME", "50000", "3"), TestData.admin());

        ArgumentCaptor<TransactionParticipant> captor = ArgumentCaptor.forClass(TransactionParticipant.class);
        verify(participantRepository).save(captor.capture());
        assertEquals(3L, captor.getValue().getUser().getId());
    }

    @Test
    void createIncome_SelectedUserNotInGroup_ShouldBeIgnored() {
        when(groupService.lockForUpdate(10L)).thenReturn(group);

        assertThrows(BusinessException.class,
                () -> transactionService.create(form("INCOME", "50000", "99"), TestData.admin()));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createIncome_GroupWithoutMembers_ShouldFail() {
        when(groupService.lockForUpdate(10L)).thenReturn(TestData.group(10L));

        assertThrows(BusinessException.class, () -> transactionService.create(form("INCOME", "50000"), TestData.admin()));
        verify(transactionRepository, never()).save(any());
    }

    // ===================== KIỂM TRA DỮ LIỆU =====================
    @Test
    void create_NonPositiveAmount_ShouldFail() {
        when(groupService.lockForUpdate(10L)).thenReturn(group);

        assertThrows(BusinessException.class, () -> transactionService.create(form("INCOME", "-5000"), TestData.admin()));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void create_InvalidType_ShouldFail() {
        when(groupService.lockForUpdate(10L)).thenReturn(group);

        assertThrows(BusinessException.class, () -> transactionService.create(form("GIFT", "5000"), TestData.admin()));
    }

    @Test
    void create_InvalidDueDate_ShouldFail() {
        when(groupService.lockForUpdate(10L)).thenReturn(group);
        TransactionForm form = form("INCOME", "5000");
        form.setDueDate("không-phải-ngày");

        assertThrows(BusinessException.class, () -> transactionService.create(form, TestData.admin()));
    }

    // ===================== TẠO KHOẢN CHI =====================
    @Test
    void createExpense_MoreThanFund_ShouldFail() {
        when(groupService.lockForUpdate(10L)).thenReturn(group);
        when(groupService.getCurrentFund(group)).thenReturn(new BigDecimal("100000"));

        assertThrows(BusinessException.class, () -> transactionService.create(form("EXPENSE", "200000"), TestData.admin()));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createExpense_WithinFund_ShouldSaveWithoutParticipants() {
        when(groupService.lockForUpdate(10L)).thenReturn(group);
        when(groupService.getCurrentFund(group)).thenReturn(new BigDecimal("300000"));

        Transaction created = transactionService.create(form("expense", "200000"), TestData.admin());

        assertEquals(Transaction.TYPE_EXPENSE, created.getType());
        verify(transactionRepository).save(created);
        verify(participantRepository, never()).save(any());
    }

    // ===================== CẬP NHẬT =====================
    @Test
    void update_ShouldChangeAmountOfUnpaidParticipantsOnly() {
        Transaction tx = TestData.income(5L, group, "100000");
        TransactionParticipant paid = new TransactionParticipant(tx, member2, new BigDecimal("100000"));
        paid.confirmPaid(TestData.admin());
        TransactionParticipant unpaid = new TransactionParticipant(tx, member3, new BigDecimal("100000"));
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(tx));
        when(participantRepository.findByTransaction_Id(5L)).thenReturn(List.of(paid, unpaid));

        TransactionForm form = form("INCOME", "150000");
        form.setTitle("Quỹ (sửa)");
        transactionService.update(5L, form, TestData.admin());

        assertEquals("Quỹ (sửa)", tx.getTitle());
        assertEquals(0, paid.getAmount().compareTo(new BigDecimal("100000")));
        assertEquals(0, unpaid.getAmount().compareTo(new BigDecimal("150000")));
    }

    @Test
    void updateExpense_OldAmountIsRefundedBeforeCheckingFund() {
        Transaction tx = TestData.expense(6L, group, "100000");
        when(transactionRepository.findById(6L)).thenReturn(Optional.of(tx));
        when(groupService.lockForUpdate(10L)).thenReturn(group);
        when(groupService.getCurrentFund(group)).thenReturn(new BigDecimal("50000")); // quỹ sau khi đã chi 100k

        transactionService.update(6L, form("EXPENSE", "150000"), TestData.admin()); // 50k + 100k hoàn lại = đủ 150k

        verify(groupService).lockForUpdate(10L); // phải khóa nhóm trước khi kiểm tra quỹ

        assertEquals(0, tx.getAmount().compareTo(new BigDecimal("150000")));
    }

    // ===================== QUY TRÌNH ĐÓNG TIỀN =====================

    private TransactionParticipant stubParticipant(Transaction tx, User user) {
        TransactionParticipant participant = new TransactionParticipant(tx, user, tx.getAmount());
        tx.setParticipants(new java.util.ArrayList<>(List.of(participant)));
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(participantRepository.findByTransaction_IdAndUser_Id(tx.getId(), user.getId())).thenReturn(Optional.of(participant));
        return participant;
    }

    @Test
    void reportPayment_ShouldWaitForTreasurerAndNotCountAsPaid() {
        TransactionParticipant participant = stubParticipant(TestData.income(5L, group, "100000"), member2);

        transactionService.reportPayment(5L, 2L, "FT123");

        assertTrue(participant.isWaitingConfirmation());
        assertFalse(participant.isPaid(), "Báo chuyển chưa được tính là đã đóng");
        assertEquals("Chờ xác nhận", participant.getPaidStatus());
    }

    @Test
    void reportPayment_Twice_ShouldFail() {
        TransactionParticipant participant = stubParticipant(TestData.income(5L, group, "100000"), member2);
        participant.reportPaid("FT123");

        assertThrows(BusinessException.class, () -> transactionService.reportPayment(5L, 2L, "FT123"));
    }

    @Test
    void reportPayment_NotInPayerList_ShouldFail() {
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(TestData.income(5L, group, "100000")));
        when(participantRepository.findByTransaction_IdAndUser_Id(5L, 2L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> transactionService.reportPayment(5L, 2L, "FT123"));
    }

    @Test
    void reportPayment_ExpenseOrCancelledIncome_ShouldFail() {
        Transaction cancelled = TestData.income(5L, group, "100000");
        cancelled.cancel();
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(cancelled));
        when(transactionRepository.findById(6L)).thenReturn(Optional.of(TestData.expense(6L, group, "100000")));

        assertThrows(BusinessException.class, () -> transactionService.reportPayment(5L, 2L, "FT123"));
        assertThrows(BusinessException.class, () -> transactionService.reportPayment(6L, 2L, "FT123"));
        verify(participantRepository, never()).save(any());
    }

    @Test
    void confirmPayment_LastPayer_ShouldCompleteIncome() {
        Transaction tx = TestData.income(5L, group, "100000");
        TransactionParticipant participant = stubParticipant(tx, member2);
        participant.reportPaid("FT123");

        transactionService.confirmPayment(5L, 2L, TestData.admin());

        assertTrue(participant.isPaid());
        assertNotNull(participant.getPaidDate());
        assertEquals(Transaction.STATUS_COMPLETED, tx.getStatus());
    }

    @Test
    void confirmPayment_CashWithoutReport_ShouldBeAllowed() {
        Transaction tx = TestData.income(5L, group, "100000");
        TransactionParticipant participant = stubParticipant(tx, member2);
        TransactionParticipant other = new TransactionParticipant(tx, member3, tx.getAmount());
        tx.setParticipants(new java.util.ArrayList<>(List.of(participant, other)));

        transactionService.confirmPayment(5L, 2L, TestData.admin());

        assertTrue(participant.isPaid());
        assertEquals(Transaction.STATUS_ACTIVE, tx.getStatus(), "Còn người chưa đóng thì vẫn đang thu");
    }

    @Test
    void confirmPayment_AlreadyPaid_ShouldFail() {
        TransactionParticipant participant = stubParticipant(TestData.income(5L, group, "100000"), member2);
        participant.confirmPaid(TestData.admin());

        assertThrows(BusinessException.class, () -> transactionService.confirmPayment(5L, 2L, TestData.admin()));
    }

    @Test
    void rejectPayment_ShouldReturnToUnpaid() {
        TransactionParticipant participant = stubParticipant(TestData.income(5L, group, "100000"), member2);
        participant.reportPaid("FT123");

        transactionService.rejectPayment(5L, 2L, TestData.admin());

        assertFalse(participant.isWaitingConfirmation());
        assertEquals("Chưa đóng", participant.getPaidStatus());
    }

    @Test
    void rejectPayment_NotReported_ShouldFail() {
        stubParticipant(TestData.income(5L, group, "100000"), member2);

        assertThrows(BusinessException.class, () -> transactionService.rejectPayment(5L, 2L, TestData.admin()));
    }

    // ===================== HỦY / XÓA =====================

    @Test
    void cancel_ActiveIncome_ShouldBeCancelled() {
        Transaction tx = TestData.income(5L, group, "100000");
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(tx));

        transactionService.cancel(5L, TestData.admin());

        assertTrue(tx.isCancelled());
    }

    @Test
    void cancel_CompletedIncome_ShouldFail() {
        Transaction tx = TestData.income(5L, group, "100000");
        tx.setStatus(Transaction.STATUS_COMPLETED);
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(tx));

        assertThrows(BusinessException.class, () -> transactionService.cancel(5L, TestData.admin()));
    }

    @Test
    void delete_IncomeWithoutPayments_ShouldDelete() {
        Transaction tx = TestData.income(5L, group, "100000");
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(tx));

        transactionService.delete(5L, TestData.admin());

        verify(transactionRepository).delete(tx);
    }

    @Test
    void delete_IncomeWithConfirmedPayment_ShouldFail() {
        Transaction tx = TestData.income(5L, group, "100000");
        TransactionParticipant paid = new TransactionParticipant(tx, member2, tx.getAmount());
        paid.confirmPaid(TestData.admin());
        tx.setParticipants(List.of(paid));
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(tx));

        assertThrows(BusinessException.class, () -> transactionService.delete(5L, TestData.admin()));
        verify(transactionRepository, never()).delete(any());
    }

    @Test
    void delete_Expense_ShouldFailAndSuggestCancel() {
        when(transactionRepository.findById(6L)).thenReturn(Optional.of(TestData.expense(6L, group, "100000")));

        BusinessException e = assertThrows(BusinessException.class, () -> transactionService.delete(6L, TestData.admin()));
        assertTrue(e.getMessage().contains("Hủy"));
    }

    // ===================== KHOẢN ĐANG CHỜ XÁC NHẬN =====================

    private Transaction incomeWithWaitingPayment() {
        Transaction tx = TestData.income(5L, group, "100000");
        TransactionParticipant waiting = new TransactionParticipant(tx, member2, tx.getAmount());
        waiting.reportPaid("FT123");
        tx.setParticipants(new java.util.ArrayList<>(List.of(waiting)));
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(tx));
        return tx;
    }

    @Test
    void update_ChangeAmountWhileWaitingConfirmation_ShouldFail() {
        incomeWithWaitingPayment();

        assertThrows(BusinessException.class, () -> transactionService.update(5L, form("INCOME", "150000"), TestData.admin()));
        verify(participantRepository, never()).save(any());
    }

    @Test
    void update_SameAmountWhileWaitingConfirmation_ShouldBeAllowed() {
        Transaction tx = incomeWithWaitingPayment();
        TransactionForm form = form("INCOME", "100000");
        form.setTitle("Chỉ sửa tiêu đề");

        transactionService.update(5L, form, TestData.admin());

        assertEquals("Chỉ sửa tiêu đề", tx.getTitle());
    }

    @Test
    void cancelOrDelete_WhileWaitingConfirmation_ShouldFail() {
        Transaction tx = incomeWithWaitingPayment();

        assertThrows(BusinessException.class, () -> transactionService.cancel(5L, TestData.admin()));
        assertThrows(BusinessException.class, () -> transactionService.delete(5L, TestData.admin()));
        assertTrue(tx.isActive());
        verify(transactionRepository, never()).delete(any());
    }

    @Test
    void createExpense_ShouldLockGroupBeforeCheckingFund() {
        when(groupService.lockForUpdate(10L)).thenReturn(group);
        when(groupService.getCurrentFund(group)).thenReturn(new BigDecimal("300000"));

        transactionService.create(form("EXPENSE", "100000"), TestData.admin());

        org.mockito.InOrder order = inOrder(groupService);
        order.verify(groupService).lockForUpdate(10L);
        order.verify(groupService).getCurrentFund(group);
    }

    // ===================== PHÂN QUYỀN & TÀI KHOẢN NHẬN TIỀN =====================

    @Test
    void createIncome_GroupWithoutBankAccount_ShouldFail() {
        group.setBankAccountNumber(null);
        when(groupService.lockForUpdate(10L)).thenReturn(group);

        BusinessException e = assertThrows(BusinessException.class,
                () -> transactionService.create(form("INCOME", "50000"), TestData.admin()));
        assertTrue(e.getMessage().contains("tài khoản nhận tiền"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void create_InGroupOfAnotherTreasurer_ShouldFail() {
        when(groupService.lockForUpdate(10L)).thenReturn(group); // nhóm của thủ quỹ id 1

        assertThrows(BusinessException.class,
                () -> transactionService.create(form("EXPENSE", "1000"), TestData.otherTreasurer()));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void confirmPayment_ByAnotherTreasurer_ShouldFailBeforeTouchingPayment() {
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(TestData.income(5L, group, "100000")));

        BusinessException e = assertThrows(BusinessException.class,
                () -> transactionService.confirmPayment(5L, 2L, TestData.otherTreasurer()));
        assertEquals("Bạn không quản lý nhóm này!", e.getMessage());
        verify(participantRepository, never()).save(any());
    }

    @Test
    void confirmPayment_BySystemAdmin_ShouldRecordConfirmer() {
        TransactionParticipant participant = stubParticipant(TestData.income(5L, group, "100000"), member2);
        participant.reportPaid("FT123");

        transactionService.confirmPayment(5L, 2L, TestData.systemAdmin());

        assertTrue(participant.isPaid());
        assertEquals("bql", participant.getConfirmedBy().getUsername());
        assertEquals("FT123", participant.getPaymentReference());
    }

    @Test
    void reportPayment_ReferenceTooLong_ShouldFail() {
        assertThrows(BusinessException.class,
                () -> transactionService.reportPayment(5L, 2L, "x".repeat(101)));
    }

    // ===================== NHÓM ĐÃ ĐÓNG =====================

    @Test
    void create_InClosedGroup_ShouldFail() {
        group.setActive(false);
        when(groupService.lockForUpdate(10L)).thenReturn(group);

        assertThrows(BusinessException.class, () -> transactionService.create(form("INCOME", "50000"), TestData.admin()));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void update_CancelledTransaction_ShouldFail() {
        Transaction tx = TestData.income(5L, group, "100000");
        tx.cancel();
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(tx));

        assertThrows(BusinessException.class, () -> transactionService.update(5L, form("INCOME", "150000"), TestData.admin()));
    }
}
