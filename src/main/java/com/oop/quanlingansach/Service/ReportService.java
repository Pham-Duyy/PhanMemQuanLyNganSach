package com.oop.quanlingansach.Service;

import com.oop.quanlingansach.Dto.ContributionReport;
import com.oop.quanlingansach.Dto.GroupFundReport;
import com.oop.quanlingansach.Dto.ReportOverview;
import com.oop.quanlingansach.Model.Group;

/**
 * Số liệu thống kê cho các trang báo cáo của admin.
 */
public interface ReportService {

    ReportOverview getOverview();

    /** Đóng góp của mọi nhóm, hoặc của một nhóm nếu groupId khác null. */
    ContributionReport getContributionReport(Long groupId);

    GroupFundReport getGroupFundReport(Group group);
}
