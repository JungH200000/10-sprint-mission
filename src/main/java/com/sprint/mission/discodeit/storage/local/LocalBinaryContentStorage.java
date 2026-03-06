package com.sprint.mission.discodeit.storage.local;


import com.sprint.mission.discodeit.dto.binarycontent.BinaryContentDto;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "discodeit.storage.type", havingValue = "local")
public class LocalBinaryContentStorage implements BinaryContentStorage {
    private final Path root;

    public LocalBinaryContentStorage(@Value("${discodeit.storage.local.root-path}") String root) {
        this.root = Path.of(root);
    }

    /**
     * 루트 디렉토리 초기화 <br>
     * Bean이 생성되면 자동으로 호출
     */
    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("디렉토리 초기화 실패", e);
        }
    }

    /**
     * 파일의 실제 저장 위치에 대한 규칙을 정의 <br>
     * 파일 저장 위치 규칙 예시 : {@code {root}/{UUID}} <br>
     * {@code put}, {@code get} 메서드에서 호출해 일관된 파일 경로 규칙을 유지
     * @param binaryContentId
     * @return {@code Path}
     */
    public Path resolvePath(UUID binaryContentId) {
        // root 하위 경로로 새로운 Path를 만듬
        // root = "/storage/binary" 이면 "/storage/binary/UUID.toString"이 만들어짐
        return root.resolve(binaryContentId.toString());
    }

    @Override
    public UUID put(UUID binaryContentId, byte[] bytes) {
        Path path = resolvePath(binaryContentId);

        try {
            Files.write(path, bytes);
        } catch (IOException e) {
            throw new IllegalArgumentException("저장 실패", e);
        }

        return binaryContentId;
    }

    @Override
    public InputStream get(UUID binaryContentId) {
        Path path = resolvePath(binaryContentId);

        try {
            return Files.newInputStream(path);
        } catch(IOException e) {
            throw new IllegalStateException("조회 실패", e);
        }
    }

    @Override
    public ResponseEntity<Resource> download(BinaryContentDto binaryContentDto) {
        InputStream inputStream = get(binaryContentDto.id());
        InputStreamResource resource = new InputStreamResource(inputStream);

        return null;
    }
}
