package com.beam;

import com.beam.util.InputSanitizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Search", description = "메시지 검색 API")
@SecurityRequirement(name = "bearerAuth")
public class SearchController {

    @Value("${app.search.max-keyword-length:100}")
    private int maxKeywordLength;

    @Autowired
    private MessageSearchService messageSearchService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private InputSanitizer inputSanitizer;

    @Operation(summary = "메시지 검색", description = "DM 또는 채팅방 메시지를 키워드로 검색합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "검색 결과 반환"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (키워드 없음 또는 너무 김)"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/messages")
    public ResponseEntity<?> searchMessages(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token,
            @Parameter(description = "검색 키워드 (최대 100자)", required = true)
            @RequestParam String keyword,
            @Parameter(description = "검색 유형 (DM, ROOM, 또는 전체)")
            @RequestParam(required = false) String type) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(jwtToken);

            // 키워드 유효성 검증
            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Keyword is required"));
            }

            // 키워드 길이 제한
            if (keyword.length() > maxKeywordLength) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Keyword too long",
                    "maxLength", maxKeywordLength
                ));
            }

            // 입력 살균 - XSS 및 검색 인젝션 방지
            String sanitizedKeyword = inputSanitizer.sanitizeSearchQuery(keyword);

            // 타입 유효성 검증
            if (type != null && !type.isEmpty() &&
                !type.equalsIgnoreCase("DM") && !type.equalsIgnoreCase("ROOM")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid type. Must be 'DM', 'ROOM', or empty for all"
                ));
            }

            List<Map<String, Object>> results;

            if ("DM".equalsIgnoreCase(type)) {
                results = messageSearchService.searchDirectMessages(userId, sanitizedKeyword);
            } else if ("ROOM".equalsIgnoreCase(type)) {
                results = messageSearchService.searchRoomMessages(userId, sanitizedKeyword);
            } else {
                results = messageSearchService.searchAllMessages(userId, sanitizedKeyword);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("keyword", keyword); // 원본 키워드 반환 (디스플레이용)
            response.put("count", results.size());
            response.put("results", results);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}