package com.oop.quanlingansach.Service;

import com.oop.quanlingansach.Dto.ContributionReport;
import com.oop.quanlingansach.Dto.GroupFundReport;
import com.oop.quanlingansach.Dto.ReportOverview;
import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Model.User;

/**
 * Số liệu thống kê cho các trang báo cáo, trong phạm vi quản lý của người xem
 * (ban quản lý: mọi nhóm; thủ quỹ: các nhóm của mình).
 */
public interface ReportService {

    ReportOverview getOverview(User actor);

    /** Đóng góp trong phạm vi quản lý, hoặc của một nhóm (phải thuộc phạm vi) nếu groupId khác null. */
    ContributionReport getContributionReport(Long groupId, User actor);

    /** Báo cáo thu chi của một nhóm; người gọi phải kiểm tra quyền trước (GroupService.getManagedGroup). */
    GroupFundReport getGroupFundReport(Group group);
}
