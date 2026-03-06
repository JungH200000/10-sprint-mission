package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.binarycontent.BinaryContentDto;
import com.sprint.mission.discodeit.service.BinaryContentService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 바이너리 파일 다운로드 Controller
 */
@RestController
@RequestMapping("/api/binaryContents")
@AllArgsConstructor
@Tag(name = "BinaryContent", description = "첨부 파일 API")
public class BinaryContentController {
    private final BinaryContentService binaryContentService;
    private final BinaryContentStorage binaryContentStorage;

    /**
     * 바이너리 파일 1개 조회
     */
    @RequestMapping(value = "/{binaryContentId}", method = RequestMethod.GET)
    @Operation(summary = "첨부 파일 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "첨부 파일 조회 성공"),
            @ApiResponse(responseCode = "404", description = "첨부 파일을 찾을 수 없음", content = @Content(examples = @ExampleObject(value = "BinaryContent with id {binaryContentId} not found")))
    })
    public ResponseEntity<BinaryContentDto> find(
            @Parameter(description = "조회할 첨부 파일 ID") @PathVariable UUID binaryContentId
    ) {
        BinaryContentDto binaryContentDto = binaryContentService.find(binaryContentId);

        return ResponseEntity.status(HttpStatus.OK).body(binaryContentDto);
    }

    /**
     * 바이너리 파일 여러 개 조회
     */
    @RequestMapping(method = RequestMethod.GET)
    @Operation(summary = "여러 첨부 파일 조회")
    @ApiResponse(responseCode = "200", description = "첨부 파일 목록 조회 성공")
    public ResponseEntity<List<BinaryContentDto>> findAllByIdIn(
            @Parameter(description = "조회할 첨부 파일 ID 목록") @RequestParam List<UUID> binaryContentIds
    ) {
        List<BinaryContentDto> binaryContents = binaryContentService.findAllByIdIn(binaryContentIds);

        return ResponseEntity.status(HttpStatus.OK).body(binaryContents);
    }

    /**
     * 바이너리 파일 다운로드
     */
    @RequestMapping(value = "/{binaryContentId}/download", method = RequestMethod.GET)
    @Operation(summary = "파일 다운로드")
    @ApiResponse(responseCode = "200", description = "파일 다운로드 성공")
    public ResponseEntity<?> download(
            @Parameter(description = "다운로드할 파일 ID") @PathVariable UUID binaryContentId
    ) {
        BinaryContentDto binaryContentDto = binaryContentService.find(binaryContentId);

        return binaryContentStorage.download(binaryContentDto);
    }
}
