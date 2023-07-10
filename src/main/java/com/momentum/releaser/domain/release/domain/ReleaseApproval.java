package com.momentum.releaser.domain.release.domain;

import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.global.common.BaseTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "release_approval")
@Entity
public class ReleaseApproval extends BaseTime {

    @Id
    @Column(name = "approval_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long approvalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private ProjectMember member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_id")
    private ReleaseNote release;

    @Column(name = "approval")
    private char approval;

    @Builder
    public ReleaseApproval(ProjectMember member, ReleaseNote release) {
        this.member = member;
        this.release = release;
    }

    @PrePersist
    public void prePersist() {
        this.approval = (this.approval == '\0') ? 'N' : this.approval;
    }

    /**
     * 릴리즈 노트 배포 동의 여부
     */
    public void updateApproval(char approval) {
        this.approval = approval;
    }
}
