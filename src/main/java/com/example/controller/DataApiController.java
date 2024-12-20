package com.example.controller;

import com.example.service.DataAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DataApiController {
    private static final String PRODUCES = "application/json";
    private final DataAnalysisService service;
    public DataApiController(DataAnalysisService service) {
        this.service = service;
    }

    /**
     * 1a. Top-N Frequent Topics
     */
    @GetMapping(value="/api/topics-frequency", produces=PRODUCES)
    public Map<String, Long> getTopicsFrequency(
            @RequestParam(defaultValue = "24") int topN
    ) {return service.getTopTagFrequency(topN);}
    /**
     * 1b. Frequency of specific Topic
     */
    @GetMapping(value="/api/topics-frequency/{topic}", produces=PRODUCES)
    public Map<String, Long> getTopicFrequencyByName(
            @PathVariable String topic,
            @RequestParam(defaultValue = "false") boolean ignoreCase
    ) {return service.getTopicFrequencyByName(topic, ignoreCase);}

    /**
     * 2a. Top-N Frequent Bugs
     */
    @GetMapping(value="/api/bugs-frequency/{type}", produces=PRODUCES)
    public Map<String, Map<String, Long>> getBugsFrequency(
            @PathVariable String type,
            @RequestParam(defaultValue = "16") int topN
    ) {return service.getTopBugFrequency(type, topN);}
    /**
     * 2b. Frequency of specific Bug
     */
    @GetMapping(value="/api/bugs-frequency/{type}/{bug}", produces=PRODUCES)
    public Map<String, Map<String, Long>> getBugFrequencyByName(
            @PathVariable String type,
            @PathVariable String bug,
            @RequestParam(defaultValue = "false") boolean ignoreCase
    ) {return service.getBugFrequencyByName(type, bug, ignoreCase);}


}
