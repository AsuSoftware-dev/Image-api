package com.asusoftware.image_api.controller;

import com.asusoftware.image_api.model.Image;
import com.asusoftware.image_api.model.ImageType;
import com.asusoftware.image_api.model.dto.ImageDto;
import com.asusoftware.image_api.model.dto.UpdateImagesRequest;
import com.asusoftware.image_api.service.ImageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/images")
public class ImageController {

    @Autowired
    private ImageService imageService;

    // Endpoint pentru încărcarea imaginilor
    @PostMapping
    public ResponseEntity<List<ImageDto>> uploadImages(
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam("ownerId") UUID ownerId,
            @RequestParam("type") ImageType type) {
        List<ImageDto> uploadedImages = imageService.uploadImages(images, ownerId, type);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadedImages);
    }

    // Endpoint pentru actualizarea imaginilor (PUT)
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ImageDto>> updateImages(
            @RequestPart("data") UpdateImagesRequest updateImagesRequest,  // DTO trimis ca JSON
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages) {

        List<ImageDto> updatedImages = imageService.updateImagesByOwnerId(
                updateImagesRequest.getOwnerId(),
                updateImagesRequest.getExistingImages(),
                newImages,
                updateImagesRequest.getType()
        );

        return ResponseEntity.ok(updatedImages);
    }

    // Endpoint pentru obținerea tuturor imaginilor pentru un anumit owner (post sau user)
    @GetMapping
    public ResponseEntity<List<ImageDto>> getImagesByOwnerId(
            @RequestParam("ownerId") UUID ownerId,
            @RequestParam("type") ImageType type) {
        List<ImageDto> images = imageService.getImagesByOwnerId(ownerId, type);
        return ResponseEntity.ok(images);
    }

    // Endpoint pentru ștergerea unei imagini după numele fișierului
    @DeleteMapping("/{filename}/{ownerId}/{type}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable String filename,
            @PathVariable UUID ownerId,
            @PathVariable ImageType type) {
        String folder = (type == ImageType.POST) ? "posts" : "users";
        imageService.deleteImage(filename, folder, ownerId);
        return ResponseEntity.noContent().build();
    }

    // Endpoint pentru ștergerea tuturor imaginilor și folderului asociat cu ownerId (post/user)
    @DeleteMapping("/all/{ownerId}/{type}")
    public ResponseEntity<Void> deleteAllImages(
            @PathVariable UUID ownerId,
            @PathVariable ImageType type) {
        String folder = (type == ImageType.POST) ? "posts" : "users";
        imageService.deleteAllImagesAndFolder(ownerId, folder);
        return ResponseEntity.noContent().build();
    }
}

