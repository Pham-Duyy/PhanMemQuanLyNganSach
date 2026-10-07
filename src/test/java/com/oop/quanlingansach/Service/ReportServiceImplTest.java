package com.oop.quanlingansach.Service;

import com.oop.quanlingansach.Dto.ContributionReport;
import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Model.Transaction;
import com.oop.quanlingansach.Model.TransactionParticipant;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Báo cáo đóng góp: phân loại đúng 4 trạng thái và chỉ cộng tiền đã thu thật.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionParticipantRepository participantRepository;
    @Mock private GroupService groupService;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void contributionReport_ShouldClassifyFourStatusesAndSumOnlyPaid() {
        Group group = TestData.group(10L, TestData.member(2L), TestData.member(3L), TestData.member(4L), TestData.member(5L));
        Transaction active = TestData.income(5L, group, "100000");
        Transaction cancelled = TestData.income(6L, group, "100000");
        cancelled.cancel();

        TransactionParticipant paid = new TransactionParticipant(active, group.getMembers().get(0), new BigDecimal("100000"));
        paid.confirmPaid();
        TransactionParticipant waiting = new TransactionParticipant(active, group.getMembers().get(1), new BigDecimal("100000"));
        waiting.reportPaid();
        TransactionParticipant unpaid = new TransactionParticipant(active, group.getMembers().get(2), new BigDecimal("100000"));
        TransactionParticipant cancelledDue = new TransactionParticipant(cancelled, group.getMembers().get(3), new BigDecimal("100000"));
        when(participantRepository.findAll()).thenReturn(List.of(paid, waiting, unpaid, cancelledDue));

        ContributionReport report = reportService.getContributionReport(null);

        List<Object> statuses = report.rows().stream().map((Map<String, Object> row) -> row.get("status")).toList();
        assertEquals(List.of("PAID", "PENDING", "UNPAID", "CANCELLED"), statuses);
        assertEquals(0, report.paidAmount().compareTo(new BigDecimal("100000")), "Chỉ cộng tiền đã xác nhận");
    }
}
