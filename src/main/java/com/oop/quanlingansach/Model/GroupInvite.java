package com.oop.quanlingansach.Model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Lời mời một user vào nhóm. User chấp nhận thì trở thành thành viên.
 */
@Entity
@Table(name = "group_invites")
public class GroupInvite {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_DECLINED = "DECLINED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String status = STATUS_PENDING;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    public GroupInvite() {}

    public GroupInvite(Group group, User user) {
        this.group = group;
        this.user = user;
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    /** User chấp nhận: lời mời đóng lại và user trở thành thành viên nhóm. */
    public void accept() {
        requirePending();
        status = STATUS_ACCEPTED;
        group.addMember(user);
    }

    public void decline() {
        requirePending();
        status = STATUS_DECLINED;
    }

    private void requirePending() {
        if (!isPending()) {
            throw new IllegalStateException("Lời mời đã được xử lý");
        }
    }

    public Long getId() { return id; }

    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
