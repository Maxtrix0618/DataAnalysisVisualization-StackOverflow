package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class Comment extends Post {
    @Id
    private Long commentId;
    private Long postId;
    /**
     * 0 for Question, 1 for Answer
     */
    private Integer postTo;
}
