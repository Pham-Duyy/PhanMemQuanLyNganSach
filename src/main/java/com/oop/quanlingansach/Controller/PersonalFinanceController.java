package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.Config.SessionKeys;
import com.oop.quanlingansach.Model.TransactionParticipant;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Service.GroupService;
import com.oop.quanlingansach.Service.TransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.util.List;

/**
 * Trang thu chi cá nhân của user: lịch sử các khoản được yêu cầu đóng.
 */
@Controller
public class PersonalFinanceController {

    private final TransactionService transactionService;
    private final GroupService groupService;

    public PersonalFinanceController(TransactionService transactionService, GroupService groupService) {
        this.transactionService = transactionService;
        this.groupService = groupService;
    }

    @GetMapping("/personal-finance")
    public String personalFinance(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser, Model model) {
        List<TransactionParticipant> contributions = transactionService.findContributionsOfUser(currentUser.getId());
        List<TransactionParticipant> paid = contributions.stream().filter(TransactionParticipant::isPaid).toList();

        model.addAttribute("user", currentUser);
        model.addAttribute("contributions", contributions);
        model.addAttribute("totalContributions", contributions.size());
        model.addAttribute("totalAmount", TransactionParticipant.totalAmount(paid));
        model.addAttribute("joinedGroups", groupService.findGroupsOfMember(currentUser.getId()).size());
        return "user/personal-finance/index";
    }
}
