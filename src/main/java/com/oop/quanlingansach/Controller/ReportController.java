package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.Dto.ContributionReport;
import com.oop.quanlingansach.Dto.GroupFundReport;
import com.oop.quanlingansach.Dto.ReportOverview;
import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Service.BusinessException;
import com.oop.quanlingansach.Service.GroupService;
import com.oop.quanlingansach.Service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    public String overview(Model model) {
        ReportOverview overview = reportService.getOverview();
        model.addAttribute("overview", overview);
        return "admin/reports/index";
    }

    @GetMapping("/contributions")
    public String contributions(@RequestParam(required = false) Long groupId, Model model) {
        ContributionReport report = reportService.getContributionReport(groupId);
        model.addAttribute("contributionStatistics", report.rows());
        model.addAttribute("totalContributionAmount", report.paidAmount());
        model.addAttribute("groups", groupService.findAll());
        model.addAttribute("selectedGroupId", groupId);
        return "admin/reports/contributions";
    }

    @GetMapping("/expenses")
    public String expenses(@RequestParam(required = false) Long groupId, Model model) {
        model.addAttribute("groups", groupService.findAll());
        model.addAttribute("selectedGroupId", groupId);

        Group group = findGroupOrNull(groupId); // id sai thì coi như chưa chọn nhóm
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

    private Group findGroupOrNull(Long groupId) {
        if (groupId == null) return null;
        try {
            return groupService.getById(groupId);
        } catch (BusinessException e) {
            return null;
        }
    }
}
