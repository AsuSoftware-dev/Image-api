package com.asusoftware.image_api.repository;

import com.asusoftware.image_api.model.Image;
import com.asusoftware.image_api.model.ImageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImageRepository extends JpaRepository<Image, UUID> {
    // Find images by a list of IDs
    List<Image> findByIdIn(List<UUID> imageIds);

    Optional<Image> findByFileName(String filename);

    // Găsește toate imaginile pentru un anumit owner (post sau user) și un tip specific de imagine (POST sau USER)
    List<Image> findByOwnerIdAndType(UUID ownerId, ImageType type);

    // Șterge o imagine din baza de date după numele fișierului
    void deleteByFileName(String fileName);
}
