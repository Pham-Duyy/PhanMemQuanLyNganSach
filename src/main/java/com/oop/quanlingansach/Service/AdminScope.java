package com.oop.quanlingansach.Service;

import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Model.User;

/**
 * Quy tắc phạm vi quản lý dùng chung cho các service phía quản trị:
 *  - Ban quản lý (SYSTEM_ADMIN) giám sát mọi nhóm
 *  - Thủ quỹ (ADMIN) chỉ quản lý các nhóm được giao (group.adminId)
 */
final class AdminScope {

    private AdminScope() {}

    /** Tham số adminId cho repository: null = mọi nhóm, id thủ quỹ = chỉ nhóm của người đó. */
    static Long adminIdOf(User actor) {
        return actor.isSystemAdmin() ? null : actor.getId();
    }

    static void requireManages(Group group, User actor) {
        if (!group.isManagedBy(actor)) {
            throw new BusinessException("Bạn không quản lý nhóm này!");
        }
    }
}
