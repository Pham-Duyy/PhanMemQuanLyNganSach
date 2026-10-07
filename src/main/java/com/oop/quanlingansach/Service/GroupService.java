package com.oop.quanlingansach.Service;

import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Model.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Nhóm quỹ: tạo/sửa/xóa, thành viên, bàn giao thủ quỹ, số dư quỹ.
 * Các hàm có tham số actor kiểm tra người thao tác có quản lý nhóm đó không.
 */
public interface GroupService {

    /** Nhóm trong phạm vi quản lý của actor, lọc theo tên (keyword trống = tất cả). */
    List<Group> findManaged(User actor, String keyword);

    /** Lấy nhóm, không kiểm tra quyền (dùng cho phía thành viên và nội bộ). */
    Group getById(Long id);

    /** Lấy nhóm, chỉ khi actor là thủ quỹ của nhóm hoặc ban quản lý. */
    Group getManagedGroup(Long id, User actor);

    /**
     * Lấy nhóm và khóa dòng dữ liệu (SELECT ... FOR UPDATE) tới hết transaction hiện tại.
     * Dùng trước mọi thao tác kiểm tra số dư rồi trừ tiền, để các yêu cầu đồng thời phải xếp hàng.
     */
    Group lockForUpdate(Long id);

    /** Các nhóm mà user là thành viên. */
    List<Group> findGroupsOfMember(Long userId);

    /** Tạo nhóm; actor trở thành thủ quỹ của nhóm. */
    Group create(Group form, User actor);

    void update(Long id, Group form, User actor);

    void delete(Long id, User actor);

    void removeMember(Long groupId, Long userId, User actor);

    /** Ban quản lý bàn giao nhóm cho một thủ quỹ khác. */
    void transferTreasurer(Long groupId, Long newTreasurerId, User actor);

    /** User tự rời nhóm. */
    void leave(Long groupId, Long userId);

    long countManaged(User actor);

    /** Số thành viên (không trùng) của các nhóm trong phạm vi quản lý. */
    long countMembersOfManaged(User actor);

    /** Số dư quỹ = quỹ ban đầu + tổng đóng góp đã xác nhận - tổng chi. */
    BigDecimal getCurrentFund(Group group);

    /** Số dư quỹ của nhiều nhóm, theo id nhóm. */
    Map<Long, BigDecimal> getCurrentFunds(List<Group> groups);
}
