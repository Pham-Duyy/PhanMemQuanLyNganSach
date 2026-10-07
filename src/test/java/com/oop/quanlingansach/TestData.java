package com.oop.quanlingansach;

import com.oop.quanlingansach.Config.SessionKeys;
import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Model.Transaction;
import com.oop.quanlingansach.Model.User;
import org.springframework.mock.web.MockHttpSession;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Dữ liệu mẫu dùng chung cho các test.
 */
public final class TestData {

    private TestData() {}

    public static User user(Long id, String username, User.Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setFullName("Họ tên " + username);
        user.setEmail(username + "@example.com");
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    public static User admin() {
        return user(1L, "admin", User.Role.ADMIN);
    }

    public static User member(Long id) {
        return user(id, "member" + id, User.Role.USER);
    }

    public static Group group(Long id, User... members) {
        Group group = new Group("Nhóm " + id, "Mô tả nhóm", 1L);
        group.setId(id);
        group.setMembers(new ArrayList<>(List.of(members)));
        return group;
    }

    public static Transaction income(Long id, Group group, String amount) {
        Transaction transaction = new Transaction("Thu quỹ", null, new BigDecimal(amount),
                Transaction.TYPE_INCOME, group, admin(), null);
        transaction.setId(id);
        return transaction;
    }

    public static Transaction expense(Long id, Group group, String amount) {
        Transaction transaction = new Transaction("Chi tiêu", null, new BigDecimal(amount),
                Transaction.TYPE_EXPENSE, group, admin(), null);
        transaction.setId(id);
        return transaction;
    }

    public static MockHttpSession sessionOf(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER, user);
        return session;
    }
}
