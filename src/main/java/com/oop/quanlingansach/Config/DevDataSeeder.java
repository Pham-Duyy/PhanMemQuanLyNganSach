package com.oop.quanlingansach.Config;

import com.oop.quanlingansach.Dto.TransactionForm;
import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Model.Transaction;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Repository.GroupRepository;
import com.oop.quanlingansach.Repository.UserRepository;
import com.oop.quanlingansach.Service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Tạo dữ liệu mẫu cho chế độ chạy thử (profile "dev", database H2 trong bộ nhớ).
 * Khoản thu/chi đi qua TransactionService để dữ liệu mẫu tuân theo đúng quy tắc nghiệp vụ.
 * Mọi tài khoản mẫu dùng chung mật khẩu {@value #DEMO_PASSWORD}.
 */
@Component
@Profile("dev")
class DevDataSeeder implements CommandLineRunner {

    static final String DEMO_PASSWORD = "123456";

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final TransactionService transactionService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    DevDataSeeder(UserRepository userRepository, GroupRepository groupRepository,
                  TransactionService transactionService) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.transactionService = transactionService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        createUser("bql", "Ban Quản Lý", User.Role.SYSTEM_ADMIN);
        User treasurer = createUser("thuquy", "Nguyễn Văn An", User.Role.ADMIN);
        User binh = createUser("binh", "Trần Thị Bình", User.Role.USER);
        User chi = createUser("chi", "Lê Minh Chi", User.Role.USER);
        User dung = createUser("dung", "Phạm Quốc Dũng", User.Role.USER);

        Group group = new Group("Quỹ lớp OOP N04", "Quỹ chung của lớp cho các hoạt động học kỳ", treasurer.getId());
        group.setType("OTHER");
        group.setFundAmount(new BigDecimal("2000000"));
        group.setTargetAmount(new BigDecimal("5000000"));
        group.setBankName("Vietcombank");
        group.setBankAccountNumber("0123456789");
        group.setBankAccountName("NGUYEN VAN AN");
        List.of(binh, chi, dung).forEach(group::addMember);
        group = groupRepository.save(group);

        // Khoản thu: Bình đã đóng và được xác nhận, Chi đã báo chuyển đang chờ, Dũng chưa đóng
        Transaction income = transactionService.create(
                form(group, Transaction.TYPE_INCOME, "100000", "Quỹ tháng 10"), treasurer);
        transactionService.reportPayment(income.getId(), binh.getId(), "FT2410001");
        transactionService.confirmPayment(income.getId(), binh.getId(), treasurer);
        transactionService.reportPayment(income.getId(), chi.getId(), "FT2410002");

        transactionService.create(form(group, Transaction.TYPE_EXPENSE, "300000", "Mua nước họp lớp"), treasurer);

        log.info("Dữ liệu mẫu đã sẵn sàng. Đăng nhập bằng bql / thuquy / binh / chi / dung, mật khẩu {}", DEMO_PASSWORD);
    }

    private User createUser(String username, String fullName, User.Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@demo.local");
        user.setFullName(fullName);
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRole(role);
        user.setActive(true);
        user.setCreatedDate(LocalDateTime.now());
        return userRepository.save(user);
    }

    private static TransactionForm form(Group group, String type, String amount, String title) {
        TransactionForm form = new TransactionForm();
        form.setGroupId(group.getId());
        form.setType(type);
        form.setAmount(new BigDecimal(amount));
        form.setTitle(title);
        return form;
    }
}
