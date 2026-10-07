package com.oop.quanlingansach.Service;

import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Model.Transaction;
import com.oop.quanlingansach.Model.TransactionParticipant;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Repository.GroupRepository;
import com.oop.quanlingansach.Repository.TransactionParticipantRepository;
import com.oop.quanlingansach.Repository.TransactionRepository;
import com.oop.quanlingansach.Repository.UserRepository;
import com.oop.quanlingansach.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Số dư quỹ, tạo nhóm an toàn, rời nhóm.
 */
@ExtendWith(MockitoExtension.class)
class GroupServiceImplTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionParticipantRepository participantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GroupServiceImpl groupService;

    @Test
    void getCurrentFund_ShouldBeInitialPlusPaidMinusExpense() {
        Group group = TestData.group(10L);
        group.setFundAmount(new BigDecimal("500000"));
        when(participantRepository.sumPaidAmountByGroup(10L)).thenReturn(new BigDecimal("300000"));
        when(transactionRepository.sumExpenseByGroup(10L)).thenReturn(new BigDecimal("200000"));

        assertEquals(0, groupService.getCurrentFund(group).compareTo(new BigDecimal("600000")));
    }

    @Test
    void create_ShouldIgnoreIdAndMembersSentFromForm() {
        Group form = TestData.group(99L, TestData.member(2L));
        form.setName("Nhóm mới");
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

        Group created = groupService.create(form, TestData.admin());

        assertNull(created.getId());
        assertTrue(created.getMembers().isEmpty());
        assertEquals("Nhóm mới", created.getName());
        assertEquals(1L, created.getAdminId());
    }

    @Test
    void create_InvalidForm_ShouldFail() {
        Group blankName = TestData.group(1L);
        blankName.setName("  ");
        Group negativeFund = TestData.group(1L);
        negativeFund.setFundAmount(new BigDecimal("-1"));
        Group badType = TestData.group(1L);
        badType.setType("HACK");

        assertThrows(BusinessException.class, () -> groupService.create(blankName, TestData.admin()));
        assertThrows(BusinessException.class, () -> groupService.create(negativeFund, TestData.admin()));
        assertThrows(BusinessException.class, () -> groupService.create(badType, TestData.admin()));
        verify(groupRepository, never()).save(any());
    }

    @Test
    void getCurrentFunds_ShouldUseOneGroupedQueryPerKindForAllGroups() {
        Group g1 = TestData.group(1L);
        g1.setFundAmount(new BigDecimal("100000"));
        Group g2 = TestData.group(2L);
        when(participantRepository.sumPaidAmountByGroups(List.of(1L, 2L)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, new BigDecimal("50000")}));
        when(transactionRepository.sumExpenseByGroups(List.of(1L, 2L)))
                .thenReturn(List.<Object[]>of(new Object[]{2L, new BigDecimal("30000")}));

        var funds = groupService.getCurrentFunds(List.of(g1, g2));

        assertEquals(0, funds.get(1L).compareTo(new BigDecimal("150000")));
        assertEquals(0, funds.get(2L).compareTo(new BigDecimal("-30000")));
        verify(participantRepository, never()).sumPaidAmountByGroup(any());
    }

    // ===================== PHẠM VI QUẢN LÝ =====================

    @Test
    void findManaged_Treasurer_ShouldOnlyQueryOwnGroups() {
        groupService.findManaged(TestData.admin(), "  ");

        verify(groupRepository).findManaged(1L, null); // từ khóa trống = không lọc tên
    }

    @Test
    void findManaged_SystemAdmin_ShouldQueryAllGroups() {
        groupService.findManaged(TestData.systemAdmin(), " CLB ");

        verify(groupRepository).findManaged(null, "CLB");
    }

    @Test
    void getManagedGroup_OfAnotherTreasurer_ShouldFail() {
        when(groupRepository.findById(10L)).thenReturn(Optional.of(TestData.group(10L))); // của thủ quỹ id 1

        BusinessException e = assertThrows(BusinessException.class,
                () -> groupService.getManagedGroup(10L, TestData.otherTreasurer()));
        assertEquals("Bạn không quản lý nhóm này!", e.getMessage());
        assertEquals(10L, groupService.getManagedGroup(10L, TestData.systemAdmin()).getId());
    }

    @Test
    void delete_ByAnotherTreasurer_ShouldFail() {
        when(groupRepository.findById(10L)).thenReturn(Optional.of(TestData.group(10L)));

        assertThrows(BusinessException.class, () -> groupService.delete(10L, TestData.otherTreasurer()));
        verify(groupRepository, never()).deleteById(any());
    }

    // ===================== BÀN GIAO THỦ QUỸ =====================

    @Test
    void transferTreasurer_BySystemAdmin_ShouldChangeOwner() {
        Group group = TestData.group(10L);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(userRepository.findById(50L)).thenReturn(Optional.of(TestData.otherTreasurer()));

        groupService.transferTreasurer(10L, 50L, TestData.systemAdmin());

        assertEquals(50L, group.getAdminId());
        verify(groupRepository).save(group);
    }

    @Test
    void transferTreasurer_ByTreasurer_ShouldFail() {
        assertThrows(BusinessException.class, () -> groupService.transferTreasurer(10L, 50L, TestData.admin()));
        verify(groupRepository, never()).save(any());
    }

    @Test
    void transferTreasurer_ToNormalOrLockedUser_ShouldFail() {
        User locked = TestData.otherTreasurer();
        locked.setActive(false);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(TestData.group(10L)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(TestData.member(2L)));
        when(userRepository.findById(50L)).thenReturn(Optional.of(locked));

        assertThrows(BusinessException.class, () -> groupService.transferTreasurer(10L, 2L, TestData.systemAdmin()));
        assertThrows(BusinessException.class, () -> groupService.transferTreasurer(10L, 50L, TestData.systemAdmin()));
        verify(groupRepository, never()).save(any());
    }

    // ===================== KIỂM TRA DỮ LIỆU =====================

    @Test
    void create_IncompleteOrInvalidBankAccount_ShouldFail() {
        Group missingHolder = TestData.group(1L);
        missingHolder.setBankAccountName(" ");
        Group lettersInNumber = TestData.group(1L);
        lettersInNumber.setBankAccountNumber("12AB5678");

        assertThrows(BusinessException.class, () -> groupService.create(missingHolder, TestData.admin()));
        assertThrows(BusinessException.class, () -> groupService.create(lettersInNumber, TestData.admin()));
        verify(groupRepository, never()).save(any());
    }

    @Test
    void create_BankAccountName_ShouldBeUppercased() {
        Group form = TestData.group(1L);
        form.setBankAccountName("Nguyen Van A");
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("NGUYEN VAN A", groupService.create(form, TestData.admin()).getBankAccountName());
    }

    @Test
    void delete_GroupWithTransactions_ShouldFail() {
        when(groupRepository.findById(10L)).thenReturn(Optional.of(TestData.group(10L)));
        when(transactionRepository.existsByGroup_Id(10L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> groupService.delete(10L, TestData.admin()));
        verify(groupRepository, never()).deleteById(any());
    }

    @Test
    void update_ChangeInitialFundAfterTransactions_ShouldFail() {
        Group group = TestData.group(10L);
        group.setFundAmount(new BigDecimal("500000"));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(transactionRepository.existsByGroup_Id(10L)).thenReturn(true);
        Group form = TestData.group(10L);
        form.setFundAmount(new BigDecimal("900000"));

        assertThrows(BusinessException.class, () -> groupService.update(10L, form, TestData.admin()));
    }

    @Test
    void update_SameInitialFundAfterTransactions_ShouldBeAllowed() {
        Group group = TestData.group(10L);
        group.setFundAmount(new BigDecimal("500000"));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        Group form = TestData.group(10L);
        form.setName("Tên mới");
        form.setFundAmount(new BigDecimal("500000"));

        groupService.update(10L, form, TestData.admin());

        assertEquals("Tên mới", group.getName());
    }

    @Test
    void leave_WithUnpaidDues_ShouldFail() {
        User member = TestData.member(2L);
        Group group = TestData.group(10L, member);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        Transaction income = TestData.income(5L, group, "100000");
        when(participantRepository.findByUser_IdAndPaidFalseAndTransaction_Group_IdAndTransaction_Status(2L, 10L, Transaction.STATUS_ACTIVE))
                .thenReturn(List.of(new TransactionParticipant(income, member, income.getAmount())));

        assertThrows(BusinessException.class, () -> groupService.leave(10L, 2L));
        assertTrue(group.hasMember(2L));
    }

    @Test
    void removeMember_ShouldDropTheirUnpaidDues() {
        User member = TestData.member(2L);
        Group group = TestData.group(10L, member);
        Transaction income = TestData.income(5L, group, "100000");
        TransactionParticipant due = new TransactionParticipant(income, member, income.getAmount());
        income.setParticipants(new ArrayList<>(List.of(due)));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(participantRepository.findByUser_IdAndPaidFalseAndTransaction_Group_IdAndTransaction_Status(2L, 10L, Transaction.STATUS_ACTIVE))
                .thenReturn(List.of(due));

        groupService.removeMember(10L, 2L, TestData.admin());

        assertFalse(group.hasMember(2L));
        assertTrue(income.getParticipants().isEmpty());
    }

    @Test
    void removeMember_WithReportedPayment_ShouldFail() {
        User member = TestData.member(2L);
        Group group = TestData.group(10L, member);
        Transaction income = TestData.income(5L, group, "100000");
        TransactionParticipant due = new TransactionParticipant(income, member, income.getAmount());
        due.reportPaid("FT123");
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(participantRepository.findByUser_IdAndPaidFalseAndTransaction_Group_IdAndTransaction_Status(2L, 10L, Transaction.STATUS_ACTIVE))
                .thenReturn(List.of(due));

        assertThrows(BusinessException.class, () -> groupService.removeMember(10L, 2L, TestData.admin()));
        assertTrue(group.hasMember(2L));
    }

    @Test
    void leave_NotAMember_ShouldFail() {
        when(groupRepository.findById(10L)).thenReturn(Optional.of(TestData.group(10L)));

        assertThrows(BusinessException.class, () -> groupService.leave(10L, 2L));
        verify(groupRepository, never()).save(any());
    }

    @Test
    void leave_Member_ShouldBeRemoved() {
        Group group = TestData.group(10L, TestData.member(2L));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));

        groupService.leave(10L, 2L);

        assertFalse(group.hasMember(2L));
        verify(groupRepository).save(group);
    }

    @Test
    void getById_NotFound_ShouldFail() {
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> groupService.getById(99L));
        assertEquals("Nhóm không tồn tại!", e.getMessage());
    }
}
