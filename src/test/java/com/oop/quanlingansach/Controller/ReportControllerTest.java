package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.TestWebConfig;
import org.springframework.context.annotation.Import;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import com.oop.quanlingansach.Dto.ContributionReport;
import com.oop.quanlingansach.Dto.GroupFundReport;
import com.oop.quanlingansach.Dto.ReportOverview;
import com.oop.quanlingansach.Service.BusinessException;
import com.oop.quanlingansach.Service.GroupService;
import com.oop.quanlingansach.Service.ReportService;
import com.oop.quanlingansach.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Các trang báo cáo của admin render đúng với dữ liệu từ ReportService.
 */
@Import(TestWebConfig.class)
@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private GroupService groupService;

    @Test
    void overview_ShouldRender() throws Exception {
        // 2 nhóm, quỹ ban đầu 500k; đã thu 300k, còn phải thu 200k, đã chi 100k -> số dư 700k
        ReportOverview overview = new ReportOverview(2, 5, 3, 3, 1, 2,
                new BigDecimal("500000"), new BigDecimal("300000"), new BigDecimal("200000"), new BigDecimal("100000"));
        when(reportService.getOverview()).thenReturn(overview);

        mockMvc.perform(get("/admin/reports").session(TestData.sessionOf(TestData.admin())))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("700,000")))  // số dư
                .andExpect(content().string(org.hamcrest.Matchers.containsString("500,000"))); // dự kiến thu
    }

    @Test
    void overviewRecord_ShouldDeriveTotals() {
        ReportOverview overview = new ReportOverview(0, 0, 0, 3, 1, 2,
                new BigDecimal("500000"), new BigDecimal("300000"), new BigDecimal("200000"), new BigDecimal("100000"));

        org.junit.jupiter.api.Assertions.assertEquals(6, overview.totalContributions());
        org.junit.jupiter.api.Assertions.assertEquals(0, overview.expectedIncome().compareTo(new BigDecimal("500000")));
        org.junit.jupiter.api.Assertions.assertEquals(0, overview.balance().compareTo(new BigDecimal("700000")));
    }

    @Test
    void contributions_ShouldRender() throws Exception {
        Map<String, Object> row = Map.of("userName", "Duy", "userEmail", "duy@example.com", "groupName", "Nhóm 10",
                "transactionDescription", "Quỹ", "transactionType", "INCOME", "amount", new BigDecimal("100000"),
                "status", "PAID", "createdDate", LocalDateTime.now());
        when(reportService.getContributionReport(null)).thenReturn(new ContributionReport(List.of(row), new BigDecimal("100000")));

        mockMvc.perform(get("/admin/reports/contributions").session(TestData.sessionOf(TestData.admin())))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports/contributions"));
    }

    @Test
    void expenses_WithGroup_ShouldRender() throws Exception {
        var group = TestData.group(10L, TestData.member(2L));
        group.setTargetAmount(new BigDecimal("1000000"));
        when(groupService.getById(10L)).thenReturn(group);
        when(reportService.getGroupFundReport(group)).thenReturn(new GroupFundReport(new BigDecimal("300000"),
                new BigDecimal("100000"), new BigDecimal("200000"), List.of(TestData.expense(6L, group, "100000")), List.of()));

        mockMvc.perform(get("/admin/reports/expenses").session(TestData.sessionOf(TestData.admin())).param("groupId", "10"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentFund", new BigDecimal("200000")));
    }

    @Test
    void expenses_UnknownGroup_ShouldRenderWithoutGroupData() throws Exception {
        when(groupService.getById(99L)).thenThrow(new BusinessException("Nhóm không tồn tại!"));

        mockMvc.perform(get("/admin/reports/expenses").session(TestData.sessionOf(TestData.admin())).param("groupId", "99"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("group"));
    }
}
