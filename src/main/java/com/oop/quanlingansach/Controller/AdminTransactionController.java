package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.Config.SessionKeys;
import com.oop.quanlingansach.Dto.TransactionForm;
import com.oop.quanlingansach.Model.Transaction;
import com.oop.quanlingansach.Model.TransactionParticipant;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Service.BusinessException;
import com.oop.quanlingansach.Service.GroupService;
import com.oop.quanlingansach.Service.TransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Quản lý giao dịch thu/chi và xác nhận tiền, trong các nhóm thuộc phạm vi quản lý.
 */
@Controller
@RequestMapping("/admin/finance/transactions")
public class AdminTransactionController {

    private static final String LIST_PAGE = "redirect:/admin/finance/transactions";

    private final TransactionService transactionService;
    private final GroupService groupService;

    public AdminTransactionController(TransactionService transactionService, GroupService groupService) {
        this.transactionService = transactionService;
        this.groupService = groupService;
    }

    @GetMapping
    public String list(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser, Model model) {
        model.addAttribute("transactions", transactionService.findManaged(currentUser));
        model.addAttribute("groups", groupService.findManaged(currentUser, null));
        return "admin/finance/transactions";
    }

    @GetMapping("/detail/{id}")
    public String detail(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                         @PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Transaction transaction;
        try {
            transaction = transactionService.getManagedTransaction(id, currentUser);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return LIST_PAGE;
        }

        List<TransactionParticipant> participants = transaction.getParticipants();
        long paidCount = participants.stream().filter(TransactionParticipant::isPaid).count();
        long waitingCount = participants.stream().filter(TransactionParticipant::isWaitingConfirmation).count();

        model.addAttribute("transaction", transaction);
        model.addAttribute("participants", participants);
        model.addAttribute("totalParticipants", participants.size());
        model.addAttribute("paidCount", paidCount);
        model.addAttribute("waitingCount", waitingCount);
        model.addAttribute("unpaidCount", participants.size() - paidCount);
        model.addAttribute("paidPercentage", participants.isEmpty() ? 0 : (int) (paidCount * 100 / participants.size()));
        return "admin/finance/transaction-detail";
    }

    // Danh sách thành viên nhóm cho ô "chọn người phải đóng" (AJAX)
    @GetMapping("/group/{groupId}/members")
    @ResponseBody
    public List<User> groupMembers(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                                   @PathVariable Long groupId) {
        try {
            return groupService.getManagedGroup(groupId, currentUser).getMembers();
        } catch (BusinessException e) {
            return List.of();
        }
    }

    @PostMapping("/create")
    public String create(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                         @ModelAttribute TransactionForm form,
                         RedirectAttributes redirectAttributes) {
        try {
            Transaction transaction = transactionService.create(form, currentUser);
            redirectAttributes.addFlashAttribute("success", transaction.isIncome()
                    ? "Tạo khoản thu thành công! Thành viên được chọn sẽ thấy khoản cần đóng trong mục Giao dịch."
                    : "Tạo khoản chi thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return LIST_PAGE;
    }

    @PostMapping("/{id}/update")
    public String update(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                         @PathVariable Long id, @ModelAttribute TransactionForm form,
                         RedirectAttributes redirectAttributes) {
        try {
            transactionService.update(id, form, currentUser);
            redirectAttributes.addFlashAttribute("success", "Cập nhật giao dịch thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return LIST_PAGE;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                         @PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            transactionService.cancel(id, currentUser);
            redirectAttributes.addFlashAttribute("success", "Đã hủy giao dịch! Lịch sử vẫn được giữ lại.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return LIST_PAGE;
    }

    @PostMapping("/{id}/delete")
    public String delete(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                         @PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            transactionService.delete(id, currentUser);
            redirectAttributes.addFlashAttribute("success", "Đã xóa giao dịch!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return LIST_PAGE;
    }

    // ==================== THỦ QUỸ XÁC NHẬN TIỀN ====================

    @PostMapping("/{id}/participants/{userId}/confirm")
    public String confirmPayment(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                                 @PathVariable Long id, @PathVariable Long userId,
                                 RedirectAttributes redirectAttributes) {
        try {
            transactionService.confirmPayment(id, userId, currentUser);
            redirectAttributes.addFlashAttribute("success", "Đã xác nhận nhận tiền!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return detailPage(id);
    }

    @PostMapping("/{id}/participants/{userId}/reject")
    public String rejectPayment(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                                @PathVariable Long id, @PathVariable Long userId,
                                RedirectAttributes redirectAttributes) {
        try {
            transactionService.rejectPayment(id, userId, currentUser);
            redirectAttributes.addFlashAttribute("success", "Đã từ chối. Thành viên sẽ phải chuyển lại.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return detailPage(id);
    }

    private String detailPage(Long id) {
        return "redirect:/admin/finance/transactions/detail/" + id;
    }
}
