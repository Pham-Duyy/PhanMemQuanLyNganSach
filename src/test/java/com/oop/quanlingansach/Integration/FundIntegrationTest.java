package com.oop.quanlingansach.Integration;

import com.oop.quanlingansach.Dto.TransactionForm;
import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Model.Transaction;
import com.oop.quanlingansach.Model.TransactionParticipant;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Repository.GroupRepository;
import com.oop.quanlingansach.Repository.TransactionParticipantRepository;
import com.oop.quanlingansach.Repository.TransactionRepository;
import com.oop.quanlingansach.Repository.UserRepository;
import com.oop.quanlingansach.Service.BusinessException;
import com.oop.quanlingansach.Service.GroupService;
import com.oop.quanlingansach.Service.GroupServiceImpl;
import com.oop.quanlingansach.Service.TransactionService;
import com.oop.quanlingansach.Service.TransactionServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test tích hợp với database thật (H2 trong bộ nhớ, chế độ MySQL):
 * chạy thật các truy vấn JPQL, khóa SELECT ... FOR UPDATE và transaction.
 * Không đụng tới database Aiven.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:fundtest;MODE=MySQL;LOCK_TIMEOUT=10000;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@Import({GroupServiceImpl.class, TransactionServiceImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED) // mỗi thao tác tự commit, giống khi chạy thật
class FundIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private TransactionParticipantRepository participantRepository;
    @Autowired private GroupService groupService;
    @Autowired private TransactionService transactionService;

    private User admin;
    private User member;
    private Group group;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(newUser("admin", User.Role.ADMIN));
        member = userRepository.save(newUser("member", User.Role.USER));
        Group g = new Group("Quỹ lớp", null, admin.getId());
        g.setFundAmount(new BigDecimal("1000000"));
        g.addMember(member);
        group = groupRepository.save(g);
    }

    @AfterEach
    void cleanUp() {
        participantRepository.deleteAll();
        transactionRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void concurrentExpenses_ShouldNeverOverdrawFund() throws Exception {
        int threads = 5; // 5 yêu cầu chi 300.000 cùng lúc, quỹ chỉ có 1.000.000 -> tối đa 3 yêu cầu thành công
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    transactionService.create(expenseForm("300000"), admin);
                    succeeded.incrementAndGet();
                } catch (BusinessException e) {
                    rejected.incrementAndGet();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(3, succeeded.get());
        assertEquals(2, rejected.get());
        BigDecimal fund = groupService.getCurrentFund(groupRepository.findById(group.getId()).orElseThrow());
        assertEquals(0, fund.compareTo(new BigDecimal("100000")), "Quỹ còn 100.000, không bao giờ âm");
    }

    @Test
    void paymentFlow_OnlyConfirmedMoneyCountsInFund() {
        TransactionForm form = new TransactionForm();
        form.setGroupId(group.getId());
        form.setType("INCOME");
        form.setAmount(new BigDecimal("200000"));
        form.setTitle("Quỹ tháng 10");
        Transaction income = transactionService.create(form, admin);

        // User báo đã chuyển: chưa được tính vào quỹ
        transactionService.reportPayment(income.getId(), member.getId());
        assertEquals(0, groupService.getCurrentFund(group).compareTo(new BigDecimal("1000000")));
        assertEquals(java.util.Set.of(income.getId()), transactionService.findWaitingConfirmationIds(member.getId()));

        // Thủ quỹ xác nhận: tiền vào quỹ và khoản thu tự hoàn thành (chỉ có 1 người phải đóng)
        transactionService.confirmPayment(income.getId(), member.getId());
        assertEquals(0, groupService.getCurrentFund(group).compareTo(new BigDecimal("1200000")));
        assertEquals(Transaction.STATUS_COMPLETED, transactionRepository.findById(income.getId()).orElseThrow().getStatus());
        assertEquals(0, participantRepository.sumAllPaid().compareTo(new BigDecimal("200000")));
        assertTrue(transactionService.findPendingIncomeForUser(member.getId()).isEmpty());
    }

    @Test
    void groupedFundQuery_ShouldMatchPerGroupCalculation() {
        TransactionForm form = expenseForm("250000");
        transactionService.create(form, admin);

        BigDecimal single = groupService.getCurrentFund(group);
        BigDecimal grouped = groupService.getCurrentFunds(List.of(group)).get(group.getId());

        assertEquals(0, single.compareTo(new BigDecimal("750000")));
        assertEquals(0, grouped.compareTo(single));
    }

    @Test
    void sameMemberTwiceInOneIncome_ShouldBeRejectedByDatabase() {
        Transaction income = transactionRepository.save(new Transaction("Thu", null, new BigDecimal("1000"),
                Transaction.TYPE_INCOME, group, admin, null));
        participantRepository.saveAndFlush(new TransactionParticipant(income, member, new BigDecimal("1000")));

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
                participantRepository.saveAndFlush(new TransactionParticipant(income, member, new BigDecimal("1000"))));
    }

    private TransactionForm expenseForm(String amount) {
        TransactionForm form = new TransactionForm();
        form.setGroupId(group.getId());
        form.setType("EXPENSE");
        form.setAmount(new BigDecimal(amount));
        form.setTitle("Chi tiêu");
        return form;
    }

    private static User newUser(String username, User.Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setFullName(username);
        user.setPassword("x");
        user.setRole(role);
        user.setActive(true);
        user.setCreatedDate(LocalDateTime.now());
        return user;
    }
}
