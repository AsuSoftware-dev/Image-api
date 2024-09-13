package com.asusoftware.image_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
@Getter
@Setter
@Entity
@Table(name = "images")
public class Image {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;  // Acesta poate fi userId sau postId

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ImageType type;  // Tipul imaginii: POST sau USER
}
