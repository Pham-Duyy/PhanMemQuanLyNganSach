package com.oop.quanlingansach.Service;

import com.oop.quanlingansach.Model.Group;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Nhóm quỹ: tạo/sửa/xóa, thành viên, số dư quỹ.
 */
public interface GroupService {

    List<Group> findAll();

    /** Tìm theo tên; từ khóa trống thì trả về tất cả. */
    List<Group> search(String keyword);

    Group getById(Long id);

    /**
     * Lấy nhóm và khóa dòng dữ liệu (SELECT ... FOR UPDATE) tới hết transaction hiện tại.
     * Dùng trước mọi thao tác kiểm tra số dư rồi trừ tiền, để các yêu cầu đồng thời phải xếp hàng.
     */
    Group lockForUpdate(Long id);

    /** Các nhóm mà user là thành viên. */
    List<Group> findGroupsOfMember(Long userId);

    Group create(Group form, Long adminId);

    void update(Long id, Group form);

    void delete(Long id);

    void removeMember(Long groupId, Long userId);

    /** User tự rời nhóm. */
    void leave(Long groupId, Long userId);

    long count();

    /** Số dư quỹ = quỹ ban đầu + tổng đóng góp đã xác nhận - tổng chi. */
    BigDecimal getCurrentFund(Group group);

    /** Số dư quỹ của nhiều nhóm, theo id nhóm. */
    Map<Long, BigDecimal> getCurrentFunds(List<Group> groups);
}
