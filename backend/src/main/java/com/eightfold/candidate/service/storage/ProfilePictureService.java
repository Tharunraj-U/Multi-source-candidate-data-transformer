package com.eightfold.candidate.service.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.eightfold.candidate.domain.enums.SourceType;
import com.eightfold.candidate.domain.model.ParsedCandidateDTO;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfilePictureService {

    private static final List<SourceType> PICTURE_PRIORITY = List.of(
            SourceType.LINKEDIN, SourceType.GITHUB, SourceType.RESUME);

    private final LocalFileStorageService fileStorage;
    private final RestClient restClient = RestClient.create();

    public String resolveBestPicture(UUID candidateId, List<ParsedCandidateDTO> parsedList) throws IOException {
        ParsedCandidateDTO best = parsedList.stream()
                .filter(p -> p.getProfilePictureUrl() != null && !p.getProfilePictureUrl().isBlank())
                .min(Comparator.comparingInt(p -> picturePriority(p.getSourceType())))
                .orElse(null);
        if (best == null) {
            return null;
        }
        return storePicture(candidateId, best.getProfilePictureUrl());
    }

    public String storePicture(UUID candidateId, String pictureRef) throws IOException {
        if (pictureRef == null || pictureRef.isBlank()) {
            return null;
        }
        if (pictureRef.startsWith("http://") || pictureRef.startsWith("https://")) {
            byte[] bytes = restClient.get()
                    .uri(pictureRef)
                    .retrieve()
                    .body(byte[].class);
            if (bytes == null || bytes.length == 0) {
                throw new IOException("Empty picture response from URL");
            }
            String ext = pictureRef.contains(".png") ? "avatar.png" : "avatar.jpg";
            return fileStorage.store(candidateId, "picture", ext, new ByteArrayInputStream(bytes));
        }
        return pictureRef;
    }

    public InputStream openPicture(String picturePath) throws IOException {
        if (picturePath == null) {
            throw new IOException("Picture path is missing");
        }
        if (picturePath.startsWith("http://") || picturePath.startsWith("https://")) {
            byte[] bytes = restClient.get()
                    .uri(picturePath)
                    .retrieve()
                    .body(byte[].class);
            if (bytes == null) {
                throw new IOException("Failed to fetch remote picture");
            }
            return new ByteArrayInputStream(bytes);
        }
        return fileStorage.retrieve(picturePath);
    }

    private int picturePriority(SourceType type) {
        int idx = PICTURE_PRIORITY.indexOf(type);
        return idx < 0 ? PICTURE_PRIORITY.size() : idx;
    }
}
