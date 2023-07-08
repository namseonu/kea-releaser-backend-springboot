package com.momentum.releaser.domain.issue.application;

import com.momentum.releaser.domain.issue.dao.IssueNumRepository;
import com.momentum.releaser.domain.issue.dao.IssueOpinionRepository;
import com.momentum.releaser.domain.issue.dao.IssueRepository;
import com.momentum.releaser.domain.issue.domain.*;
import com.momentum.releaser.domain.issue.dto.IssueReqDto.IssueInfoReq;
import com.momentum.releaser.domain.issue.dto.IssueReqDto.RegisterOpinionReq;
import com.momentum.releaser.domain.issue.dto.IssueReqDto.UpdateLifeCycleReq;
import com.momentum.releaser.domain.project.dao.ProjectMemberRepository;
import com.momentum.releaser.domain.project.dao.ProjectRepository;
import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.domain.release.dao.ReleaseRepository;
import com.momentum.releaser.domain.release.domain.ReleaseNote;
import com.momentum.releaser.global.config.BaseResponseStatus;
import com.momentum.releaser.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.momentum.releaser.domain.issue.dto.IssueResDto.*;
import static com.momentum.releaser.global.config.BaseResponseStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueServiceImpl implements IssueService {

    private final IssueRepository issueRepository;
    private final IssueOpinionRepository issueOpinionRepository;
    private final IssueNumRepository issueNumRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ReleaseRepository releaseRepository;
    private final ModelMapper modelMapper;

    /**
     * 7.1 이슈 생성
     */
    @Override
    @Transactional
    public String registerIssue(Long projectId, IssueInfoReq createReq) {
        ProjectMember projectMember = null;
        // memberId not null
        if (createReq.getMemberId() != null) {
            projectMember = findProjectMember(createReq.getMemberId());
        }
        Project project = findProject(projectId);
        Tag tagIssue = checkTagEnum(createReq.getTag());
        Issue newIssue = saveIssue(createReq, project, projectMember, tagIssue);
        String result = "이슈 생성이 완료되었습니다.";
        return result;
    }

    private IssueNum saveIssueNum(Project project, Issue newIssue, Long number) {

        return issueNumRepository.save(IssueNum.builder()
                        .issue(newIssue)
                        .project(project)
                        .issueNum(number)
                .build());
    }

    // memberId로 프로젝트 멤버 찾기
    private ProjectMember findProjectMember(Long memberId) {
        return projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(BaseResponseStatus.NOT_EXISTS_PROJECT_MEMBER));
    }

    // projectId로 프로젝트 찾기
    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(BaseResponseStatus.NOT_EXISTS_PROJECT));
    }

    // Tag enum check
    private Tag checkTagEnum(String tagValue) {
        EnumSet<Tag> tagEnum = EnumSet.allOf(Tag.class);
        for (Tag tag : tagEnum) {
            if (tag.name().equalsIgnoreCase(tagValue)) {
                return tag;
            }
        }
        throw new CustomException(INVALID_ISSUE_TAG);
    }

    // 이슈 저장
    private Issue saveIssue(IssueInfoReq issueInfoReq, Project project, ProjectMember projectMember, Tag tagIssue) {
        Long number = issueRepository.getIssueNum(project) + 1;
        Issue issue = issueRepository.save(Issue.builder()
                .title(issueInfoReq.getTitle())
                .content(issueInfoReq.getContent())
                .tag(tagIssue)
                .endDate(issueInfoReq.getEndDate())
                .project(project)
                .member(projectMember)
                .build());
        IssueNum issueNum = saveIssueNum(project, issue, number);
        issue.updateIssueNum(issueNum);
        return issue;
    }



    /**
     * 7.2 이슈 수정
     */
    @Override
    @Transactional
    public String updateIssue(Long issueId, Long memberId, IssueInfoReq updateReq) {
        // 이슈 정보 조회
        Issue issue = findIssue(issueId);

        //edit check
        char edit = editCheck(memberId);

        ProjectMember projectMember = null;
        // 담당자 memberId가 null이 아닌 경우 프로젝트 멤버 조회
        if (updateReq.getMemberId() != null) {
            projectMember = findProjectMember(updateReq.getMemberId());
        }

        // 태그 확인
        Tag tagIssue = checkTagEnum(updateReq.getTag());

        // 이슈 업데이트
        issue.updateIssue(updateReq, edit, projectMember, tagIssue);
        issueRepository.save(issue);

        String result = "이슈 수정이 완료되었습니다.";
        return result;
    }

    // issueId로 issue 조회
    private Issue findIssue(Long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new CustomException(NOT_EXISTS_ISSUE));
    }

    // 멤버의 역할에 따라 edit 상태를 결정
    private char editCheck(Long memberId) {
        ProjectMember projectMember = findProjectMember(memberId);
        return (projectMember.getPosition() == 'M') ? 'Y' : 'N';
    }


    /**
     * 7.3 이슈 제거
     */
    @Override
    @Transactional
    public String deleteIssue(Long issueId) {
        //issue
        Issue issue = findIssue(issueId);

        //issue와 연결된 릴리즈가 있으면 삭제 안됨
        if (issue.getRelease() != null) {
            throw new CustomException(CONNECTED_RELEASE_EXISTS);
        }

        issueRepository.deleteIssueByIssueNum();
        issueRepository.deleteById(issue.getIssueId());

        return "이슈가 삭제되었습니다.";
    }

    /**
     * 7.4 프로젝트별 모든 이슈 조회
     */
    @Override
    @Transactional(readOnly = true)
    public GetIssuesList getIssues(Long projectId) {
        Project findProject = findProject(projectId);
        List<IssueInfoRes> getAllIssue = issueRepository.getIssues(findProject);

        List<IssueInfoRes> notStartedList = filterIssuesByLifeCycle(getAllIssue, "Not_Started");
        List<IssueInfoRes> inProgressList = filterIssuesByLifeCycle(getAllIssue, "In_Progress");
        List<IssueInfoRes> doneList = filterIssuesByLifeCycle(getAllIssue, "Done");

        return GetIssuesList.builder()
                .getNotStartedList(notStartedList)
                .getInProgressList(inProgressList)
                .getDoneList(doneList)
                .build();
    }

    private List<IssueInfoRes> filterIssuesByLifeCycle(List<IssueInfoRes> issues, String lifeCycle) {
        return issues.stream()
                .filter(issue -> lifeCycle.equals(issue.getLifeCycle()))
                .map(issue -> modelMapper.map(issue, IssueInfoRes.class))
                .collect(Collectors.toList());
    }


    /**
     * 7.5 프로젝트별 해결 & 미연결 이슈 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<GetDoneIssues> getDoneIssues(Long projectId) {
        Project findProject = findProject(projectId);
        List<GetDoneIssues> getDoneIssue = issueRepository.getDoneIssues(findProject);

        return getDoneIssue;
    }
    /**
     * 7.6 릴리즈 노트별 연결된 이슈 조회
     */
    @Override
    public List<GetConnectionIssues> getConnectRelease(Long projectId, Long releaseId) {
        Project findProject = findProject(projectId);
        ReleaseNote findReleaseNote = findReleaseNote(releaseId);

        List<GetConnectionIssues> getConnectionIssues = issueRepository.getConnectionIssues(findProject, findReleaseNote);

        return getConnectionIssues;
    }

    //releaseId로 releaseNote 찾기
    private ReleaseNote findReleaseNote(Long releaseId) {
        return releaseRepository.findById(releaseId)
                .orElseThrow(() -> new CustomException(NOT_EXISTS_RELEASE_NOTE));
    }

    /**
     * 7.7 이슈별 조회
     */

    /**
     * 7.8 이슈 상태 변경
     */
    @Override
    public String updateLifeCycle(Long issueId, UpdateLifeCycleReq lifeCycleReq) {
        //issue 정보
        Issue issue = findIssue(issueId);

        //연결된 이슈가 있을 경우 validation
        if (issue.getRelease() != null) {
            throw new CustomException(CONNECTED_ISSUE_EXISTS);
        }

        //이슈 상태 변경
        String result = changeLifeCycle(issue, lifeCycleReq.getLifeCycle());
        return result;
    }

    private String changeLifeCycle(Issue issue, String lifeCycle) {
        //check lifeCycle
        LifeCycle lifeCycleIssue = checkLifeCycleEnum(lifeCycle);

        issue.updateLifeCycle(lifeCycleIssue);
        issueRepository.save(issue);
        return "이슈 상태 변경이 완료되었습니다.";

    }

    //check lifeCycle
    private LifeCycle checkLifeCycleEnum(String lifeCycleValue) {
        EnumSet<LifeCycle> lifeCycleEnum = EnumSet.allOf(LifeCycle.class);
        for (LifeCycle lifeCycle : lifeCycleEnum) {
            if (lifeCycle.name().equalsIgnoreCase(lifeCycleValue)) {
                return lifeCycle;
            }
        }
        throw new CustomException(INVALID_LIFECYCLE);
    }

    /**
     * 8.1 이슈 의견 추가
     */
    @Override
    public List<OpinionInfoRes> registerOpinion(Long issueId, Long memberId, RegisterOpinionReq issueOpinionReq) {
        //issue
        Issue issue = findIssue(issueId);
        //project member
        ProjectMember member = findProjectMember(memberId);

        //save opinion
        IssueOpinion issueOpinion = saveOpinion(issue, member, issueOpinionReq.getOpinion());

        List<OpinionInfoRes> opinionRes = issueRepository.getIssueOpinion(issue);

        return opinionRes;
    }

    private IssueOpinion saveOpinion(Issue issue, ProjectMember member, String opinion) {
        //Add issue
        return issueOpinionRepository.save(IssueOpinion.builder()
                .opinion(opinion)
                .issue(issue)
                .member(member)
                .build());
    }

    /**
     * 8.2 이슈 의견 삭제
     */
    @Override
    public String deleteOpinion(Long opinionId) {
        //opinion
        IssueOpinion issueOpinion = issueOpinionRepository.findById(opinionId).orElseThrow(() -> new CustomException(NOT_EXISTS_ISSUE_OPINION));
        //opinion soft delete
        issueOpinionRepository.deleteById(opinionId);
        return "해당 이슈 의견이 삭제되었습니다.";
    }
}
