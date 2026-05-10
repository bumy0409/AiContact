package com.aicontact.backend.aiChild.service;

import com.aicontact.backend.aiChild.dto.AiChildImage;
import com.aicontact.backend.global.storage.S3StorageService;
import jakarta.transaction.Transactional;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AiChildImagenService {

    @Value("${GMS_KEY}")
    private String OPENAI_API_KEY;

    @Value("${DALLE_ENDPOINT}")
    private String ENDPOINT;

    @Autowired
    private S3StorageService s3StorageService;

    public AiChildImage generateImage(String attributes) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        String prompt = "Create an ultra-cute Apple Memoji-style 3D baby character sitting pose facing directly forward toward the camera with a gentle smile. Generate the child of two people with "
                + attributes
                + "incorporating these parental traits subtly. The baby has large expressive brown eyes, chubby round face, short straight smooth black hair (not curly or wrinkled), and East Asian features with fair porcelain-white baby skin. Baby proportions with big head and chubby limbs. Clean 3D cartoon style with beautiful unified pastel gradient background in soft pink and blue tones, not split in half.";

        JSONObject payload = new JSONObject()
                .put("model", "dall-e-3")
                .put("prompt", prompt)
                .put("n", 1)
                .put("size", "1024x1024")
                .put("response_format", "b64_json");

        Request request = new Request.Builder()
                .url(ENDPOINT)
                .header("Authorization", OPENAI_API_KEY)
                .post(RequestBody.create(payload.toString(), MediaType.get("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String err = response.body() != null
                        ? response.body().string()
                        : "(empty)";
                throw new IOException("Image API failed: "
                        + response.code() + " / " + err);
            }

            JSONObject resJson = new JSONObject(response.body().string());
            String base64 = resJson.getJSONArray("data").getJSONObject(0).getString("b64_json");

            byte[] imageBytes = Base64.getDecoder().decode(base64);
            return new AiChildImage(imageBytes, "image/png");
        }
    }

    public AiChildImage generateImageForGrowth(String attributes) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        String prompt = "Create an ultra-cute 7-8 year old child character (maintain original gender - do not change from boy to girl or girl to boy) in strong Apple Memoji style with simplified cartoon features, sitting pose facing directly forward toward the camera with a bright cheerful smile. Based on the child with "
                + attributes
                + "and maintain the same gender as the original child but keep it very simple and minimal. The child has large expressive round eyes, less chubby but still round face showing growth, short neat hair like Apple emoji style, and East Asian features with fair skin. Age-appropriate Apple Memoji proportions with slightly smaller head-to-body ratio than baby, longer limbs showing growth. Pure Apple iOS emoji aesthetic with smooth simplified 3D rendering and unified pastel gradient background in soft pink and blue tones, not split in half.";

        JSONObject payload = new JSONObject()
                .put("model", "dall-e-3")
                .put("prompt", prompt)
                .put("n", 1)
                .put("size", "1024x1024")
                .put("response_format", "b64_json");

        Request request = new Request.Builder()
                .url(ENDPOINT)
                .header("Authorization", OPENAI_API_KEY)
                .post(RequestBody.create(payload.toString(), MediaType.get("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String err = response.body() != null
                        ? response.body().string()
                        : "(empty)";
                throw new IOException("Image API failed: "
                        + response.code() + " / " + err);
            }

            JSONObject resJson = new JSONObject(response.body().string());
            String base64 = resJson.getJSONArray("data").getJSONObject(0).getString("b64_json");

            byte[] imageBytes = Base64.getDecoder().decode(base64);
            return new AiChildImage(imageBytes, "image/png");
        }
    }

    // 3) 디코딩된 바이트와 mimeType을 S3에 업로드하고 DB에 저장
    @Transactional
    public String uploadAiChildImageToS3(
            String attributes,
            Long coupleId) throws IOException {
        // 이미지 생성 + 디코딩
        AiChildImage child = generateImage(attributes);
        byte[] imageBytes = child.getData();
        String contentType = child.getMimeType();

        // 확장자 결정
        String extension = switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            case "image/png" -> "png";
            default -> "bin";
        };

        // S3 키, 업로드
        String uuid = UUID.randomUUID().toString();
        String key = String.format("media/couple/%d/%s.%s",
                coupleId, uuid, extension);

        return s3StorageService.upload(imageBytes, key, contentType);
    }

    public String uploadAiChildImageToS3ForGrowth(String attributes, Long coupleId) throws IOException {

        // 이미지 생성 + 디코딩
        AiChildImage child = generateImageForGrowth(attributes);
        byte[] imageBytes = child.getData();
        String contentType = child.getMimeType();

        // 확장자 결정
        String extension = switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            case "image/png" -> "png";
            default -> "bin";
        };

        // S3 키, 업로드
        String uuid = UUID.randomUUID().toString();
        String key = String.format("media/couple/%d/%s.%s",
                coupleId, uuid, extension);

        return s3StorageService.upload(imageBytes, key, contentType);
    }
}
