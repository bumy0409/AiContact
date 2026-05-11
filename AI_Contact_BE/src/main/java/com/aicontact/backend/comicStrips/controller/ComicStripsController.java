package com.aicontact.backend.comicStrips.controller;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aicontact.backend.auth.dto.CustomUserDetails;
import com.aicontact.backend.comicStrips.dto.request.CreateComicStripsRequest;
import com.aicontact.backend.comicStrips.dto.request.UpdateComicStripsTitleRequest;
import com.aicontact.backend.comicStrips.dto.response.ComicStripsListResponse;
import com.aicontact.backend.comicStrips.dto.response.ComicStripsResponse;
import com.aicontact.backend.comicStrips.entity.ComicStripsEntity;
import com.aicontact.backend.comicStrips.service.ComicStripsService;
import com.aicontact.backend.global.dto.response.ApiResponse;
import com.aicontact.backend.user.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/comic")
@RequiredArgsConstructor
public class ComicStripsController {

    private final ComicStripsService comicStripsService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<ComicStripsResponse>> createComicStrips(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CreateComicStripsRequest req) throws IOException {
        String myEmail = userDetails.getUserEntity().getEmail();
        var me = userService.getUserByEmail(myEmail);
        Long coupleId = me.getCoupleId();
        Long userId = me.getId();

        ComicStripsEntity created = comicStripsService.createComicStrips(
                coupleId,
                userId,
                req.getLocation(),
                req.getActivity(),
                req.getWeather());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(new ComicStripsResponse(created)));
    }

    @PatchMapping("/{id}/title")
    public ResponseEntity<ApiResponse<ComicStripsResponse>> updateTitle(
            @PathVariable("id") Long id,
            @RequestBody UpdateComicStripsTitleRequest request) {
        ComicStripsEntity updated = comicStripsService.updateTitle(id, request.getTitle());
        return ResponseEntity.ok(ApiResponse.success(new ComicStripsResponse(updated)));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<ComicStripsListResponse>>> getAllComicStrips(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String myEmail = userDetails.getUserEntity().getEmail();
        Long coupleId = userService.getUserByEmail(myEmail).getCoupleId();

        List<ComicStripsEntity> entities = comicStripsService.getAllComicStrips(coupleId);
        List<ComicStripsListResponse> dtos = entities.stream()
                .map(ComicStripsListResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteComicStrips(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String email = userDetails.getUserEntity().getEmail();
        Long coupleId = userService.getUserByEmail(email).getCoupleId();

        comicStripsService.deleteComicStrips(id, coupleId);
        return ResponseEntity.ok(ApiResponse.success("삭제되었습니다."));
    }

}
