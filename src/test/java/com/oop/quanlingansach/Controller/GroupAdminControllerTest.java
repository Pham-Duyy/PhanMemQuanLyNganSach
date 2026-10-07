package com.oop.quanlingansach.Controller;

import com.oop.quanlingansach.TestWebConfig;
import org.springframework.context.annotation.Import;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import com.oop.quanlingansach.Model.Group;
import com.oop.quanlingansach.Model.User;
import com.oop.quanlingansach.Service.BusinessException;
import com.oop.quanlingansach.Service.GroupInviteService;
import com.oop.quanlingansach.Service.GroupService;
import com.oop.quanlingansach.Service.UserService;
import com.oop.quanlingansach.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Admin quản lý nhóm: danh sách, chi tiết, JSON sửa nhóm, tạo/sửa/xóa, mời thành viên.
 */
@Import(TestWebConfig.class)
@WebMvcTest(GroupAdminController.class)
class GroupAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroupService groupService;

    @MockitoBean
    private GroupInviteService inviteService;

    @MockitoBean
    private UserService userService;

    private MockHttpSession adminSession;
    private Group group;

    @BeforeEach
    void setUp() {
        adminSession = TestData.sessionOf(TestData.admin());
        group = TestData.group(10L, TestData.member(2L));
    }

    // ===================== XEM =====================
    @Test
    void list_ShouldRenderWithSearchResult() throws Exception {
        when(groupService.search("Java")).thenReturn(List.of(group));
        when(userService.findNormalUsers()).thenReturn(List.of(TestData.member(2L)));

        mockMvc.perform(get("/admin/groups").session(adminSession).param("keyword", "Java"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/groups/group-create"))
                .andExpect(model().attribute("groups", List.of(group)));
    }

    @Test
    void detail_ShouldRenderWithFundAndInvitableUsers() throws Exception {
        when(groupService.getById(10L)).thenReturn(group);
        when(groupService.getCurrentFund(group)).thenReturn(new BigDecimal("250000"));
        when(inviteService.findInvitableUsers(10L)).thenReturn(List.of(TestData.member(3L)));

        mockMvc.perform(get("/admin/groups/10").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/groups/group-detail"))
                .andExpect(content().string(containsString("member3")));
    }

    @Test
    void detail_NotFound_ShouldGoBackToList() throws Exception {
        when(groupService.getById(99L)).thenThrow(new BusinessException("Nhóm không tồn tại!"));

        mockMvc.perform(get("/admin/groups/99").session(adminSession))
                .andExpect(redirectedUrl("/admin/groups"))
                .andExpect(flash().attribute("error", "Nhóm không tồn tại!"));
    }

    @Test
    void json_ShouldReturnOnlyFormFields() throws Exception {
        group.getMembers().get(0).setPassword("secret123");
        group.setFundAmount(new BigDecimal("500000"));
        when(groupService.getById(10L)).thenReturn(group);

        mockMvc.perform(get("/admin/groups/10/json").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fundAmount").value(500000))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.members").doesNotExist())
                .andExpect(content().string(not(containsString("secret123"))));
    }

    @Test
    void json_NotFound_ShouldReturn404() throws Exception {
        when(groupService.getById(99L)).thenThrow(new BusinessException("Nhóm không tồn tại!"));

        mockMvc.perform(get("/admin/groups/99/json").session(adminSession))
                .andExpect(status().isNotFound());
    }

    // ===================== TẠO / SỬA / XÓA =====================
    @Test
    void create_ShouldUseLoggedInAdminAsOwner() throws Exception {
        mockMvc.perform(post("/admin/groups/create").with(csrf()).session(adminSession).param("name", "Nhóm Mới"))
                .andExpect(redirectedUrl("/admin/groups"))
                .andExpect(flash().attributeExists("success"));

        verify(groupService).create(argThat(form -> "Nhóm Mới".equals(form.getName())), eq(1L));
        verifyNoInteractions(inviteService);
    }

    @Test
    void update_ShouldCallService() throws Exception {
        mockMvc.perform(post("/admin/groups/10/edit").with(csrf()).session(adminSession).param("name", "Tên Mới"))
                .andExpect(redirectedUrl("/admin/groups"));

        verify(groupService).update(eq(10L), argThat(form -> "Tên Mới".equals(form.getName())));
    }

    @Test
    void delete_ShouldCallService() throws Exception {
        mockMvc.perform(post("/admin/groups/10/delete").with(csrf()).session(adminSession))
                .andExpect(redirectedUrl("/admin/groups"));

        verify(groupService).delete(10L);
    }

    // ===================== PHÂN QUYỀN =====================
    @Test
    void delete_NotLoggedIn_ShouldBeBlocked() throws Exception {
        mockMvc.perform(post("/admin/groups/10/delete").with(csrf())).andExpect(redirectedUrl("/login"));

        verify(groupService, never()).delete(anyLong());
    }

    @Test
    void delete_NormalUser_ShouldBeBlocked() throws Exception {
        mockMvc.perform(post("/admin/groups/10/delete").with(csrf()).session(TestData.sessionOf(TestData.member(2L))))
                .andExpect(redirectedUrl("/"));

        verify(groupService, never()).delete(anyLong());
    }

    // ===================== MỜI / XÓA THÀNH VIÊN =====================
    @Test
    void inviteFromDetail_Success_ShouldReturnToDetail() throws Exception {
        User invited = TestData.member(3L);
        when(inviteService.invite(10L, 3L)).thenReturn(invited);

        mockMvc.perform(post("/admin/groups/10/members/add").with(csrf()).session(adminSession).param("userId", "3"))
                .andExpect(redirectedUrl("/admin/groups/10"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void inviteFromList_Error_ShouldShowMessage() throws Exception {
        when(inviteService.invite(10L, 1L)).thenThrow(new BusinessException("Chỉ có thể mời tài khoản người dùng thường!"));

        mockMvc.perform(post("/admin/groups/10/invite-user").with(csrf()).session(adminSession).param("userId", "1"))
                .andExpect(redirectedUrl("/admin/groups"))
                .andExpect(flash().attribute("error", "Chỉ có thể mời tài khoản người dùng thường!"));
    }

    @Test
    void removeMember_ShouldCallService() throws Exception {
        mockMvc.perform(post("/admin/groups/10/members/2/remove").with(csrf()).session(adminSession))
                .andExpect(redirectedUrl("/admin/groups/10"));

        verify(groupService).removeMember(10L, 2L);
    }
}
