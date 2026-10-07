package com.oop.quanlingansach.Dto;

import com.oop.quanlingansach.Model.Transaction;
import com.oop.quanlingansach.Model.TransactionParticipant;

import java.math.BigDecimal;
import java.util.List;

/**
 * Báo cáo thu chi của một nhóm (/admin/reports/expenses).
 */
public record GroupFundReport(
        BigDecimal totalContributed,
        BigDecimal totalExpense,
        BigDecimal currentFund,
        List<Transaction> expenseHistory,
        List<TransactionParticipant> contributionHistory
) {
}
