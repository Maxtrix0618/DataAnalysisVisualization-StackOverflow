package com.example.controller;

import com.example.service.DataCollectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

@RestController
public class DataCollectionController {
    private final DataCollectionService service;
    public DataCollectionController(DataCollectionService service) {
        this.service = service;
    }

    @GetMapping("/collect-data")
    public String collectData(
            @RequestParam(defaultValue = "1") int fromPage,
            @RequestParam(defaultValue = "1") int toPage,
            @RequestParam(defaultValue = "2") int pageSize
    ) {
        long[] nfQAC = service.collectAndStoreData(fromPage, toPage, pageSize);
        String currentTime = Instant.now()
                .atZone(java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return "<html>" +
                "<head><title>Data Collection</title></head>" +
                "<body style='font-family: Arial, sans-serif; text-align: center; margin-top: 50px;'>" +
                "<h1>Data Collection Completed!</h1>" +
                "<p style='font-size: 18px; color: #333;'>You can now close this page.</p>" +
                "<p style='font-size: 18px; color: #555;'>The data collection process has successfully finished at:</p>" +
                "<p style='font-size: 22px; color: #007bff;'><strong>" + currentTime + "</strong></p>" +
                "<hr style='margin: 20px 0;'>" +
                "<p style='font-size: 18px; color: #333;'>Number of items collected:</p>" +
                "<ul style='list-style: none; padding: 0; font-size: 18px; color: #555;'>" +
                "<li><strong>Threads:</strong> " + nfQAC[0] + " [" + nfQAC[3] + "]</li>" +
                "<li><strong>Answers:</strong> " + nfQAC[1] + " [" + nfQAC[4] + "]</li>" +
                "<li><strong>Comments:</strong> " + nfQAC[2] + " [" + nfQAC[5] + "]</li>" +
                "</ul>" +
                "</body>" +
                "</html>";
    }
}
