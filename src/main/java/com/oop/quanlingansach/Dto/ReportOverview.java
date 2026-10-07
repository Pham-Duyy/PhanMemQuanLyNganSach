package com.oop.quanlingansach.Dto;

import java.math.BigDecimal;

/**
 * Số liệu cho trang báo cáo tổng quan (/admin/reports).
 *
 * Dòng tiền:
 *  - collectedIncome   (Đã thu)        : tiền đóng góp thủ quỹ đã xác nhận
 *  - outstandingIncome (Còn phải thu)  : khoản chưa xác nhận của các khoản thu đang thu
 *  - expectedIncome    (Dự kiến thu)   : đã thu + còn phải thu
 *  - totalExpense      (Đã chi)        : khoản chi chưa hủy
 *  - balance           (Số dư)         : quỹ ban đầu của các nhóm + đã thu - đã chi
 */
public record ReportOverview(
        long totalGroups,
        long totalTransactions,
        long totalUsers,
        long paidContributions,
        long waitingContributions,
        long unpaidContributions,
        BigDecimal initialFunds,
        BigDecimal collectedIncome,
        BigDecimal outstandingIncome,
        BigDecimal totalExpense
) {
    public long totalContributions() {
        return paidContributions + waitingContributions + unpaidContributions;
    }

    public BigDecimal expectedIncome() {
        return collectedIncome.add(outstandingIncome);
    }

    public BigDecimal balance() {
        return initialFunds.add(collectedIncome).subtract(totalExpense);
    }
}
