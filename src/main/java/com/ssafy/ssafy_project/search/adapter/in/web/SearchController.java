package com.ssafy.ssafy_project.search.adapter.in.web;

import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.search.application.service.UnifiedSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SearchController {

    private final UnifiedSearchService unifiedSearchService;

    @GetMapping("/api/search")
    public ResponseEntity<UnifiedSearchService.SearchResult> search(
            @AuthenticationPrincipal Long userId,
            @RequestParam("q") String query
    ) {
        String trimmed = query != null ? query.trim() : "";
        if (trimmed.length() < 2 || trimmed.length() > 300) {
            throw new CustomException(CommonErrorCode.VALIDATION_ERROR);
        }
        return ResponseEntity.ok(unifiedSearchService.search(userId, trimmed));
    }
}
