package com.oop.quanlingansach.Service;

import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Model.Transaction;
import com.oop.quanlingansach.Model.TransactionParticipant;
import com.oop.quanlingansach.Repository.GroupRepository;
import com.oop.quanlingansach.Repository.TransactionParticipantRepository;
import com.oop.quanlingansach.Repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionParticipantRepository participantRepository;

    public GroupServiceImpl(GroupRepository groupRepository,
                            TransactionRepository transactionRepository,
                            TransactionParticipantRepository participantRepository) {
        this.groupRepository = groupRepository;
        this.transactionRepository = transactionRepository;
        this.participantRepository = participantRepository;
    }

    @Override
    public List<Group> findAll() {
        return groupRepository.findAll();
    }

    @Override
    public List<Group> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return groupRepository.findAll();
        }
        return groupRepository.findByNameContainingIgnoreCase(keyword.trim());
    }

    @Override
    public Group getById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Nhóm không tồn tại!"));
    }

    @Override
    public Group lockForUpdate(Long id) {
        return groupRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException("Nhóm không tồn tại!"));
    }

    @Override
    public List<Group> findGroupsOfMember(Long userId) {
        return groupRepository.findByMembers_Id(userId);
    }

    @Override
    public Group create(Group form, Long adminId) {
        validateForm(form);
        // Tạo entity mới từ form để không nhận id/thành viên do client gửi lên
        Group group = new Group(form.getName().trim(), form.getDescription(), adminId);
        copyEditableFields(form, group);
        return groupRepository.save(group);
    }

    @Override
    public void update(Long id, Group form) {
        validateForm(form);
        Group group = getById(id);
        if (fundChanged(group.getFundAmount(), form.getFundAmount()) && transactionRepository.existsByGroup_Id(id)) {
            throw new BusinessException("Nhóm đã có giao dịch nên không thể sửa quỹ ban đầu!");
        }
        group.setName(form.getName().trim());
        group.setDescription(form.getDescription());
        copyEditableFields(form, group);
        groupRepository.save(group);
    }

    @Override
    public void delete(Long id) {
        if (transactionRepository.existsByGroup_Id(id)) {
            throw new BusinessException("Nhóm đã có giao dịch nên không thể xóa (sẽ mất lịch sử thu chi). "
                    + "Hãy sửa nhóm sang trạng thái \"Đã đóng\".");
        }
        groupRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void removeMember(Long groupId, Long userId) {
        Group group = getById(groupId);
        List<TransactionParticipant> openDues = findOpenDues(groupId, userId);
        if (openDues.stream().anyMatch(TransactionParticipant::isWaitingConfirmation)) {
            throw new BusinessException("Thành viên có khoản đã báo chuyển tiền đang chờ xác nhận. "
                    + "Hãy xác nhận hoặc từ chối trước khi xóa khỏi nhóm.");
        }
        // Người bị xóa không còn phải đóng các khoản đang thu của nhóm
        for (TransactionParticipant due : openDues) {
            Transaction transaction = due.getTransaction();
            transaction.removeParticipant(due);
            transactionRepository.save(transaction);
        }
        group.removeMember(userId);
        groupRepository.save(group);
    }

    @Override
    public void leave(Long groupId, Long userId) {
        Group group = getById(groupId);
        if (!group.hasMember(userId)) {
            throw new BusinessException("Bạn không phải thành viên nhóm này!");
        }
        int unpaid = findOpenDues(groupId, userId).size();
        if (unpaid > 0) {
            throw new BusinessException("Bạn còn " + unpaid + " khoản chưa đóng trong nhóm này. "
                    + "Hãy hoàn tất trước khi rời nhóm.");
        }
        group.removeMember(userId);
        groupRepository.save(group);
    }

    private static final java.util.Set<String> GROUP_TYPES = java.util.Set.of("FAMILY", "FRIENDS", "WORK", "TRAVEL", "OTHER");

    // Kiểm tra dữ liệu form ở server, không chỉ dựa vào ràng buộc HTML hay cột database
    private static void validateForm(Group form) {
        if (form.getName() == null || form.getName().isBlank()) {
            throw new BusinessException("Vui lòng nhập tên nhóm!");
        }
        if (form.getName().trim().length() > 100) {
            throw new BusinessException("Tên nhóm tối đa 100 ký tự!");
        }
        if (form.getDescription() != null && form.getDescription().length() > 500) {
            throw new BusinessException("Mô tả nhóm tối đa 500 ký tự!");
        }
        if (form.getType() != null && !form.getType().isBlank() && !GROUP_TYPES.contains(form.getType())) {
            throw new BusinessException("Loại nhóm không hợp lệ!");
        }
        if (isNegative(form.getFundAmount()) || isNegative(form.getTargetAmount())) {
            throw new BusinessException("Quỹ ban đầu và mục tiêu quỹ không được âm!");
        }
    }

    private static boolean isNegative(BigDecimal value) {
        return value != null && value.signum() < 0;
    }

    private static boolean fundChanged(BigDecimal current, BigDecimal requested) {
        BigDecimal from = current != null ? current : BigDecimal.ZERO;
        BigDecimal to = requested != null ? requested : BigDecimal.ZERO;
        return from.compareTo(to) != 0;
    }

    // Các khoản user còn phải đóng ở những khoản thu đang thu của nhóm
    private List<TransactionParticipant> findOpenDues(Long groupId, Long userId) {
        return participantRepository.findByUser_IdAndPaidFalseAndTransaction_Group_IdAndTransaction_Status(
                userId, groupId, Transaction.STATUS_ACTIVE);
    }

    @Override
    public long count() {
        return groupRepository.count();
    }

    @Override
    public BigDecimal getCurrentFund(Group group) {
        BigDecimal initial = group.getFundAmount() != null ? group.getFundAmount() : BigDecimal.ZERO;
        return initial
                .add(participantRepository.sumPaidAmountByGroup(group.getId()))
                .subtract(transactionRepository.sumExpenseByGroup(group.getId()));
    }

    @Override
    public Map<Long, BigDecimal> getCurrentFunds(List<Group> groups) {
        Map<Long, BigDecimal> funds = new HashMap<>();
        if (groups.isEmpty()) return funds;

        // 2 truy vấn GROUP BY cho cả danh sách, thay vì 2 truy vấn cho mỗi nhóm
        List<Long> ids = groups.stream().map(Group::getId).toList();
        Map<Long, BigDecimal> paid = toAmountMap(participantRepository.sumPaidAmountByGroups(ids));
        Map<Long, BigDecimal> spent = toAmountMap(transactionRepository.sumExpenseByGroups(ids));

        for (Group group : groups) {
            BigDecimal initial = group.getFundAmount() != null ? group.getFundAmount() : BigDecimal.ZERO;
            funds.put(group.getId(), initial
                    .add(paid.getOrDefault(group.getId(), BigDecimal.ZERO))
                    .subtract(spent.getOrDefault(group.getId(), BigDecimal.ZERO)));
        }
        return funds;
    }

    // Kết quả truy vấn [groupId, tổng tiền] -> Map
    private static Map<Long, BigDecimal> toAmountMap(List<Object[]> rows) {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (BigDecimal) row[1]);
        }
        return map;
    }

    private void copyEditableFields(Group form, Group group) {
        group.setType(form.getType());
        group.setFundAmount(form.getFundAmount() != null ? form.getFundAmount() : BigDecimal.ZERO);
        group.setTargetAmount(form.getTargetAmount() != null ? form.getTargetAmount() : BigDecimal.ZERO);
        group.setActive(form.isActive());
    }
}
