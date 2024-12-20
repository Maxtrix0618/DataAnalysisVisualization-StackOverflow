package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class Question extends Post {
    @Id
    private Long questionId;
    private String title;
    private String tags;
    private Integer viewCount;
    private Integer answerCount;
    private Boolean isAnswered;
    private Long acceptedAnswerId;
    private Instant lastEditDate;
    private Instant lastActivityDate;
}