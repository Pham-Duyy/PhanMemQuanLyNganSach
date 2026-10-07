package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.Config.SessionKeys;
import com.oop.quanlingansach.Dto.ContributionReport;
import com.oop.quanlingansach.Dto.GroupFundReport;
import com.oop.quanlingansach.Dto.ReportOverview;
import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Service.BusinessException;
import com.oop.quanlingansach.Service.GroupService;
import com.oop.quanlingansach.Service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Các trang báo cáo của admin.
 */
@Controller
@RequestMapping("/admin/reports")
public class ReportController {

    private final ReportService reportService;
    private final GroupService groupService;

    public ReportController(ReportService reportService, GroupService groupService) {
        this.reportService = reportService;
        this.groupService = groupService;
    }

    @GetMapping
    public String overview(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser, Model model) {
        ReportOverview overview = reportService.getOverview(currentUser);
        model.addAttribute("overview", overview);
        return "admin/reports/index";
    }

    @GetMapping("/contributions")
    public String contributions(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                                @RequestParam(required = false) Long groupId, Model model,
                                RedirectAttributes redirectAttributes) {
        ContributionReport report;
        try {
            report = reportService.getContributionReport(groupId, currentUser);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/reports/contributions";
        }
        model.addAttribute("contributionStatistics", report.rows());
        model.addAttribute("totalContributionAmount", report.paidAmount());
        model.addAttribute("groups", groupService.findManaged(currentUser, null));
        model.addAttribute("selectedGroupId", groupId);
        return "admin/reports/contributions";
    }

    @GetMapping("/expenses")
    public String expenses(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                           @RequestParam(required = false) Long groupId, Model model) {
        model.addAttribute("groups", groupService.findManaged(currentUser, null));
        model.addAttribute("selectedGroupId", groupId);

        // id sai hoặc nhóm ngoài phạm vi quản lý thì coi như chưa chọn nhóm
        Group group = findManagedGroupOrNull(groupId, currentUser);
        if (group != null) {
            GroupFundReport report = reportService.getGroupFundReport(group);
            model.addAttribute("group", group);
            model.addAttribute("targetAmount", group.getTargetAmount());
            model.addAttribute("totalContributed", report.totalContributed());
            model.addAttribute("totalExpense", report.totalExpense());
            model.addAttribute("currentFund", report.currentFund());
            model.addAttribute("expenseHistory", report.expenseHistory());
            model.addAttribute("contributionHistory", report.contributionHistory());
        }
        return "admin/reports/expenses";
    }

    private Group findManagedGroupOrNull(Long groupId, User actor) {
        if (groupId == null) return null;
        try {
            return groupService.getManagedGroup(groupId, actor);
        } catch (BusinessException e) {
            return null;
        }
    }
}
