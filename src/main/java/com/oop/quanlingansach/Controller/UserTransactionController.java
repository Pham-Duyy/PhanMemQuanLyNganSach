package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.Config.SessionKeys;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Service.BusinessException;
import com.oop.quanlingansach.Service.TransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * User xem các khoản cần đóng / khoản chi của nhóm và xác nhận đã chuyển tiền.
 */
@Controller
@RequestMapping("/user/finance/transactions")
public class UserTransactionController {

    private static final String LIST_PAGE = "redirect:/user/finance/transactions";

    private final TransactionService transactionService;

    public UserTransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public String list(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser, Model model) {
        model.addAttribute("incomeTransactions", transactionService.findPendingIncomeForUser(currentUser.getId()));
        model.addAttribute("waitingIds", transactionService.findWaitingConfirmationIds(currentUser.getId()));
        model.addAttribute("expenseNotifications", transactionService.findExpensesForMember(currentUser.getId()));
        return "user/finance/transactions";
    }

    // User báo đã chuyển tiền; tiền chỉ vào quỹ khi thủ quỹ xác nhận
    @PostMapping("/{id}/confirm")
    public String reportPayment(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                                @PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        try {
            transactionService.reportPayment(id, currentUser.getId());
            redirectAttributes.addFlashAttribute("success", "Đã báo chuyển tiền! Vui lòng chờ thủ quỹ xác nhận.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return LIST_PAGE;
    }
}
