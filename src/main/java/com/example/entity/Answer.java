package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class Answer extends Post {
    @Id
    private Long answerId;
    private Long questionId;
    private Boolean isAccepted;
    private Instant lastEditDate;
    private Instant lastActivityDate;
    private Integer order_oq;
}
