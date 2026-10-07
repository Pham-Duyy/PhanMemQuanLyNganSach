package com.oop.quanlingansach.Service;

import com.oop.quanlingansach.Dto.ContributionReport;
import com.oop.quanlingansach.Dto.GroupFundReport;
import com.oop.quanlingansach.Dto.ReportOverview;
import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Model.Transaction;
import com.oop.quanlingansach.Model.TransactionParticipant;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Repository.GroupRepository;
import com.oop.quanlingansach.Repository.TransactionParticipantRepository;
import com.oop.quanlingansach.Repository.TransactionRepository;
import com.oop.quanlingansach.Repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionParticipantRepository participantRepository;
    private final GroupService groupService;

    public ReportServiceImpl(UserRepository userRepository,
                             GroupRepository groupRepository,
                             TransactionRepository transactionRepository,
                             TransactionParticipantRepository participantRepository,
                             GroupService groupService) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.transactionRepository = transactionRepository;
        this.participantRepository = participantRepository;
        this.groupService = groupService;
    }

    @Override
    public ReportOverview getOverview() {
        return new ReportOverview(
                groupRepository.count(),
                transactionRepository.count(),
                userRepository.countByRole(User.Role.USER),
                participantRepository.countByPaidTrue(),
                participantRepository.countWaitingConfirmation(),
                participantRepository.countUnpaid(),
                groupRepository.sumInitialFunds(),
                participantRepository.sumAllPaid(),
                participantRepository.sumOutstanding(),
                transactionRepository.sumAllExpenses());
    }

    @Override
    public ContributionReport getContributionReport(Long groupId) {
        List<TransactionParticipant> participants = groupId != null
                ? participantRepository.findByTransaction_Group_Id(groupId)
                : participantRepository.findAll();

        List<Map<String, Object>> rows = participants.stream().map(this::toContributionRow).toList();
        // Tổng tiền = tiền đã thu thật (đã xác nhận), không cộng các khoản mới chỉ là dự kiến
        List<TransactionParticipant> paid = participants.stream().filter(TransactionParticipant::isPaid).toList();
        return new ContributionReport(rows, TransactionParticipant.totalAmount(paid));
    }

    @Override
    public GroupFundReport getGroupFundReport(Group group) {
        return new GroupFundReport(
                participantRepository.sumPaidAmountByGroup(group.getId()),
                transactionRepository.sumExpenseByGroup(group.getId()),
                groupService.getCurrentFund(group),
                transactionRepository.findExpensesByGroup(group.getId()),
                participantRepository.findByTransaction_Group_IdAndPaidTrue(group.getId()));
    }

    private Map<String, Object> toContributionRow(TransactionParticipant participant) {
        User user = participant.getUser();
        Transaction transaction = participant.getTransaction();

        Map<String, Object> row = new HashMap<>();
        row.put("userName", user.getFullName());
        row.put("userEmail", user.getEmail());
        row.put("groupName", transaction.getGroup().getName());
        row.put("transactionDescription", transaction.getDescription());
        row.put("transactionType", transaction.getType());
        row.put("amount", participant.getAmount());
        row.put("status", participant.getStatusCode());
        row.put("createdDate", transaction.getCreatedDate());
        return row;
    }
}
