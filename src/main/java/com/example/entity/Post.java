package com.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

import java.time.Instant;

@Data
@MappedSuperclass
public abstract class Post {
    @Column(length = 1000000)
    private String body;
    private Integer score;
    private Long ownerUserId;
    private String ownerDisplayName;
    private Integer ownerReputation;
    private Integer ownerAcceptRate;
    private Instant creationDate;
}