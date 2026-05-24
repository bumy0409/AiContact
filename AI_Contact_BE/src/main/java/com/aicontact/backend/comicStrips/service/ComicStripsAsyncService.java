package com.aicontact.backend.comicStrips.service;

import com.aicontact.backend.comicStrips.entity.ComicStripsEntity;
import com.aicontact.backend.comicStrips.repository.ComicStripsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComicStripsAsyncService {

    private final ComicStripsRepository comicStripsRepo;
    private final ComicStripsImagenService imagenService;

    // id -> "PROCESSING" | "FAILED"
    private final Map<Long, String> taskStatus = new ConcurrentHashMap<>();

    public void markProcessing(Long id) {
        taskStatus.put(id, "PROCESSING");
    }

    @Async
    @Transactional
    public void generateImageAsync(Long comicStripsId, String location, String activity, String weather, Long coupleId) {
        try {
            String imageUrl = imagenService.uploadComicStripsImageToS3(location, activity, weather, coupleId);
            ComicStripsEntity entity = comicStripsRepo.findById(comicStripsId)
                    .orElseThrow(() -> new EntityNotFoundException("ComicStrips not found: " + comicStripsId));
            entity.setImageUrl(imageUrl);
            comicStripsRepo.save(entity);
            taskStatus.remove(comicStripsId);
            log.info("만화 생성 완료: id={}", comicStripsId);
        } catch (Exception e) {
            log.error("만화 생성 실패: id={}", comicStripsId, e);
            taskStatus.put(comicStripsId, "FAILED");
        }
    }

    public Map<String, Object> getStatus(Long id) {
        String status = taskStatus.get(id);

        if ("FAILED".equals(status)) {
            return Map.of("status", "FAILED");
        }
        if ("PROCESSING".equals(status)) {
            return Map.of("status", "PROCESSING");
        }

        // taskStatus에 없으면 완료된 것
        ComicStripsEntity entity = comicStripsRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ComicStrips not found: " + id));

        if (entity.getImageUrl() != null) {
            return Map.of(
                    "status", "DONE",
                    "imageUrl", entity.getImageUrl(),
                    "title", entity.getTitle() != null ? entity.getTitle() : ""
            );
        }

        return Map.of("status", "PROCESSING");
    }
}
