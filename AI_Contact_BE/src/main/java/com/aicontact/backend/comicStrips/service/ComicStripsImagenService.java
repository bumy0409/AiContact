package com.aicontact.backend.comicStrips.service;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aicontact.backend.global.storage.S3StorageService;

import jakarta.transaction.Transactional;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


@Service
public class ComicStripsImagenService {

    @Value("${GMS_KEY}")
    private String OPENAI_API_KEY;

    @Value("${DALLE_ENDPOINT}")
    private String ENDPOINT;

    @Autowired
    private S3StorageService s3StorageService;

    public byte[] generateImage(String location, String activity, String weather) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        String prompt = """
                Create one square image (1:1 aspect ratio) that contains exactly four equal-sized square visual scenes arranged in a clean 2x2 grid layout.

                Scene context:
                Location: %s
                Main activity: %s
                Weather/Season: %s

                Instructions:
                - The image must consist of four completely separate romantic scenes featuring the same young Korean couple in their 20s.
                - Each scene should show a different visual moment based on the given activity and setting — but without any indication of time passing.
                - Do not show time of day, sun position, lighting changes, or anything that suggests a time sequence.
                - The couple must look consistent in every scene — same outfits, same facial features, same environment and lighting.
                - Each scene must express a unique mood or action visually (e.g. talking, smiling, walking, sitting, eating) without repeating the same pose.

                ⚠ STRICTLY FORBIDDEN:
                - Do NOT include any text, words, letters, numbers, timestamps, clock elements, or writing of any kind.
                - Do NOT show any time indicators such as sunlight angles, sunsets, night skies, or shadows that imply different times of day.
                - Do NOT use speech bubbles, labels, UI, or any comic-style overlays.

                Style and Layout:
                - The full image must be a 1:1 square, composed of exactly four square scenes, arranged in a 2x2 grid.
                - Each scene should be vivid, expressive, and purely visual.
                - Art style: soft anime or clean romantic cartoon.
                - Use consistent seasonal and weather-appropriate backgrounds and colors.
                - The result must look like four independent but connected visual moments — with no sense of time progression and no text.
                """
                .formatted(location, activity, weather);

        JSONObject json = new JSONObject()
                .put("model", "dall-e-3")
                .put("prompt", prompt)
                .put("n", 1)
                .put("size", "1024x1024");

        Request request = new Request.Builder()
                .url(ENDPOINT)
                .header("Authorization", OPENAI_API_KEY)
                .post(RequestBody.create(json.toString(), MediaType.get("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "(empty)";
                throw new IOException("GMS image API failed: " + response + "\n" + errorBody);
            }

            JSONObject resJson = new JSONObject(response.body().string());
            String imageUrl = resJson.getJSONArray("data").getJSONObject(0).getString("url");

            Request imageRequest = new Request.Builder().url(imageUrl).build();
            try (Response imageResponse = client.newCall(imageRequest).execute()) {
                if (!imageResponse.isSuccessful() || imageResponse.body() == null) {
                    throw new IOException("이미지 다운로드 실패: " + imageUrl);
                }
                return imageResponse.body().bytes();
            }
        }

    }

    @Transactional
    public String uploadComicStripsImageToS3(
            String location,
            String activity,
            String weather,
            Long coupleId) throws IOException {

        byte[] imageBytes = generateImage(location, activity, weather);
        String uuid = UUID.randomUUID().toString();
        String key = String.format("media/couple/%d/%s.png", coupleId, uuid);
        return s3StorageService.upload(imageBytes, key, "image/png");
    }

    @Transactional
    public void deleteFromS3(String key) {
        s3StorageService.delete(key);
    }

    // S3 URL로부터 키 추출하는 메서드
    public String extractKeyFromUrl(String url) {
        // 예: https://your-bucket.s3.amazonaws.com/media/couple/3/abcd-1234.jpg
        // → media/couple/3/abcd-1234.jpg
        int idx = url.indexOf("media/");
        if (idx == -1) {
            throw new IllegalArgumentException("S3 키를 추출할 수 없습니다: " + url);
        }
        return url.substring(idx);
    }

}
