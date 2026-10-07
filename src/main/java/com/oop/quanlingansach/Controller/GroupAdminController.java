package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.Config.SessionKeys;
import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Service.BusinessException;
import com.oop.quanlingansach.Service.GroupInviteService;
import com.oop.quanlingansach.Service.GroupService;
import com.oop.quanlingansach.Service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Admin quản lý nhóm: danh sách, tạo/sửa/xóa, mời và xóa thành viên.
 */
@Controller
@RequestMapping("/admin/groups")
public class GroupAdminController {

    private static final String LIST_PAGE = "redirect:/admin/groups";

    private final GroupService groupService;
    private final GroupInviteService inviteService;
    private final UserService userService;

    public GroupAdminController(GroupService groupService, GroupInviteService inviteService, UserService userService) {
        this.groupService = groupService;
        this.inviteService = inviteService;
        this.userService = userService;
    }

    // Danh sách nhóm + modal tạo/sửa nhóm + modal mời thành viên
    @GetMapping
    public String list(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("groups", groupService.search(keyword));
        model.addAttribute("keyword", keyword);
        model.addAttribute("group", new Group());
        model.addAttribute("users", userService.findNormalUsers());
        return "admin/groups/group-create";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Group group;
        try {
            group = groupService.getById(id);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return LIST_PAGE;
        }
        model.addAttribute("group", group);
        model.addAttribute("currentFund", groupService.getCurrentFund(group));
        model.addAttribute("availableUsers", inviteService.findInvitableUsers(id));
        return "admin/groups/group-detail";
    }

    // Dữ liệu điền vào modal sửa nhóm (AJAX). Chỉ trả các trường của form.
    @GetMapping("/{id}/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> json(@PathVariable Long id) {
        Group group;
        try {
            group = groupService.getById(id);
        } catch (BusinessException e) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", group.getId());
        json.put("name", group.getName());
        json.put("type", group.getType());
        json.put("description", group.getDescription());
        json.put("fundAmount", group.getFundAmount());
        json.put("targetAmount", group.getTargetAmount());
        json.put("active", group.isActive());
        return ResponseEntity.ok(json);
    }

    @PostMapping("/create")
    public String create(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                         @ModelAttribute Group form,
                         RedirectAttributes redirectAttributes) {
        try {
            groupService.create(form, currentUser.getId());
            redirectAttributes.addFlashAttribute("success", "Tạo nhóm thành công! Hãy gửi lời mời cho các thành viên.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return LIST_PAGE;
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute Group form, RedirectAttributes redirectAttributes) {
        try {
            groupService.update(id, form);
            redirectAttributes.addFlashAttribute("success", "Cập nhật nhóm thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return LIST_PAGE;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            groupService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa nhóm!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return LIST_PAGE;
    }

    // Mời thành viên từ trang danh sách nhóm
    @PostMapping("/{groupId}/invite-user")
    public String inviteFromList(@PathVariable Long groupId, @RequestParam Long userId,
                                 RedirectAttributes redirectAttributes) {
        invite(groupId, userId, redirectAttributes);
        return LIST_PAGE;
    }

    // Mời thành viên từ trang chi tiết nhóm
    @PostMapping("/{groupId}/members/add")
    public String inviteFromDetail(@PathVariable Long groupId, @RequestParam Long userId,
                                   RedirectAttributes redirectAttributes) {
        invite(groupId, userId, redirectAttributes);
        return "redirect:/admin/groups/" + groupId;
    }

    @PostMapping("/{groupId}/members/{userId}/remove")
    public String removeMember(@PathVariable Long groupId, @PathVariable Long userId,
                               RedirectAttributes redirectAttributes) {
        try {
            groupService.removeMember(groupId, userId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa thành viên khỏi nhóm! Các khoản họ chưa đóng cũng được bỏ.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/groups/" + groupId;
    }

    private void invite(Long groupId, Long userId, RedirectAttributes redirectAttributes) {
        try {
            User invited = inviteService.invite(groupId, userId);
            redirectAttributes.addFlashAttribute("success", "Đã gửi lời mời tham gia nhóm cho " + invited.getFullName() + "!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
    }
}
