package com.asusoftware.image_api.service;

import com.asusoftware.image_api.exception.ImageDeletionException;
import com.asusoftware.image_api.exception.ImageUploadException;
import com.asusoftware.image_api.exception.ResourceNotFoundException;
import com.asusoftware.image_api.model.Image;
import com.asusoftware.image_api.model.dto.ImageDto;
import com.asusoftware.image_api.repository.ImageRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ImageService {

    @Value("${upload.dir}")
    private String uploadDir;  // Directory di caricamento presa dal file application.yml

    @Value("${external-link.url}")
    private String externalLinkUrl;  // URL base per le immagini preso dal file application.yml

    @Autowired
    private ImageRepository imageRepository;

    @Transactional
    public List<ImageDto> uploadImages(List<MultipartFile> images, UUID postId) {
        // Directorul pentru imaginile postării
        Path postImagesDir = Paths.get(uploadDir, "posts", postId.toString()).toAbsolutePath().normalize();

        List<ImageDto> uploadedImageDtos = new ArrayList<>();

        try {
            for (MultipartFile image : images) {
                String originalFilename = image.getOriginalFilename();
                if (originalFilename == null) {
                    throw new ImageUploadException("Invalid file name.");
                }

                // Sanitizarea numelui fișierului și adăugarea unui UUID pentru unicitate
                String sanitizedFilename = originalFilename.replaceAll("\\s+", "_");
                String uniqueFilename = UUID.randomUUID() + "_" + sanitizedFilename;

                // Creează directorul dacă nu există
                Files.createDirectories(postImagesDir);

                // Salvează fișierul pe server
                Path filePath = postImagesDir.resolve(uniqueFilename);
                Files.copy(image.getInputStream(), filePath);

                // Construiește URL-ul public pentru imagine
                String fileUrl = externalLinkUrl + postId + '/' + uniqueFilename;

                // Salvează informațiile imaginii în baza de date
                Image savedImage = new Image();
                savedImage.setFileName(uniqueFilename);
                savedImage.setFilePath(filePath.toString());
                savedImage.setFileUrl(fileUrl);
                savedImage.setPostId(postId);

                Image saved = imageRepository.save(savedImage);

                // Construiește DTO-ul imaginii
                ImageDto imageDto = new ImageDto();
                imageDto.setId(saved.getId());
                imageDto.setFileName(saved.getFileName());
                imageDto.setFileUrl(constructImageUrlsForPost(postId).get(0));  // Returnăm primul URL generat

                uploadedImageDtos.add(imageDto);
            }

        } catch (IOException e) {
            throw new ImageUploadException("Error uploading images for post ID: " + postId, e);
        }

        return uploadedImageDtos;
    }

    @Transactional
    public void deleteImagesByPostId(UUID postId) {
        List<Image> images = imageRepository.findByPostId(postId);

        for (Image image : images) {
            try {
                Path filePath = Paths.get(image.getFilePath());

                // Elimina il file dal server
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }

                // Elimina il record dal database
                imageRepository.delete(image);
            } catch (IOException e) {
                throw new ImageDeletionException("Error during the deletion of the image: " + image.getFileName(), e);
            }
        }
    }

    public List<ImageDto> getImagesByPostId(UUID postId) {
        List<Image> images = imageRepository.findByPostId(postId);
        return images.stream().map(ImageDto::toDto).collect(Collectors.toList());
    }

    private List<String> constructImageUrlsForPost(UUID postId) {
        // Construiește URL-ul de bază pentru imagini folosind URL-ul extern din config
        String baseUrl = externalLinkUrl + "posts/";

        // Normalizează calea către directorul care conține imaginile pentru post
        Path postImagesDir = Paths.get(uploadDir, "posts", postId.toString()).toAbsolutePath().normalize();

        List<String> imageUrls = new ArrayList<>();

        try {
            if (Files.exists(postImagesDir)) {
                // Listează toate fișierele din directorul specific postului și construiește URL-ul public pentru fiecare imagine
                Files.list(postImagesDir).forEach(filePath -> {
                    if (Files.isRegularFile(filePath)) {
                        // Construiește un URL public accesibil pentru fiecare imagine
                        String imageUrl = baseUrl + postId + '/' + filePath.getFileName().toString();
                        imageUrls.add(imageUrl);
                    }
                });
            }
        } catch (IOException e) {
            // Gestionare eroare, dacă există probleme la accesarea fișierelor
            System.err.println("Error listing images for post: " + e.getMessage());
        }

        return imageUrls;
    }
}