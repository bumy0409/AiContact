package com.aicontact.backend.aiChild.service;

import com.aicontact.backend.aiChild.entity.AiChildEntity;
import com.aicontact.backend.aiChild.repository.AiChildRepository;
import com.aicontact.backend.couple.entity.CoupleEntity;
import com.aicontact.backend.couple.repository.CoupleRepository;
import com.aicontact.backend.global.service.GptScenarioService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Transactional
@RequiredArgsConstructor
public class AiChildService {

    private final AiChildRepository childRepo;
    private final CoupleRepository coupleRepo;
    private final GptScenarioService gptScenarioService;
    private final AiChildImagenService imagenService;

    public AiChildEntity getMyChild(Long coupleId) {
        return childRepo.findByCoupleId(coupleId)
                .orElseThrow(() -> new EntityNotFoundException("자녀 정보가 없습니다. coupleId=" + coupleId));
    }

    @Transactional
    public AiChildEntity createChild(Long coupleId, String name) throws IOException {
        CoupleEntity couple = coupleRepo.findById(coupleId)
                .orElseThrow(() -> new EntityNotFoundException("Couple not found: " + coupleId));
        AiChildEntity child = new AiChildEntity();
        child.setCouple(couple);
        child.setName(name);

        // 1. 사진 외모 특성 추출하기
        String url1 = couple.getUser1().getProfileImageUrl();
        String url2 = couple.getUser2().getProfileImageUrl();

        String attributes = gptScenarioService.getAppearanceAttributes(url1, url2);
        String imageUrl = imagenService.uploadAiChildImageToS3(attributes, coupleId);
        child.setImageUrl(imageUrl);
        return childRepo.save(child);
    }

    public AiChildEntity createChildForCouple(CoupleEntity couple) throws IOException {
        AiChildEntity child = new AiChildEntity();
        child.setCouple(couple);

        // 1. 사진 외모 특성 추출하기
        String url1 = couple.getUser1().getProfileImageUrl();
        String url2 = couple.getUser2().getProfileImageUrl();

        String attributes = gptScenarioService.getAppearanceAttributes(url1, url2);
        String imageUrl = imagenService.uploadAiChildImageToS3(attributes, couple.getId());
        child.setImageUrl(imageUrl);
        return childRepo.save(child);
    }

    public AiChildEntity getChild(Long id) {
        return childRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AiChild not found: " + id));
    }

    @Transactional
    public AiChildEntity updateChild(Long id, String name, String imageUrl, Integer growthLevel,
            Integer experiencePoints) {
        AiChildEntity child = childRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AiChild not found: " + id));
        if (name != null)
            child.setName(name);
        if (imageUrl != null)
            child.setImageUrl(imageUrl);
        if (growthLevel != null)
            child.setGrowthLevel(growthLevel);
        if (experiencePoints != null)
            child.setExperiencePoints(experiencePoints);
        return childRepo.save(child);
    }

    @Transactional
    public AiChildEntity updateChildPoints(Long id, Integer experiencePoints) {
        AiChildEntity child = childRepo.findByCoupleId(id)
                .orElseThrow(() -> new EntityNotFoundException("AiChild not found: " + id));
        int current = child.getExperiencePoints();
        if (experiencePoints != null)
            child.setExperiencePoints(current + experiencePoints);
        return childRepo.save(child);
    }

    @Transactional
    public void deleteChild(Long id) {
        if (!childRepo.existsById(id)) {
            throw new EntityNotFoundException("AiChild not found: " + id);
        }
        childRepo.deleteById(id);
    }

    @Transactional
    public AiChildEntity regenerateImage(Long id) throws IOException {
        AiChildEntity child = childRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AiChild not found: " + id));

        CoupleEntity couple = child.getCouple();
        String url1 = couple.getUser1().getProfileImageUrl();
        String url2 = couple.getUser2().getProfileImageUrl();

        String attributes = gptScenarioService.getAppearanceAttributes(url1, url2);
        String imageUrl = imagenService.uploadAiChildImageToS3(attributes, couple.getId());
        child.setImageUrl(imageUrl);
        return childRepo.save(child);
    }

    @Transactional
    public AiChildEntity growChild(Long id) throws IOException {
        AiChildEntity child = childRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AiChild not found: " + id));

        child.setGrowthLevel(child.getGrowthLevel() + 1);
        
        // 1. 사진 외모 특성 추출하기
        String url = child.getImageUrl();

        String attributes = gptScenarioService.getAppearanceAttributesForGrowth(url);
        String imageUrl = imagenService.uploadAiChildImageToS3ForGrowth(attributes, child.getCouple().getId());
        child.setImageUrl(imageUrl);
        return childRepo.save(child);
    }
}
