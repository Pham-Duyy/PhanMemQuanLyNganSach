package com.oop.quanlingansach.Dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Báo cáo đóng góp (/admin/reports/contributions).
 * Mỗi dòng là một Map vì template đọc theo dạng stat['userName'], stat['amount']...
 */
public record ContributionReport(List<Map<String, Object>> rows, BigDecimal paidAmount) {
}
