package com.example.service;

import com.example.entity.Answer;
import com.example.entity.Comment;
import com.example.entity.Question;
import com.example.repository.AnswerRepository;
import com.example.repository.CommentRepository;
import com.example.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DataAnalysisService {
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final CommentRepository commentRepository;

    public DataAnalysisService(QuestionRepository questionRepository,
                               AnswerRepository answerRepository,
                               CommentRepository commentRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.commentRepository = commentRepository;
    }

    public Map<String, Long> getDataSize() {
        return Map.of(
                QUESTIONS, questionRepository.count(),
                ANSWERS, answerRepository.count(),
                COMMENTS, commentRepository.count()
        );
    }

    /**
     * 1. Java Topics
     */
    public Map<String, Long> getTopTopicsAndDistribution(int topN) {
        return fetchTopTagFrequency(getTagFrequency(), topN);
    }
    private Map<String, Long> getTagFrequency() {
        return questionRepository.findAll().stream()
                .map(Question::getTags)
                .filter(Objects::nonNull)
                .flatMap(s -> Arrays.stream(s.split(",")))
                .filter(t -> !t.equalsIgnoreCase("java"))
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
    }
    /**
     * Limit to top N, and Merge the rest into the element "other"
     */
    private Map<String, Long> fetchTopTagFrequency(Map<String, Long> tagFrequency, int topN) {
        Map<String, Long> topMap = tagFrequency.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(topN).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        topMap.put("(tags:others)", tagFrequency.entrySet().stream()
                .filter(entry -> !topMap.containsKey(entry.getKey()))
                .mapToLong(Map.Entry::getValue)
                .sum());
        return topMap;
    }
    /**
     * Limit to whose count > 1, and Merge the rest into the element "count<2"
     */
    private Map<String, Long> fetchAllTagFrequency(Map<String, Long> tagFrequency) {
        Map<String, Long> allMap = tagFrequency.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        allMap.put("(tags:count<2)", tagFrequency.entrySet().stream()
                .filter(entry -> !allMap.containsKey(entry.getKey()))
                .mapToLong(Map.Entry::getValue)
                .sum());
        return allMap;
    }

    /**
     * 2. User Engagement<br/>
     * tag_engagement = questions_sum + answers_sum + comments_sum
     */
    public Map<String, Map<String, Long>> getTopEngagedTopics(int topN, int reputationThreshold) {
        Map<String, Map<String, Long>> tag_locEng = new HashMap<>();
        questionRepository.findAll().stream()
                .filter(q -> q.getOwnerReputation() != null && q.getOwnerReputation() >= reputationThreshold)
                .forEach(question ->
                        tagsEngUp(tag_locEng, QUESTIONS, question));
        answerRepository.findAll().stream()
                .filter(a -> a.getOwnerReputation() != null && a.getOwnerReputation() >= reputationThreshold)
                .forEach(answer -> questionRepository.findById(answer.getQuestionId()).ifPresent(question ->
                        tagsEngUp(tag_locEng, ANSWERS, question)));
        commentRepository.findAll().stream()
                .filter(c -> c.getOwnerUserId() != null && c.getOwnerReputation() >= reputationThreshold)
                .forEach(comment -> {
                    switch (comment.getPostTo()) {
                        case 0 -> questionRepository.findById(comment.getPostId()).ifPresent(question ->
                                tagsEngUp(tag_locEng, COMMENTS, question));
                        case 1 -> answerRepository.findById(comment.getPostId()).flatMap(answer ->
                                questionRepository.findById(answer.getQuestionId())).ifPresent(question ->
                                tagsEngUp(tag_locEng, COMMENTS, question));
                    }
                });
        return limitByScoreSum(tag_locEng ,topN);
    }
    private static void tagsEngUp(Map<String, Map<String, Long>> tag_locEng, String location, Question question) {
        for (String tag : question.getTags().split(","))
            if (!tag.equalsIgnoreCase("java"))
                scoreUp(tag_locEng, tag, location);
    }
    private static void scoreUp(Map<String, Map<String, Long>> xxx_locScore, String xxx, String location) {
        xxx_locScore.computeIfAbsent(xxx, k -> new HashMap<>(dataTemplate))
                    .merge(location, 1L, Long::sum);
    }
    private Map<String, Map<String, Long>> limitByScoreSum(Map<String, Map<String, Long>> xxx_subScore, int topN) {
        if (topN < 0) return xxx_subScore;
        return xxx_subScore.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Map<String, Long>>comparingByValue(Comparator.comparingLong(
                        map -> map.values().stream().mapToLong(Long::longValue).sum()
                )).reversed()).limit(topN)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    private static final String QUESTIONS = "questions", ANSWERS = "answers", COMMENTS = "comments";
    private static final Map<String, Long> dataTemplate = Map.of(QUESTIONS,0L, ANSWERS,0L, COMMENTS,0L);


    /**
     * 3. Common Mistakes
     */
    public Map<String, Map<String, Map<String, Long>>> getTopMistakes(int topN) {
        Map<String, Map<String, Map<String, Long>>> THE_EE = new HashMap<>();
        THE_EE.put("errorFreTop", getTopEE(topN, "error"));
        THE_EE.put("exceptionFreTop", getTopEE(topN, "exception"));
        return THE_EE;
    }
    private Map<String, Map<String, Long>> getTopEE(int topN, String type) {
        Map<String, List<String>> allBodies = allBodies();
        Map<String, Map<String, Long>> bug_locEng = new HashMap<>();

        Pattern key_pattern = Pattern.compile("(?i)\\b(?:[A-Za-z]+)?"+type+"\\b");
        extractEE(bug_locEng, allBodies, type, key_pattern, QUESTIONS);
        extractEE(bug_locEng, allBodies, type, key_pattern, ANSWERS);
        extractEE(bug_locEng, allBodies, type, key_pattern, COMMENTS);

        return limitByScoreSum(bug_locEng, topN);
    }
    private Map<String, List<String>> allBodies() {
        List<String> questionBodies = questionRepository.findAll().stream()
                .map(q -> q.getTitle()+" "+q.getBody()+" "+q.getTags()).toList();
        List<String> answerBodies = answerRepository.findAll().stream()
                .map(Answer::getBody).filter(Objects::nonNull).toList();
        List<String> commentBodies = commentRepository.findAll().stream()
                .map(Comment::getBody).filter(Objects::nonNull).toList();
        return Map.of(QUESTIONS, questionBodies, ANSWERS, answerBodies, COMMENTS, commentBodies);
    }
    private void extractEE(Map<String, Map<String, Long>> bug_locEng, Map<String, List<String>> allBodies, String type, Pattern key_pattern, String location) {
        Set<String> visited = new HashSet<>();   // Each bug counts only once per body.
        for (String body : allBodies.get(location)) {
            Matcher matcher = key_pattern.matcher(body);
            while (matcher.find()) {
                String key = matcher.group();
                if (!visited.contains(key) && !key.equalsIgnoreCase(type)) {
                    scoreUp(bug_locEng, capFirstLetter(key), location);
                    visited.add(key);
                }
            } visited.clear();
        }
    }
    private static String capFirstLetter(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }


    /**
     * 4. Answer Quality
     */
    public Map<String, List<Map<Number, List<Number>>>> getAnswerQualityAnalysisBooklet(int[] limits) {
        Map<String, List<Map<Number, List<Number>>>> THE_TABLE = new HashMap<>();
        for (int t = 0; t < 2; t++) {
            treatMode = t;
            THE_TABLE.putAll(classifiedData(getAnswersAccepted(true, limits), "_a"));
            THE_TABLE.putAll(classifiedData(getAnswersAccepted(false, limits), "_r"));
        }
        return THE_TABLE;
    }
    private Map<String, List<Map<Number, List<Number>>>> classifiedData(List<Map<String, Number>> ansList, String suffix) {
        return ansList.stream()
                .flatMap(map -> map.entrySet().stream()
                        .filter(ans -> !ans.getKey().equals(ID) && !ans.getKey().equals(SCORE))
                        .map(ans -> {
                            Map<Number, List<Number>> point = new HashMap<>();
                            point.put(map.get(ID), Arrays.asList(ans.getValue(), map.get(SCORE)));
                            return new AbstractMap.SimpleEntry<>(ans.getKey()+suffix+treatMode, point);
                        }))
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }
    private List<Map<String, Number>> getAnswersAccepted(boolean isAccepted, int[] limits) {
        List<Map<String, Number>> answersData = new ArrayList<>();
        List<Answer> answers = ((isAccepted) ? answerRepository.findByIsAcceptedTrue()
                                             : answerRepository.findByIsAcceptedFalse()).stream()
                .filter(a -> a.getOwnerReputation() != null && a.getOwnerReputation() <= limits[2])
                .filter(a -> a.getBody() != null && a.getBody().length() <= limits[3])
                .filter(a -> a.getOrder_oq() != null && a.getOrder_oq() <= limits[4])
                .filter(a -> a.getScore() != null && a.getScore() <= limits[0]).toList();
        for (Answer answer : answers) {
            Optional<Question> questionOpt = questionRepository.findById(answer.getQuestionId());
            if (questionOpt.isPresent()) {
                Map<String, Number> ans = new HashMap<>();
                long timeInterval = Duration.between(questionOpt.get().getCreationDate(), answer.getCreationDate()).toMinutes();
                if (timeInterval <= limits[1]) {
                    ans.put(ID, answer.getAnswerId());
                    ans.put(SCORE, treat(answer.getScore(), 10));
                    ans.put("timeInterval", treat(timeInterval, 1));
                    ans.put("order", treat(answer.getOrder_oq(),  0));
                    ans.put("reputation", treat(answer.getOwnerReputation(), 1));
                    ans.put("answerLength", treat(answer.getBody().length(), 1));
                    answersData.add(ans);
                }
            }
        } return answersData;
    }
    private Number treat(Number value, long logInc) {
        return switch (treatMode) {
            case 0 -> value;
            case 1 -> Math.log(value.longValue() + logInc);
            default -> 0;
        };
    }
    private int treatMode;
    private static final String SCORE = "score", ID = "id";


    /**
     * 1a. Top-N Frequent Topics
     */
    public Map<String, Long> getTopTagFrequency(int topN) {
        return getTopTopicsAndDistribution(topN);
    }
    /**
     * 1b. Frequency of specific Topic
     */
    public Map<String, Long> getTopicFrequencyByName(String topic, boolean ignoreCase) {
        return (ignoreCase) ? siftIgnoreCaseOrDefault(getTagFrequency(), topic, 0L)
                            : Map.of(topic, getTagFrequency().getOrDefault(topic, 0L));
    }
    private <T> Map<String, T> siftIgnoreCaseOrDefault(Map<String, T> map, String key, T defaultValue) {
        return map.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(key))
                .findFirst()
                .map(entry -> Collections.singletonMap(entry.getKey(), entry.getValue()))
                .orElse(Map.of(key, defaultValue));
    }

    /**
     * 2a. Top-N Frequent Bugs
     */
    public Map<String, Map<String, Long>> getTopBugFrequency(String type, int topN) {
        return getTopEE(topN, type);
    }
    /**
     * 2b. Frequency of specific Bug
     */
    public Map<String, Map<String, Long>> getBugFrequencyByName(String type, String bug, boolean ignoreCase) {
        return (ignoreCase) ? siftIgnoreCaseOrDefault(getTopEE(-1, type), bug, new HashMap<>(dataTemplate))
                            : Map.of(bug, getTopEE(-1, type).getOrDefault(bug, new HashMap<>(dataTemplate)));
    }

}
