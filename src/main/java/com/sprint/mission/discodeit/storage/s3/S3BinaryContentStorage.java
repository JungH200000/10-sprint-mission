package com.sprint.mission.discodeit.storage.s3;

import com.sprint.mission.discodeit.config.AwsProperties;
import com.sprint.mission.discodeit.dto.binarycontent.BinaryContentDto;
import com.sprint.mission.discodeit.exception.binarycontent.BinaryContentReadFailedException;
import com.sprint.mission.discodeit.exception.binarycontent.BinaryContentSaveFailedException;
import com.sprint.mission.discodeit.exception.binarycontent.PresignedUrlCreateFailedException;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "discodeit.storage.type", havingValue = "s3")
public class S3BinaryContentStorage implements BinaryContentStorage {

    private final AwsProperties awsProperties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3BinaryContentStorage(AwsProperties awsProperties, S3Client s3Client, S3Presigner s3Presigner) {
        this.awsProperties = awsProperties;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public UUID put(UUID binaryContentId, byte[] bytes) {
        String key = resolveKey(binaryContentId);

        try {
            // 어떤 파일을 업로드할지 담는 요청 객체 생성
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(awsProperties.getS3().getBucket())  // 어느 bucket에 저장할 건지
                    .key(key) // 어떤 이름으로 저장할 건지
                    .build();

            // 생성한 요청 객체를 바탕으로 업로드(저장)
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));
        } catch (Exception e) {
            throw new BinaryContentSaveFailedException(binaryContentId, e);
        }

        return binaryContentId;
    }

    @Override
    public InputStream get(UUID binaryContentId) {
        String key = resolveKey(binaryContentId);

        try {
            // 어떤 파일을 가져올지 담는 요청 객체 생성
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(awsProperties.getS3().getBucket())
                    .key(key)
                    .responseContentDisposition("attachment; filename=\"" + key + "\"")
                    .build();

            // 생성한 요청 객체를 바탕으로 S3에 파일을 요청
            return s3Client.getObject(getObjectRequest);
        } catch (Exception e) {
            throw new BinaryContentReadFailedException(binaryContentId, e);
        }
    }

    @Override
    public ResponseEntity<Void> download(BinaryContentDto binaryContentDto) {
        // InputStream :  바이트 데이터를 읽기 위한 통로
        String key = resolveKey(binaryContentDto.id());
        try {
            String signedUrl = generatePresignedUrl(key, binaryContentDto.fileName(), binaryContentDto.contentType());

            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.parseMediaType(binaryContentDto.contentType()))
                    .contentLength(binaryContentDto.size())
                    .location(URI.create(signedUrl))
                    .build();
        } catch (Exception e) {
            throw new PresignedUrlCreateFailedException(binaryContentDto.id(), e);
        }
    }

    private String resolveKey(UUID binaryContentId) {
        return binaryContentId.toString();
    }

    private String generatePresignedUrl(String key, String fileName, String contentType) {
        // 어떤 파일을 가져올지 담는 요청 객체 생성
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(awsProperties.getS3().getBucket())
                .key(key)
                .responseContentType(contentType)
                .responseContentDisposition("attachment; filename=\"" + fileName + "\"")
                .build();

        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(getObjectRequest)
                .signatureDuration(Duration.ofMinutes(5))
                .build();

        return s3Presigner.presignGetObject(getObjectPresignRequest).url().toString();
    }
}
