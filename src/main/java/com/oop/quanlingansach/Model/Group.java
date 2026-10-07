package com.oop.quanlingansach.Model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Nhóm quỹ do một admin tạo; thành viên tham gia qua lời mời.
 */
@Entity
@Table(name = "`groups`") // "groups" là từ khóa của MySQL nên cần backtick
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(length = 50)
    private String type; // FAMILY, FRIENDS, WORK, TRAVEL, OTHER

    @Column(name = "fund_amount")
    private BigDecimal fundAmount = BigDecimal.ZERO; // Quỹ ban đầu

    @Column(name = "target_amount")
    private BigDecimal targetAmount = BigDecimal.ZERO; // Mục tiêu quỹ

    // Cột cũ, không còn dùng (số dư được tính từ giao dịch). Giữ lại vì DB bắt buộc NOT NULL.
    @Column(name = "total_budget", nullable = false)
    private BigDecimal totalBudget = BigDecimal.ZERO;
    @Column(name = "total_income", nullable = false)
    private BigDecimal totalIncome = BigDecimal.ZERO;
    @Column(name = "total_expense", nullable = false)
    private BigDecimal totalExpense = BigDecimal.ZERO;

    // Xóa nhóm chỉ xóa liên kết thành viên, không xóa user
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> members = new ArrayList<>();

    // Xóa nhóm sẽ xóa luôn lời mời và giao dịch của nhóm
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupInvite> invites = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();

    public Group() {}

    public Group(String name, String description, Long adminId) {
        this.name = name;
        this.description = description;
        this.adminId = adminId;
    }

    public boolean hasMember(Long userId) {
        return members.stream().anyMatch(m -> m.getId().equals(userId));
    }

    public void addMember(User user) {
        if (!hasMember(user.getId())) {
            members.add(user);
        }
    }

    public void removeMember(Long userId) {
        members.removeIf(m -> m.getId().equals(userId));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getFundAmount() { return fundAmount; }
    public void setFundAmount(BigDecimal fundAmount) { this.fundAmount = fundAmount; }

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    // Chỉ đọc: thêm/bớt thành viên qua addMember/removeMember
    public List<User> getMembers() { return Collections.unmodifiableList(members); }
    public void setMembers(List<User> members) { this.members = new ArrayList<>(members); }

    public List<GroupInvite> getInvites() { return invites; }
    public void setInvites(List<GroupInvite> invites) { this.invites = invites; }

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Group other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
