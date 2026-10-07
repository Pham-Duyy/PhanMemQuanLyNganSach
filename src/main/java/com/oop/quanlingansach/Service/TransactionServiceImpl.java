package com.oop.quanlingansach.Service;

import com.oop.quanlingansach.Dto.TransactionForm;
import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Model.Transaction;
import com.oop.quanlingansach.Model.TransactionParticipant;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Repository.TransactionParticipantRepository;
import com.oop.quanlingansach.Repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionParticipantRepository participantRepository;
    private final GroupService groupService;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  TransactionParticipantRepository participantRepository,
                                  GroupService groupService) {
        this.transactionRepository = transactionRepository;
        this.participantRepository = participantRepository;
        this.groupService = groupService;
    }

    @Override
    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    @Override
    public Transaction getById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Giao dịch không tồn tại!"));
    }

    @Override
    @Transactional
    public Transaction create(TransactionForm form, User creator) {
        if (form.getGroupId() == null) {
            throw new BusinessException("Vui lòng chọn nhóm!");
        }
        // Khóa dòng của nhóm tới hết transaction: hai yêu cầu chi cùng lúc phải chờ nhau,
        // không thể cùng đọc một số dư cũ rồi cùng vượt qua bước kiểm tra quỹ
        Group group = groupService.lockForUpdate(form.getGroupId());
        if (!group.isActive()) {
            throw new BusinessException("Nhóm đã đóng, không thể tạo giao dịch mới!");
        }
        String type = normalizeType(form.getType());
        validateCommonFields(form);

        List<User> payers = List.of();
        if (Transaction.TYPE_INCOME.equals(type)) {
            if (group.getMembers().isEmpty()) {
                throw new BusinessException("Nhóm chưa có thành viên nào để thu tiền!");
            }
            payers = selectPayers(group, form.getTargetUserId());
            if (payers.isEmpty()) {
                throw new BusinessException("Người được chọn không phải thành viên của nhóm!");
            }
        } else {
            ensureFundCovers(group, form.getAmount(), BigDecimal.ZERO);
        }

        Transaction transaction = new Transaction(form.getTitle().trim(), form.getDescription(), form.getAmount(),
                type, group, creator, parseDueDate(form.getDueDate()));
        transactionRepository.save(transaction);

        for (User payer : payers) {
            participantRepository.save(new TransactionParticipant(transaction, payer, form.getAmount()));
        }
        return transaction;
    }

    @Override
    @Transactional
    public void update(Long id, TransactionForm form) {
        Transaction transaction = getById(id);
        if (transaction.isCancelled()) {
            throw new BusinessException("Giao dịch đã hủy, không thể sửa!");
        }
        if (transaction.isIncome() && !transaction.isActive()) {
            throw new BusinessException("Khoản thu đã thu đủ, không thể sửa!");
        }
        validateCommonFields(form);
        boolean amountChanged = form.getAmount().compareTo(transaction.getAmount()) != 0;
        if (amountChanged && transaction.countWaitingConfirmations() > 0) {
            throw new BusinessException("Còn " + transaction.countWaitingConfirmations()
                    + " khoản đã báo chuyển đang chờ xác nhận. Hãy xác nhận/từ chối trước khi đổi số tiền.");
        }
        if (transaction.isExpense()) {
            // Khóa nhóm rồi hoàn khoản chi cũ vào quỹ trước khi kiểm tra số tiền mới
            Group group = groupService.lockForUpdate(transaction.getGroup().getId());
            ensureFundCovers(group, form.getAmount(), transaction.getAmount());
        }

        transaction.setTitle(form.getTitle().trim());
        transaction.setDescription(form.getDescription());
        transaction.setAmount(form.getAmount());
        transaction.setDueDate(parseDueDate(form.getDueDate()));
        transactionRepository.save(transaction);

        // Người chưa đóng phải đóng theo số tiền mới; người đã đóng giữ nguyên số đã đóng
        for (TransactionParticipant participant : participantRepository.findByTransaction_Id(id)) {
            if (!participant.isPaid()) {
                participant.setAmount(form.getAmount());
                participantRepository.save(participant);
            }
        }
    }

    @Override
    public void cancel(Long id) {
        Transaction transaction = getById(id);
        if (transaction.isCancelled()) {
            throw new BusinessException("Giao dịch đã được hủy trước đó!");
        }
        if (transaction.isIncome() && !transaction.isActive()) {
            throw new BusinessException("Khoản thu đã thu đủ, không thể hủy!");
        }
        ensureNoWaitingConfirmations(transaction, "hủy");
        transaction.cancel();
        transactionRepository.save(transaction);
    }

    @Override
    public void delete(Long id) {
        Transaction transaction = getById(id);
        if (transaction.isExpense() || transaction.hasConfirmedPayments()) {
            throw new BusinessException("Giao dịch đã phát sinh tiền nên không thể xóa. Hãy dùng \"Hủy\" để giữ lại lịch sử.");
        }
        ensureNoWaitingConfirmations(transaction, "xóa");
        transactionRepository.delete(transaction);
    }

    // Tiền đã báo chuyển phải được thủ quỹ xử lý trước, nếu không sẽ mất dấu khoản tiền đó
    private void ensureNoWaitingConfirmations(Transaction transaction, String action) {
        long waiting = transaction.countWaitingConfirmations();
        if (waiting > 0) {
            throw new BusinessException("Còn " + waiting + " khoản đã báo chuyển đang chờ xác nhận. "
                    + "Hãy xác nhận/từ chối trước khi " + action + " giao dịch.");
        }
    }

    // ==================== QUY TRÌNH ĐÓNG TIỀN ====================

    @Override
    public void reportPayment(Long transactionId, Long userId) {
        TransactionParticipant participant = getOpenParticipant(transactionId, userId);
        if (participant.isWaitingConfirmation()) {
            throw new BusinessException("Bạn đã báo chuyển tiền, đang chờ thủ quỹ xác nhận!");
        }
        participant.reportPaid();
        participantRepository.save(participant);
    }

    @Override
    @Transactional
    public void confirmPayment(Long transactionId, Long userId) {
        TransactionParticipant participant = getOpenParticipant(transactionId, userId);
        participant.confirmPaid();
        participantRepository.save(participant);

        Transaction transaction = participant.getTransaction();
        transaction.completeIfFullyPaid();
        transactionRepository.save(transaction);
    }

    @Override
    public void rejectPayment(Long transactionId, Long userId) {
        TransactionParticipant participant = getOpenParticipant(transactionId, userId);
        if (!participant.isWaitingConfirmation()) {
            throw new BusinessException("Thành viên này chưa báo chuyển tiền!");
        }
        participant.rejectReport();
        participantRepository.save(participant);
    }

    // Khoản đóng góp còn mở: thuộc khoản thu đang thu và chưa được xác nhận
    private TransactionParticipant getOpenParticipant(Long transactionId, Long userId) {
        Transaction transaction = getById(transactionId);
        if (!transaction.isIncome() || !transaction.isActive()) {
            throw new BusinessException("Khoản thu này không còn nhận đóng tiền!");
        }
        TransactionParticipant participant = participantRepository.findByTransaction_IdAndUser_Id(transactionId, userId)
                .orElseThrow(() -> new BusinessException("Không có trong danh sách cần đóng của khoản thu này!"));
        if (participant.isPaid()) {
            throw new BusinessException("Khoản này đã được xác nhận đóng tiền rồi!");
        }
        return participant;
    }

    // ==================== TRUY VẤN ====================

    @Override
    public List<Transaction> findPendingIncomeForUser(Long userId) {
        return transactionRepository.findPendingIncomeForUser(userId);
    }

    @Override
    public Set<Long> findWaitingConfirmationIds(Long userId) {
        return participantRepository.findByUser_IdAndPaidFalseAndReportedDateIsNotNull(userId).stream()
                .map(participant -> participant.getTransaction().getId())
                .collect(Collectors.toSet());
    }

    @Override
    public List<Transaction> findExpensesForMember(Long userId) {
        return transactionRepository.findExpensesForMember(userId);
    }

    @Override
    public List<TransactionParticipant> findContributionsOfUser(Long userId) {
        return participantRepository.findByUser_Id(userId);
    }

    @Override
    public List<TransactionParticipant> findPaidContributionsOfUser(Long userId) {
        return participantRepository.findByUser_IdAndPaidTrue(userId);
    }

    @Override
    public long countAll() {
        return transactionRepository.count();
    }

    @Override
    public long countByType(String type) {
        return transactionRepository.countByType(type);
    }

    // ==================== HÀM PHỤ ====================

    private String normalizeType(String type) {
        if (Transaction.TYPE_INCOME.equalsIgnoreCase(type)) return Transaction.TYPE_INCOME;
        if (Transaction.TYPE_EXPENSE.equalsIgnoreCase(type)) return Transaction.TYPE_EXPENSE;
        throw new BusinessException("Loại giao dịch không hợp lệ!");
    }

    private void validateCommonFields(TransactionForm form) {
        if (form.getAmount() == null || form.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền phải lớn hơn 0!");
        }
        if (form.getTitle() == null || form.getTitle().isBlank()) {
            throw new BusinessException("Vui lòng nhập tiêu đề giao dịch!");
        }
        parseDueDate(form.getDueDate()); // báo lỗi sớm nếu ngày sai định dạng
    }

    private LocalDateTime parseDueDate(String dueDate) {
        if (dueDate == null || dueDate.isBlank()) return null;
        try {
            return LocalDateTime.parse(dueDate);
        } catch (DateTimeParseException e) {
            throw new BusinessException("Hạn thực hiện không hợp lệ!");
        }
    }

    // Quỹ (cộng phần được hoàn lại, nếu có) phải đủ cho khoản chi
    private void ensureFundCovers(Group group, BigDecimal amount, BigDecimal refunded) {
        BigDecimal available = groupService.getCurrentFund(group).add(refunded);
        if (amount.compareTo(available) > 0) {
            throw new BusinessException("Quỹ nhóm không đủ để chi! Số dư có thể dùng: "
                    + String.format("%,.0f", available) + " VNĐ.");
        }
    }

    // Không chọn ai hoặc chọn "ALL" -> tất cả thành viên; chỉ nhận người đang là thành viên nhóm
    private List<User> selectPayers(Group group, List<String> targetUserIds) {
        List<User> members = group.getMembers();
        if (targetUserIds == null || targetUserIds.isEmpty() || targetUserIds.contains("ALL")) {
            return members;
        }
        return members.stream()
                .filter(member -> targetUserIds.contains(String.valueOf(member.getId())))
                .toList();
    }
}
