package com.asusoftware.image_api.service;

import com.asusoftware.image_api.exception.ImageDeletionException;
import com.asusoftware.image_api.exception.ImageUploadException;
import com.asusoftware.image_api.model.Image;
import com.asusoftware.image_api.model.ImageType;
import com.asusoftware.image_api.model.dto.ImageDto;
import com.asusoftware.image_api.repository.ImageRepository;
import jakarta.transaction.Transactional;
import org.antlr.v4.runtime.misc.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImageService {

    @Value("${upload.dir}")
    private String uploadDir;  // Directory di caricamento presa dal file application.yml

    @Value("${external-link.url}")
    private String externalLinkUrl;  // URL base per le immagini preso dal file application.yml

    @Autowired
    private ImageRepository imageRepository;

    /**
     * Metodă pentru încărcarea de imagini noi pentru un owner (poate fi fie post, fie user).
     */
    @Transactional
    public List<ImageDto> uploadImages(List<MultipartFile> images, UUID ownerId, ImageType type) {
        String folder = type == ImageType.POST ? "posts" : "users";  // Folosim folderul potrivit
        Path ownerImagesDir = Paths.get(uploadDir, folder, ownerId.toString()).toAbsolutePath().normalize();

        List<ImageDto> uploadedImageDtos;

        try {
            Files.createDirectories(ownerImagesDir);  // Creează directorul dacă nu există

            for (MultipartFile image : images) {
                String originalFilename = image.getOriginalFilename();
                if (originalFilename == null) {
                    throw new ImageUploadException("Invalid file name.");
                }

                // Sanitizăm numele fișierului și adăugăm un UUID pentru unicitate
                String sanitizedFilename = originalFilename.replaceAll("\\s+", "_");
                String uniqueFilename = UUID.randomUUID() + "_" + sanitizedFilename;

                // Salvăm fișierul pe server
                Path filePath = ownerImagesDir.resolve(uniqueFilename);
                Files.copy(image.getInputStream(), filePath);

                // Construim URL-ul public
                String fileUrl = externalLinkUrl + folder + '/' + ownerId + '/' + uniqueFilename;

                // Salvăm imaginea în baza de date
                Image savedImage = new Image();
                savedImage.setFileName(uniqueFilename);
                savedImage.setFilePath(filePath.toString());
                savedImage.setFileUrl(fileUrl);
                savedImage.setOwnerId(ownerId);
                savedImage.setType(type);

                imageRepository.save(savedImage);
            }

            // Returnăm DTO-urile pentru toate imaginile încărcate
            uploadedImageDtos = constructImageDtosForOwner(ownerId, type);

        } catch (IOException e) {
            throw new ImageUploadException("Error uploading images for owner ID: " + ownerId, e);
        }

        return uploadedImageDtos;
    }

    /**
     * Metodă pentru actualizarea imaginilor (ștergerea celor vechi și adăugarea celor noi).
     */
    @Transactional
    public List<ImageDto> updateImagesByOwnerId(UUID ownerId, List<ImageDto> existingImages, List<MultipartFile> newImages, ImageType type) {
        String folder = type == ImageType.POST ? "posts" : "users";
        Path ownerImagesDir = Paths.get(uploadDir, folder, ownerId.toString()).toAbsolutePath().normalize();

        List<Image> updatedImages = new ArrayList<>();

        // 1. Verificăm imaginile existente și ștergem cele care nu mai sunt prezente în request
        List<Image> imagesInDb = imageRepository.findByOwnerIdAndType(ownerId, type);
        Set<UUID> existingImageIds = existingImages.stream()
                .map(ImageDto::getId)
                .collect(Collectors.toSet());

        for (Image imageInDb : imagesInDb) {
            if (!existingImageIds.contains(imageInDb.getId())) {
                // Imaginea nu mai este în request, o ștergem din DB și de pe server
                deleteImage(imageInDb.getFileName(), folder, ownerId);
            } else {
                // Imaginea există, o păstrăm în lista actualizată
                updatedImages.add(imageInDb);
            }
        }

        // 2. Adăugăm noile imagini primite în request
        if (newImages != null && !newImages.isEmpty()) {
            try {
                Files.createDirectories(ownerImagesDir);  // Creăm directorul dacă nu există

                for (MultipartFile newImage : newImages) {
                    String originalFilename = newImage.getOriginalFilename();
                    if (originalFilename == null || !originalFilename.contains(".")) {
                        throw new ImageUploadException("Invalid file name or no extension found.");
                    }

                    // Extragem extensia fișierului
                    String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                    String sanitizedFilename = originalFilename.replaceAll("\\s+", "_").replace(fileExtension, "");

                    // Adăugăm un UUID pentru unicitate și păstrăm extensia fișierului
                    String uniqueFilename = UUID.randomUUID() + "_" + sanitizedFilename + fileExtension;

                    // Salvăm fișierul pe server
                    Path filePath = ownerImagesDir.resolve(uniqueFilename);
                    Files.copy(newImage.getInputStream(), filePath);

                    // Construim URL-ul public
                    String fileUrl = externalLinkUrl + folder + '/' + ownerId + '/' + uniqueFilename;

                    // Salvăm imaginea în baza de date
                    Image savedImage = new Image();
                    savedImage.setFileName(uniqueFilename); // Include extensia în filename
                    savedImage.setFilePath(filePath.toString());
                    savedImage.setFileUrl(fileUrl);
                    savedImage.setOwnerId(ownerId);
                    savedImage.setType(type);

                    updatedImages.add(imageRepository.save(savedImage));
                }
            } catch (IOException e) {
                throw new ImageUploadException("Error uploading new images for owner ID: " + ownerId, e);
            }
        }

        // 3. Returnăm DTO-urile actualizate pentru toate imaginile
        return constructImageDtosForOwner(ownerId, type);
    }

    /**
     * Metodă pentru ștergerea unei imagini din server și din baza de date.
     */
    @Transactional
    public void deleteImage(String filename, String folder, @NotNull UUID ownerId) {
        // Elimina imaginea din baza de date și de pe server
        Path ownerImagesDir = Paths.get(uploadDir, folder, ownerId.toString()).toAbsolutePath().normalize();
        Path filePath = ownerImagesDir.resolve(filename);

        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath); // Șterge fișierul de pe server
            } else {
                throw new FileNotFoundException("Fișierul nu a fost găsit pe server: " + filename);
            }

            // Șterge imaginea din baza de date
            imageRepository.deleteByFileName(filename);
        } catch (IOException e) {
            throw new ImageDeletionException("Eroare la ștergerea imaginii: " + filename, e);
        }
    }

    @Transactional
    public void deleteAllImagesAndFolder(UUID ownerId, String folder) {
        Path ownerImagesDir = Paths.get(uploadDir, folder, ownerId.toString()).toAbsolutePath().normalize();

        try {
            // 1. Ștergem toate înregistrările din baza de date
            List<Image> images = imageRepository.findByOwnerIdAndType(ownerId, folder.equals("posts") ? ImageType.POST : ImageType.USER);
            for (Image image : images) {
                imageRepository.delete(image); // Șterge din baza de date
            }

            // 2. Ștergem toate fișierele și subfolderul de pe server
            if (Files.exists(ownerImagesDir)) {
                Files.walk(ownerImagesDir)
                        .sorted(Comparator.reverseOrder()) // Sortează în ordine inversă pentru a șterge fișierele înaintea folderelor
                        .forEach(file -> {
                            try {
                                Files.delete(file);
                            } catch (IOException e) {
                                throw new ImageDeletionException("Error deleting file: " + file.toString(), e);
                            }
                        });
            } else {
                throw new FileNotFoundException("Directory not found: " + ownerImagesDir.toString());
            }
        } catch (IOException e) {
            throw new ImageDeletionException("Error deleting all images for owner ID: " + ownerId, e);
        }
    }

    @Transactional
    public List<ImageDto> getImagesByOwnerId(UUID ownerId, ImageType type) {
        List<Image> images = imageRepository.findByOwnerIdAndType(ownerId, type);
        return images.stream().map(ImageDto::toDto).collect(Collectors.toList());
    }

    /**
     * Construiește DTO-urile pentru imagini în funcție de owner (post sau user).
     */
    private List<ImageDto> constructImageDtosForOwner(UUID ownerId, ImageType type) {
        String folder = type == ImageType.POST ? "posts" : "users";
        String baseUrl = externalLinkUrl + folder + '/' + ownerId + '/';

        Path ownerImagesDir = Paths.get(uploadDir, folder, ownerId.toString()).toAbsolutePath().normalize();
        List<ImageDto> imageDtos = new ArrayList<>();

        try {
            if (Files.exists(ownerImagesDir)) {
                Files.list(ownerImagesDir).forEach(filePath -> {
                    if (Files.isRegularFile(filePath)) {
                        String imageUrl = baseUrl + filePath.getFileName().toString();

                        // Retrieve the image entity from the database using the fileName
                        Optional<Image> imageOpt = imageRepository.findByFileName(filePath.getFileName().toString());

                        // If the image is found, create the ImageDto
                        if (imageOpt.isPresent()) {
                            Image image = imageOpt.get();
                            ImageDto imageDto = new ImageDto();
                            imageDto.setId(image.getId());  // Set the ID from the image entity
                            imageDto.setFileName(image.getFileName());
                            imageDto.setFileUrl(imageUrl);
                            imageDtos.add(imageDto);
                        }
                    }
                });
            }
        } catch (IOException e) {
            throw new ImageUploadException("Error constructing image DTOs for owner ID: " + ownerId, e);
        }

        return imageDtos;
    }
}