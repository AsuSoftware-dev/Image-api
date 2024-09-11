package com.asusoftware.image_api.controller;

import com.asusoftware.image_api.model.Image;
import com.asusoftware.image_api.model.dto.ImageDto;
import com.asusoftware.image_api.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    public ResponseEntity<List<ImageDto>> uploadImages(@RequestParam("images") List<MultipartFile> images, @RequestParam("postId") UUID postId) {
        List<ImageDto> uploadedImages = imageService.uploadImages(images, postId);
        return new ResponseEntity<>(uploadedImages, HttpStatus.CREATED);
    }

    @DeleteMapping("/deleteByPostId")
    public ResponseEntity<Void> deleteImagesByPostId(@RequestParam UUID postId) {
        imageService.deleteImagesByPostId(postId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ImageDto>> getImagesByPostId(@RequestParam UUID postId) {
        List<ImageDto> images = imageService.getImagesByPostId(postId);
        return ResponseEntity.ok(images);
    }
}

