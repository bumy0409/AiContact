package com.aicontact.backend.comicStrips.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.aicontact.backend.comicStrips.entity.ComicStripsEntity;
import com.aicontact.backend.comicStrips.repository.ComicStripsRepository;
import com.aicontact.backend.couple.entity.CoupleEntity;
import com.aicontact.backend.couple.repository.CoupleRepository;
import com.aicontact.backend.user.entity.UserEntity;
import com.aicontact.backend.user.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ComicStripsService {

    private final ComicStripsRepository comicStripsRepo;
    private final CoupleRepository coupleRepo;
    private final UserRepository userRepo;
    private final ComicStripsImagenService imagenService;
    private final ComicStripsAsyncService asyncService;

    @Transactional
    public ComicStripsEntity createComicStrips(Long coupleId, Long userId, String location, String activity, String weather) {
        CoupleEntity couple = coupleRepo.findById(coupleId)
                .orElseThrow(() -> new EntityNotFoundException("Couple not found: " + coupleId));
        UserEntity creator = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        // 엔티티 먼저 저장 (imageUrl은 null)
        ComicStripsEntity comicStrips = new ComicStripsEntity();
        comicStrips.setCouple(couple);
        comicStrips.setCreator(creator);
        comicStripsRepo.save(comicStrips);

        // 백그라운드에서 이미지 생성
        asyncService.markProcessing(comicStrips.getId());
        asyncService.generateImageAsync(comicStrips.getId(), location, activity, weather, coupleId);

        return comicStrips;
    }

    @Transactional
    public ComicStripsEntity updateTitle(Long id, String title) {
        ComicStripsEntity comicStrips = comicStripsRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ComicStrips not found: " + id));
        comicStrips.setTitle(title);
        return comicStripsRepo.save(comicStrips);
    }

    public List<ComicStripsEntity> getAllComicStrips(Long coupleId) {
        return comicStripsRepo.findAllByCouple_IdOrderByCreatedAtDesc(coupleId);
    }

    @Transactional
    public void deleteComicStrips(Long comicId, Long coupleId) {
        ComicStripsEntity comic = comicStripsRepo.findById(comicId)
                .orElseThrow(() -> new EntityNotFoundException("해당 만화를 찾을 수 없습니다."));

        if (!comic.getCouple().getId().equals(coupleId)) {
            throw new AccessDeniedException("해당 만화에 대한 삭제 권한이 없습니다.");
        }

        // S3 키 추출
        String imageUrl = comic.getImageUrl();
        if (imageUrl != null && !imageUrl.isBlank()) {
            String key = imagenService.extractKeyFromUrl(imageUrl);
            imagenService.deleteFromS3(key); // 이미지 삭제
        }

        comicStripsRepo.deleteById(comicId);
    }

}
