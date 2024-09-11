package com.asusoftware.image_api.repository;

import com.asusoftware.image_api.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImageRepository extends JpaRepository<Image, UUID> {
    // Find all images by the post ID
    List<Image> findByPostId(UUID postId);

    // Find images by a list of IDs
    List<Image> findByIdIn(List<UUID> imageIds);
}
