package com.oop.quanlingansach.Service;

import com.oop.quanlingansach.Dto.RegisterForm;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserServiceImpl implements UserService {

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]?\\$\\d{2}\\$[./0-9A-Za-z]{53}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,50}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> authenticate(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username)
                .filter(u -> passwordMatches(u, password));
        // Chỉ báo "bị khóa" khi đã đúng mật khẩu, để không lộ trạng thái tài khoản cho người đoán mò
        if (user.isPresent() && !user.get().isActive()) {
            throw new BusinessException("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }
        return user;
    }

    @Override
    public User register(RegisterForm form) {
        if (isBlank(form.getUsername()) || isBlank(form.getEmail()) || isBlank(form.getFullName())) {
            throw new BusinessException("Vui lòng nhập đầy đủ thông tin!");
        }
        if (!USERNAME_PATTERN.matcher(form.getUsername().trim()).matches()) {
            throw new BusinessException("Tên đăng nhập dài 3–50 ký tự, chỉ gồm chữ không dấu, số và . _ -");
        }
        validateNameAndEmail(form.getFullName(), form.getEmail());
        validateNewPassword(form.getPassword(), form.getConfirmPassword());
        if (userRepository.existsByUsername(form.getUsername().trim())) {
            throw new BusinessException("Tên đăng nhập đã tồn tại!");
        }
        if (userRepository.existsByEmail(form.getEmail().trim())) {
            throw new BusinessException("Email đã được sử dụng!");
        }

        // Đăng ký công khai luôn là USER; tài khoản ADMIN chỉ được cấp trực tiếp trong DB
        User user = new User();
        user.setUsername(form.getUsername().trim());
        user.setEmail(form.getEmail().trim());
        user.setFullName(form.getFullName().trim());
        user.setPassword(PASSWORD_ENCODER.encode(form.getPassword()));
        user.setRole(User.Role.USER);
        user.setCreatedDate(LocalDateTime.now());
        user.setActive(true);
        return userRepository.save(user);
    }

    @Override
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Tài khoản không tồn tại!"));
    }

    @Override
    public User updateProfile(Long id, String fullName, String email) {
        if (isBlank(fullName) || isBlank(email)) {
            throw new BusinessException("Vui lòng nhập họ tên và email!");
        }
        validateNameAndEmail(fullName, email);
        String newEmail = email.trim();
        User user = getById(id);
        if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new BusinessException("Email đã được sử dụng!");
        }
        user.setFullName(fullName.trim());
        user.setEmail(newEmail);
        return userRepository.save(user);
    }

    @Override
    public void changePassword(Long id, String oldPassword, String newPassword, String confirmPassword) {
        if (isBlank(oldPassword)) {
            throw new BusinessException("Vui lòng nhập mật khẩu cũ!");
        }
        validateNewPassword(newPassword, confirmPassword);
        if (oldPassword.equals(newPassword)) {
            throw new BusinessException("Mật khẩu mới phải khác mật khẩu cũ!");
        }
        User user = getById(id);
        if (!passwordMatches(user, oldPassword)) {
            throw new BusinessException("Mật khẩu cũ không đúng!");
        }
        user.setPassword(PASSWORD_ENCODER.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public List<User> findNormalUsers() {
        return userRepository.findByRole(User.Role.USER);
    }

    @Override
    public long countNormalUsers() {
        return userRepository.countByRole(User.Role.USER);
    }

    // So khớp mật khẩu; tài khoản cũ còn lưu mật khẩu thô sẽ được băm lại ngay khi đăng nhập đúng
    private boolean passwordMatches(User user, String rawPassword) {
        String stored = user.getPassword();
        if (stored == null || rawPassword == null) return false;
        if (BCRYPT_PATTERN.matcher(stored).matches()) {
            return PASSWORD_ENCODER.matches(rawPassword, stored);
        }
        if (stored.equals(rawPassword)) {
            user.setPassword(PASSWORD_ENCODER.encode(rawPassword));
            userRepository.save(user);
            return true;
        }
        return false;
    }

    // Giới hạn độ dài theo cột database: full_name 255, email 100
    private static void validateNameAndEmail(String fullName, String email) {
        if (fullName.trim().length() > 255) {
            throw new BusinessException("Họ tên tối đa 255 ký tự!");
        }
        String trimmedEmail = email.trim();
        if (trimmedEmail.length() > 100 || !EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            throw new BusinessException("Email không hợp lệ!");
        }
    }

    private void validateNewPassword(String password, String confirmPassword) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException("Mật khẩu phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự!");
        }
        if (!password.equals(confirmPassword)) {
            throw new BusinessException("Mật khẩu xác nhận không khớp!");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
