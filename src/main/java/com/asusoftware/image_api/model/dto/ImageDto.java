package com.asusoftware.image_api.model.dto;

import com.asusoftware.image_api.model.Image;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ImageDto {
    private UUID id;
    private String fileName;
    private String fileUrl;


    public static ImageDto toDto(Image image) {
        ImageDto imageDto = new ImageDto();
        imageDto.setId(image.getId());
        imageDto.setFileName(image.getFileName());
        imageDto.setFileUrl(image.getFileUrl());
        return imageDto;
    }
}
