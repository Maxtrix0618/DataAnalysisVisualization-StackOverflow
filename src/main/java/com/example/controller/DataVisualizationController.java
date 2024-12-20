package com.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.service.DataAnalysisService;

@Controller
public class DataVisualizationController {
    private final DataAnalysisService service;
    public DataVisualizationController(DataAnalysisService dataAnalysisService) {
        this.service = dataAnalysisService;
    }

    @GetMapping("/do-test")
    public String doTest() {return "test";}


    /**
     * Terminal Panel
     */
    @GetMapping("/panel")
    public String panelPage(Model model) {
        service.getDataSize().forEach(model::addAttribute);
        return "panel";
    }

    /**
     * 1. Java Topics
     */
    @GetMapping("/fa-topics")
    public String getFQTopics(
            @RequestParam(defaultValue = "16") int topN,
            Model model) {
        model.addAttribute("tagFreTop", service.getTopTopicsAndDistribution(topN));
        return "fa-topics";
    }

    /**
     * 2. User Engagement
     */
    @GetMapping("/hrue-topics")
    public String getHURETopics(
            @RequestParam(defaultValue = "24") int topN,
            @RequestParam(defaultValue = "2000") int reputationThreshold,
            Model model) {
        model.addAttribute("engagedTopics", service.getTopEngagedTopics(topN, reputationThreshold));
        model.addAttribute("reputationThreshold", reputationThreshold);
        return "hrue-topics";
    }

    /**
     * 3. Common Mistakes
     */
    @GetMapping("/fd-mistakes")
    public String getFDMistakes(
            @RequestParam(defaultValue = "16") int topN,
            Model model) {
        service.getTopMistakes(topN).forEach(model::addAttribute);
        return "fd-mistakes";
    }

    /**
     * 4. Answer Quality
     */
    @GetMapping("/analysis-highQualityAnswer")
    public String getAnalysisHQA(
            @RequestParam(defaultValue = "80000") int scoreLimit,
            @RequestParam(defaultValue = "1000000") int timeIntervalLimit,
            @RequestParam(defaultValue = "200000") int reputationLimit,
            @RequestParam(defaultValue = "50000") int answerLengthLimit,
            @RequestParam(defaultValue = "64") int orderLimit,
            Model model) {
        int[] limits = new int[]{scoreLimit, timeIntervalLimit, reputationLimit, answerLengthLimit, orderLimit};
        service.getAnswerQualityAnalysisBooklet(limits).forEach(model::addAttribute);
        return "analysis-hqa";
    }

}
