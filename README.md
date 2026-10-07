# 💰 Fund Manager – Ứng Dụng Quản Lý Ngân Sách Nhóm  
### Group 09

---

## 1. Giới thiệu  
**Fund Manager** là ứng dụng web giúp người dùng quản lý thu chi nhóm một cách **minh bạch, tiện lợi và hiệu quả**.  
Ứng dụng cho phép **tạo nhóm, gửi lời mời, ghi nhận giao dịch, thống kê chi tiêu, và theo dõi quỹ nhóm theo thời gian thực.**

---

## 2. Tính năng chính  

### 👑 Dành cho Admin (thủ quỹ)  
- Tạo, sửa, đóng nhóm; mời và xóa thành viên.  
- Tạo khoản thu (chọn người phải đóng) và khoản chi (kiểm tra đủ quỹ); sửa, hủy giao dịch.  
- **Xác nhận hoặc từ chối** tiền thành viên báo đã chuyển; ghi nhận đóng tiền mặt.  
- Xem báo cáo tổng quan, đóng góp, thu chi theo nhóm.  

### 👥 Dành cho User (thành viên)  
- Đăng ký, đăng nhập, chấp nhận / từ chối lời mời vào nhóm, rời nhóm.  
- Xem khoản cần đóng, chuyển khoản qua QR rồi **báo đã chuyển tiền**.  
- Xem số dư quỹ nhóm, các khoản chi, lịch sử đóng góp cá nhân; sửa thông tin cá nhân, đổi mật khẩu.  

---

## 3. Kiến trúc hệ thống  

Ứng dụng được thiết kế theo mô hình **MVC (Model – View – Controller)** chia nhiều tầng. Mỗi tầng chỉ gọi tầng ngay bên dưới nó:

```
 Trình duyệt
     │  request (GET/POST)
     ▼
 Config/RoleInterceptor   ── chưa đăng nhập / sai vai trò → chuyển về /login hoặc /
     │
     ▼
 Controller   nhận dữ liệu form, gọi Service, chọn trang trả về (không chứa nghiệp vụ)
     │
     ▼
 Service      toàn bộ nghiệp vụ: kiểm tra dữ liệu, số dư quỹ, ai được mời, ai phải đóng tiền...
     │        lỗi nghiệp vụ → BusinessException (Controller hiển thị message cho người dùng)
     ▼
 Repository   truy vấn database (Spring Data JPA)
     │
     ▼
 Model        entity ánh xạ bảng MySQL
```

- **View** là các file Thymeleaf trong `templates/`, nhận dữ liệu từ Controller qua `Model`.
- **Dto** chứa dữ liệu form (`RegisterForm`, `TransactionForm`) và kết quả báo cáo. Form không bind thẳng vào entity, để người dùng không gửi thêm `id`/`role` để ghi đè dữ liệu.
- **Phân quyền 2 lớp:** (1) theo đường dẫn ở `Config/WebConfig.java`: `/admin/users/**` chỉ ban quản lý, `/admin/**` cho thủ quỹ và ban quản lý, `/user/**` chỉ thành viên; (2) theo từng nhóm ở tầng service (`AdminScope`): thủ quỹ chỉ thao tác được nhóm mình phụ trách, kể cả khi gõ thẳng URL.

---

## 4. Thành viên nhóm  

| Họ và tên | MSV | Vai trò |
|------------|------|----------|
| **Phạm Khương Duy** | 23010743 | Trưởng nhóm – Frontend, Backend, Database, Kiểm thử |
| **Dương Hồng Thái** | 23010326 | Kiểm thử, UI/UX, Báo cáo, Demo |

---

## 5. Công nghệ sử dụng  

| Công nghệ / Công cụ | Vai trò |
|----------------------|---------|
| **Spring Boot 3** | Xây dựng backend, xử lý logic nghiệp vụ |
| **Thymeleaf** | Template engine render HTML |
| **Bootstrap 5** | Thiết kế giao diện người dùng |
| **MySQL (Aiven Cloud)** | Lưu trữ dữ liệu |
| **Spring Data JPA (Hibernate)** | ORM mapping |
| **Git & GitHub** | Quản lý mã nguồn |
| **draw.io / Lucidchart** | Vẽ sơ đồ UML |

---

## 6. Cấu trúc thư mục dự án  

```bash
src/main/java/com/oop/quanlingansach/
│
├─ Main.java                     # Điểm khởi chạy Spring Boot
│
├─ Config/                       # Cấu hình web, phân quyền
│   ├─ WebConfig.java            # Khu vực nào cần vai trò nào (/admin/**, /user/**, ...)
│   ├─ RoleInterceptor.java      # Chặn request chưa đăng nhập / sai vai trò
│   └─ SessionKeys.java          # Tên thuộc tính lưu trong session
│
├─ Controller/                   # Nhận request -> gọi Service -> trả về trang Thymeleaf
│   ├─ AuthController.java              # Đăng nhập, đăng ký, đăng xuất, thông tin cá nhân
│   ├─ AdminController.java             # Dashboard admin
│   ├─ GroupAdminController.java        # Admin: quản lý nhóm, mời / xóa thành viên
│   ├─ AdminTransactionController.java  # Admin: tạo / sửa / xóa giao dịch thu chi
│   ├─ ReportController.java            # Admin: báo cáo
│   ├─ UserAdminController.java         # Ban quản lý: bổ nhiệm thủ quỹ, khóa tài khoản
│   ├─ UserController.java              # Dashboard user
│   ├─ GroupUserController.java         # User: nhóm của tôi, lời mời, rời nhóm
│   ├─ UserTransactionController.java   # User: khoản cần đóng, xác nhận đã chuyển tiền
│   ├─ PersonalFinanceController.java   # User: thu chi cá nhân
│   └─ GlobalExceptionHandler.java      # Lỗi nghiệp vụ chưa được xử lý -> về trang chủ kèm thông báo
│
├─ Service/                      # Nghiệp vụ (interface + Impl)
│   ├─ UserService(Impl)         # Tài khoản, mật khẩu BCrypt
│   ├─ GroupService(Impl)        # Nhóm, thành viên, số dư quỹ
│   ├─ GroupInviteService(Impl)  # Lời mời vào nhóm
│   ├─ TransactionService(Impl)  # Giao dịch thu/chi, người phải đóng, xác nhận đóng tiền
│   ├─ ReportService(Impl)       # Số liệu báo cáo
│   ├─ AdminScope.java           # Quy tắc phạm vi: thủ quỹ chỉ quản lý nhóm của mình
│   └─ BusinessException.java    # Lỗi nghiệp vụ, message hiển thị cho người dùng
│
├─ Repository/                   # Truy vấn database (Spring Data JPA)
│   ├─ UserRepository.java
│   ├─ GroupRepository.java
│   ├─ GroupInviteRepository.java
│   ├─ TransactionRepository.java
│   └─ TransactionParticipantRepository.java
│
├─ Model/                        # Entity JPA = bảng trong database
│   ├─ User.java                 # users
│   ├─ Group.java                # groups (+ bảng nối group_members)
│   ├─ GroupInvite.java          # group_invites
│   ├─ Transaction.java          # transactions
│   └─ TransactionParticipant.java  # transaction_participants: ai phải đóng, đã đóng chưa
│
└─ Dto/                          # Dữ liệu form và kết quả báo cáo (không phải bảng DB)
    ├─ RegisterForm.java
    ├─ TransactionForm.java
    ├─ ReportOverview.java
    ├─ ContributionReport.java
    └─ GroupFundReport.java

src/main/resources/
├─ application.properties        # Cấu hình; mật khẩu DB lấy từ biến môi trường DB_PASSWORD
├─ static/css, static/js         # Giao diện dùng chung
└─ templates/
    ├─ fragments/  layout (menu, thanh trên), bank (tài khoản nhận tiền của nhóm)
    ├─ auth/    login, register, profile
    ├─ admin/   dashboard, groups/, finance/, reports/, users/ (ban quản lý)
    └─ user/    dashboard, groups/, finance/, personal-finance/

src/test/java/com/oop/quanlingansach/
├─ Service/      # Test nghiệp vụ (Mockito, không cần Spring)
├─ Controller/   # Test phân quyền, điều hướng, render giao diện (@WebMvcTest)
└─ TestData.java # Dữ liệu mẫu dùng chung
```

---

## 7. Quy trình nghiệp vụ

**Mô hình: quỹ do một tổ chức quản lý**, 3 vai trò:

| Vai trò | Ai | Được làm |
|---|---|---|
| **Ban quản lý** (SYSTEM_ADMIN) | Lãnh đạo tổ chức | Bổ nhiệm / thu hồi thủ quỹ, khóa tài khoản, bàn giao nhóm giữa các thủ quỹ, giám sát mọi nhóm |
| **Thủ quỹ** (ADMIN) | Người được bổ nhiệm | Tạo nhóm và chỉ quản lý nhóm của mình: thành viên, thu chi, xác nhận tiền, báo cáo |
| **Thành viên** (USER) | Người đóng quỹ | Tham gia nhóm qua lời mời, chuyển khoản và báo đã chuyển |

Tài khoản đăng ký trên web luôn là Thành viên. Thủ quỹ do Ban quản lý bổ nhiệm trong trang **Quản lý người dùng**; Ban quản lý đầu tiên được cấp trực tiếp trong database.

Mỗi nhóm có **tài khoản nhận tiền riêng** (của thủ quỹ nhóm). Thành viên chuyển khoản với nội dung chuẩn
`QUY<mã nhóm> THU<mã khoản thu> <tên đăng nhập>` và có thể khai mã giao dịch; thủ quỹ đối chiếu sao kê rồi xác nhận,
hệ thống ghi lại người xác nhận.

**Nguyên tắc chung:** số dư quỹ chỉ tính **tiền thủ quỹ đã xác nhận**:

```
Số dư quỹ = Quỹ ban đầu + Tổng tiền đóng đã xác nhận − Tổng chi (không tính khoản chi đã hủy)
```

### 7.1. Nhóm và thành viên

```
Admin tạo nhóm ──> Admin mời USER ──> USER chấp nhận ──> trở thành thành viên
                                   └─> USER từ chối  ──> admin có thể mời lại
```

| Quy tắc | Lý do |
|---|---|
| Chỉ mời được tài khoản USER, không mời trùng khi đang có lời mời chờ | Tránh mời nhầm admin / spam lời mời |
| Nhóm **Đã đóng**: không tạo giao dịch, không mời, không nhận thêm thành viên | Nhóm đã kết thúc hoạt động |
| Nhóm đã có giao dịch: **không xóa được**, chỉ chuyển sang "Đã đóng" | Giữ sổ sách thu chi |
| Quỹ ban đầu chỉ sửa được khi nhóm **chưa có giao dịch** | Không cho đổi số dư sau khi đã thu chi |
| Thành viên **còn khoản chưa đóng** thì không được rời nhóm | Không "trốn" khoản phải đóng |
| Admin xóa thành viên: các khoản chưa đóng của người đó được bỏ. Nếu họ đã báo chuyển tiền thì admin phải xác nhận / từ chối trước | Không mất dấu tiền đã chuyển |

### 7.2. Khoản thu (thành viên đóng quỹ)

```
Admin tạo khoản thu, chọn người phải đóng (bỏ trống = cả nhóm)
   │
   ▼
Mỗi người: [Chưa đóng] ──(user bấm "Tôi đã chuyển tiền")──> [Chờ xác nhận]
                ▲                                               │
                └──────(thủ quỹ "Từ chối": không thấy tiền)─────┤
                                                                ▼
           (thủ quỹ "Xác nhận", kể cả đóng tiền mặt)──────> [Đã đóng]  → cộng vào quỹ
   │
   ▼
Khi tất cả đã đóng → khoản thu tự chuyển "Đã thu đủ" (COMPLETED)
```

- Khoản thu đang thu có thể **sửa** (tiêu đề, số tiền, hạn); người chưa đóng sẽ đóng theo số tiền mới, người đã đóng giữ nguyên.
- **Hủy** khoản thu: ngừng thu phần còn lại, tiền đã đóng vẫn nằm trong quỹ.
- **Xóa** chỉ áp dụng cho khoản thu chưa có ai đóng (tạo nhầm).

### 7.3. Khoản chi

```
Admin tạo khoản chi ──(quỹ đủ tiền?)──> có: [Đã chi], trừ vào quỹ
                                    └─> không: báo lỗi kèm số dư hiện có
[Đã chi] ──(admin Hủy)──> [Đã hủy], số tiền được hoàn lại quỹ
```

- Khoản chi **không xóa được**, chỉ hủy, để giữ lịch sử.
- Sửa số tiền khoản chi: quỹ (sau khi hoàn khoản cũ) phải đủ cho số tiền mới.
- Thành viên trong nhóm thấy được mọi khoản chi chưa hủy (minh bạch).

### 7.4. Báo cáo

- **Tổng quan**: số nhóm, giao dịch, thành viên, số khoản đã đóng / chưa đóng.
- **Đóng góp**: từng khoản phải đóng của từng thành viên, lọc theo nhóm.
- **Thu chi theo nhóm**: quỹ hiện tại, mục tiêu, lịch sử chi, lịch sử đóng góp đã xác nhận.

 ## 8.Sơ đồ UML & Kiến trúc hệ thống
### Use Case Diagram
<img src="https://github.com/user-attachments/assets/7bd82403-e56c-480e-bace-b03883649d63" width="600" />

### Class Diagram
<img src="https://github.com/user-attachments/assets/0d4a267b-a8fd-44d3-b9b6-ea3f944ae4ec" width="800" />

## Sequence Diagram
### CRUD cho User
<img src="https://github.com/user-attachments/assets/ec888897-a68d-434a-9212-b47003e8958d" width="700" />

### CRUD cho GroupAdmin
<img src="https://github.com/user-attachments/assets/8e4d5a96-87f8-4ec8-9306-7072968835cf" width="800" />

### CRUD cho Transaction
<img src="https://github.com/user-attachments/assets/c116881a-5e95-4cf2-8572-46e3ba097461" width="1000" />


 ## 9. Cách chạy dự án

### Bước 1: Clone repository về máy
```bash
git clone https://github.com/PhamDuyVQ/VP_NM_25_26_KhuongDuy_HongThai.git
```
---

### Bước 2: Mở dự án bằng IDE (IntelliJ / Eclipse)
### Bước 3: Đặt mật khẩu database
Mật khẩu **không** lưu trong `application.properties`. Xin mật khẩu từ trưởng nhóm (gửi riêng), rồi tạo file `local.properties` ở **thư mục gốc dự án** (cùng chỗ với `pom.xml`):
```properties
DB_PASSWORD=mật-khẩu-database
```
File này đã nằm trong `.gitignore` nên không bị commit. (Cách khác: đặt biến môi trường `setx DB_PASSWORD "..."` rồi tắt hẳn và mở lại IDE.)
Nếu dùng database khác, thêm `DB_URL` và `DB_USERNAME` vào cùng file.

Tài khoản đăng ký trên web luôn là Thành viên. Thủ quỹ được Ban quản lý bổ nhiệm trên web (menu **Quản lý người dùng**).
Với database mới tinh, cần cấp **Ban quản lý đầu tiên** bằng SQL (chỉ làm một lần):
```sql
UPDATE users SET role = 'SYSTEM_ADMIN' WHERE username = 'ten-dang-nhap';
```
### Bước 4: Chạy dự án:
```bash
mvn spring-boot:run
```
---
### Bước 5 : Truy cập trình duyệt:
```bash
 http://localhost:8080
```

## 10. Hạn chế & Định hướng phát triển
### Hạn chế
Giao diện chưa hỗ trợ Dark Mode và đa ngôn ngữ.
Chưa có JWT Authentication hoặc 2FA.
Phân quyền còn đơn giản (Admin, User).
### Định hướng
Phát triển ứng dụng Mobile (Flutter/React Native).
Ứng dụng AI để dự báo chi tiêu và tối ưu ngân sách.
Cải tiến UI/UX, bổ sung Dashboard thông minh.
