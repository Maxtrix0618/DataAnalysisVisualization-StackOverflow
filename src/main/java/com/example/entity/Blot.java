package com.example.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

/**
 * Blotter(Scratch-Paper) to record something, help to the progress of the collection.
 */
@Data
@Entity
public class Blot {
    @Id
    private Long Id;
    private String Name;
}
