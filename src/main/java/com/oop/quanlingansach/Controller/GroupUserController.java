package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.Config.SessionKeys;
import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Service.BusinessException;
import com.oop.quanlingansach.Service.GroupInviteService;
import com.oop.quanlingansach.Service.GroupService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * User xem nhóm của mình, xử lý lời mời và rời nhóm.
 */
@Controller
@RequestMapping("/user/groups")
public class GroupUserController {

    private static final String MY_GROUPS_PAGE = "redirect:/user/groups";
    private static final String INVITES_PAGE = "redirect:/user/groups/invites";

    private final GroupService groupService;
    private final GroupInviteService inviteService;

    public GroupUserController(GroupService groupService, GroupInviteService inviteService) {
        this.groupService = groupService;
        this.inviteService = inviteService;
    }

    @GetMapping
    public String myGroups(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser, Model model) {
        List<Group> groups = groupService.findGroupsOfMember(currentUser.getId());
        long adminCount = groups.stream().filter(g -> currentUser.getId().equals(g.getAdminId())).count();

        model.addAttribute("groups", groups);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("adminCount", adminCount);
        model.addAttribute("memberCount", groups.size() - adminCount);
        model.addAttribute("groupFunds", groupService.getCurrentFunds(groups));
        return "user/groups/my-groups";
    }

    @GetMapping("/{groupId}")
    public String groupDetail(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                              @PathVariable Long groupId,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            Group group = groupService.getById(groupId);
            if (!group.hasMember(currentUser.getId())) {
                throw new BusinessException("Bạn không phải thành viên nhóm này!");
            }
            addGroupDetail(group, currentUser, model);
            return "user/groups/group-detail";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return MY_GROUPS_PAGE;
        }
    }

    @PostMapping("/{groupId}/leave")
    public String leave(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                        @PathVariable Long groupId,
                        RedirectAttributes redirectAttributes) {
        try {
            groupService.leave(groupId, currentUser.getId());
            redirectAttributes.addFlashAttribute("success", "Bạn đã rời khỏi nhóm thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return MY_GROUPS_PAGE;
    }

    // ==================== LỜI MỜI ====================

    @GetMapping("/invites")
    public String invites(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser, Model model) {
        model.addAttribute("invites", inviteService.findPendingInvites(currentUser.getId()));
        return "user/groups/invites";
    }

    // Xem thông tin nhóm trước khi quyết định chấp nhận lời mời
    @GetMapping("/invites/{inviteId}/group")
    public String groupFromInvite(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                                  @PathVariable Long inviteId,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        try {
            Group group = inviteService.getPendingInviteOfUser(inviteId, currentUser.getId()).getGroup();
            addGroupDetail(group, currentUser, model);
            return "user/groups/group-detail";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return INVITES_PAGE;
        }
    }

    @PostMapping("/invites/{inviteId}/accept")
    public String accept(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                         @PathVariable Long inviteId,
                         RedirectAttributes redirectAttributes) {
        try {
            inviteService.accept(inviteId, currentUser.getId());
            redirectAttributes.addFlashAttribute("success", "Bạn đã tham gia nhóm thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return INVITES_PAGE;
    }

    @PostMapping("/invites/{inviteId}/decline")
    public String decline(@SessionAttribute(SessionKeys.CURRENT_USER) User currentUser,
                          @PathVariable Long inviteId,
                          RedirectAttributes redirectAttributes) {
        try {
            inviteService.decline(inviteId, currentUser.getId());
            redirectAttributes.addFlashAttribute("success", "Bạn đã từ chối lời mời tham gia nhóm.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return INVITES_PAGE;
    }

    private void addGroupDetail(Group group, User currentUser, Model model) {
        model.addAttribute("group", group);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("members", group.getMembers());
        model.addAttribute("currentFund", groupService.getCurrentFund(group));
        model.addAttribute("targetAmount", group.getTargetAmount());
    }
}
