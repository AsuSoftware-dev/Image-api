package com.asusoftware.image_api.model.dto;

import com.asusoftware.image_api.model.ImageType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UpdateImagesRequest {
    private UUID ownerId;
    private ImageType type;
    private List<ImageDto> existingImages;
}
