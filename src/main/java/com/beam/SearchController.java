package com.beam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private MessageSearchService messageSearchService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/messages")
    public ResponseEntity<?> searchMessages(
            @RequestHeader("Authorization") String token,
            @RequestParam String keyword,
            @RequestParam(required = false) String type) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(jwtToken);

            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Keyword is required"));
            }

            List<Map<String, Object>> results;

            if ("DM".equalsIgnoreCase(type)) {
                results = messageSearchService.searchDirectMessages(userId, keyword);
            } else if ("ROOM".equalsIgnoreCase(type)) {
                results = messageSearchService.searchRoomMessages(userId, keyword);
            } else {
                results = messageSearchService.searchAllMessages(userId, keyword);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("keyword", keyword);
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