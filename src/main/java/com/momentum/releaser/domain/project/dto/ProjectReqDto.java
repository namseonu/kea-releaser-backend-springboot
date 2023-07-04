package com.momentum.releaser.domain.project.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class ProjectReqDto {
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ProjectInfoReq {

        @NotBlank
        @NotNull(message = "프로젝트명을 입력해주세요.")
        private String title;
        @NotBlank
        @NotNull(message = "팀명을 입력해주세요.")
        private String team;
        private String img;

        @Builder
        public ProjectInfoReq(String title, String team, String img) {
            this.title = title;
            this.team = team;
            this.img = img;
        }
    }
}
