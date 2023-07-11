package com.momentum.releaser.global.config.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.momentum.releaser.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

import static com.momentum.releaser.global.config.BaseResponseStatus.NOT_EXISTS_S3_FILE;

@RequiredArgsConstructor
@Service
public class S3Upload {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private final AmazonS3 amazonS3;

    /**
     * S3 파일 업로드
     */
    public String upload(MultipartFile multipartFile, String dirName) throws IOException {
        // 파일 이름이 중복되지 않게 생성
        String fileName = createS3FileName(multipartFile, dirName);

        // S3에 파일 업로드
        return uploadFileToS3(multipartFile, fileName);
    }

    /**
     * S3에 업로드된 파일을 삭제한다.
     */
    public void delete(String fileName) {
        validateFileExist(fileName);
        amazonS3.deleteObject(bucket, fileName);
    }

    // =================================================================================================================

    /**
     * S3에 업로드하는 파일의 이름을 중복되지 않게 생성한다.
     */
    private String createS3FileName(MultipartFile multipartFile, String dirName) {
        return dirName + "/" + UUID.randomUUID() + "-" + multipartFile.getOriginalFilename();
    }

    /**
     * S3에 파일을 업로드한다.
     */
    private String uploadFileToS3(MultipartFile multipartFile, String fileName) throws IOException {
        // 파일의 사이즈를 ContentLength로 S3에게 알려준다.
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(multipartFile.getInputStream().available());

        // S3 API 메서드인 putObject를 이용하여 파일 스트림(Stream)을 열어 S3에 파일을 업로드한다.
        amazonS3.putObject(bucket, fileName, multipartFile.getInputStream(), objectMetadata);

        // getUrl 메서드를 통해서 S3에 업로드된 사진 URL을 가져온다.
        return amazonS3.getUrl(bucket, fileName).toString();
    }

    /**
     * S3 파일 삭제 요청이 들어온 경우 해당 파일이 존재하는 파일인지 검사한다.
     */
    private void validateFileExist(String fileName) {
        if (!amazonS3.doesObjectExist(bucket, fileName)) {
            throw new CustomException(NOT_EXISTS_S3_FILE);
        }
    }
}
