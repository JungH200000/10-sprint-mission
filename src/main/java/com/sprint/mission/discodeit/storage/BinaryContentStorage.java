package com.sprint.mission.discodeit.storage;

import com.sprint.mission.discodeit.dto.binarycontent.BinaryContentDto;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;
import java.util.UUID;

public interface BinaryContentStorage {
    /**
     * binaryContentId로 `byte[]` 데이터 저장
     * @param binaryContentId
     * @param bytes
     * @return {@code UUID}
     */
    UUID put(UUID binaryContentId, byte[] bytes);

    /**
     * binaryContentId로 `byte[]` 데이터를 읽어 InputStream 타입으로 반환
     * @param binaryContentId
     * @return {@code InputStream}
     */
    InputStream get(UUID binaryContentId);

    /**
     * HTTP API로 다운로드 <br>
     * BinaryContentDto로 파일을 다운로드할 수 있는 응답 반환
     * @param binaryContentDto
     * @return {@code ResponseEntity<?>}
     */
    ResponseEntity<?> download(BinaryContentDto binaryContentDto);

}
