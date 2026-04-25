package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.entity.Client;
import org.misha.authservice.entity.ClientImage;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.ClientImageRepository;
import org.misha.authservice.repository.ClientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientImageService {

    private final ClientRepository clientRepository;
    private final ClientImageRepository imageRepository;

    @Transactional
    public int uploadImages(Long clientId, List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new AppException("FILES_REQUIRED", "Не переданы файлы", HttpStatus.BAD_REQUEST);
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new AppException("CLIENT_NOT_FOUND", "Клиент не найден", HttpStatus.NOT_FOUND));

        int saved = 0;
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            ClientImage image = ClientImage.builder()
                    .client(client)
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .data(file.getBytes())
                    .build();
            imageRepository.save(image);
            saved++;
        }

        if (saved == 0) {
            throw new AppException("FILES_EMPTY", "Файлы пустые", HttpStatus.BAD_REQUEST);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Long> getImageIds(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new AppException("CLIENT_NOT_FOUND", "Клиент не найден", HttpStatus.NOT_FOUND);
        }
        return imageRepository.findByClientId(clientId)
                .stream()
                .map(ClientImage::getId)
                .toList();
    }

    @Transactional
    public void deleteImage(Long imageId) {
        ClientImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new AppException("IMAGE_NOT_FOUND", "Изображение не найдено", HttpStatus.NOT_FOUND));
        imageRepository.delete(image);
    }

    @Transactional(readOnly = true)
    public ClientImage getImage(Long imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new AppException("IMAGE_NOT_FOUND", "Изображение не найдено", HttpStatus.NOT_FOUND));
    }
}

