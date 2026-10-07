package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.TestWebConfig;
import org.springframework.context.annotation.Import;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Model.GroupInvite;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Service.BusinessException;
import com.oop.quanlingansach.Service.GroupInviteService;
import com.oop.quanlingansach.Service.GroupService;
import com.oop.quanlingansach.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * User xem nhóm, xem/chấp nhận/từ chối lời mời, rời nhóm.
 */
@Import(TestWebConfig.class)
@WebMvcTest(GroupUserController.class)
class GroupUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroupService groupService;

    @MockitoBean
    private GroupInviteService inviteService;

    private User member;
    private MockHttpSession session;
    private Group group;

    @BeforeEach
    void setUp() {
        member = TestData.member(2L);
        session = TestData.sessionOf(member);
        group = TestData.group(100L, member);
    }

    @Test
    void myGroups_ShouldRenderWithFunds() throws Exception {
        when(groupService.findGroupsOfMember(2L)).thenReturn(List.of(group));
        when(groupService.getCurrentFunds(List.of(group))).thenReturn(Map.of(100L, new BigDecimal("50000")));

        mockMvc.perform(get("/user/groups").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("user/groups/my-groups"))
                .andExpect(model().attribute("memberCount", 1L));
    }

    @Test
    void groupDetail_Member_ShouldRender() throws Exception {
        when(groupService.getById(100L)).thenReturn(group);
        when(groupService.getCurrentFund(group)).thenReturn(BigDecimal.ZERO);

        mockMvc.perform(get("/user/groups/100").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("user/groups/group-detail"));
    }

    @Test
    void groupDetail_NotAMember_ShouldGoBack() throws Exception {
        when(groupService.getById(100L)).thenReturn(TestData.group(100L));

        mockMvc.perform(get("/user/groups/100").session(session))
                .andExpect(redirectedUrl("/user/groups"))
                .andExpect(flash().attribute("error", "Bạn không phải thành viên nhóm này!"));
    }

    @Test
    void invites_ShouldRender() throws Exception {
        GroupInvite invite = new GroupInvite(group, member);
        ReflectionTestUtils.setField(invite, "id", 7L);
        when(inviteService.findPendingInvites(2L)).thenReturn(List.of(invite));

        mockMvc.perform(get("/user/groups/invites").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("user/groups/invites"));
    }

    @Test
    void invites_NotLoggedIn_ShouldGoToLogin() throws Exception {
        mockMvc.perform(get("/user/groups/invites")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void accept_ShouldCallService() throws Exception {
        mockMvc.perform(post("/user/groups/invites/7/accept").with(csrf()).session(session))
                .andExpect(redirectedUrl("/user/groups/invites"))
                .andExpect(flash().attributeExists("success"));

        verify(inviteService).accept(7L, 2L);
    }

    @Test
    void decline_Error_ShouldShowMessage() throws Exception {
        doThrow(new BusinessException("Lời mời đã được xử lý trước đó!")).when(inviteService).decline(7L, 2L);

        mockMvc.perform(post("/user/groups/invites/7/decline").with(csrf()).session(session))
                .andExpect(flash().attribute("error", "Lời mời đã được xử lý trước đó!"));
    }

    @Test
    void leave_ShouldCallService() throws Exception {
        mockMvc.perform(post("/user/groups/100/leave").with(csrf()).session(session))
                .andExpect(redirectedUrl("/user/groups"));

        verify(groupService).leave(100L, 2L);
    }
}
