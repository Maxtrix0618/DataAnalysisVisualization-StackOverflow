package com.example.service;

import com.example.entity.*;
import com.example.repository.AnswerRepository;
import com.example.repository.CommentRepository;
import com.example.repository.QuestionRepository;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class DataCollectionService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final CommentRepository commentRepository;

    public DataCollectionService(
            QuestionRepository questionRepository,
            AnswerRepository answerRepository,
            CommentRepository commentRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.commentRepository = commentRepository;
    }

    private final long[] nfQAC = new long[6];

    public long[] collectAndStoreData(int fp, int tp, int psz) {
        log("\nStart Collection on size-"+psz+"-page "+fp+"~"+tp+" :\n");
        fetchThreads(fp, tp, psz);
        updateAnswerOrder();
        countTotal();
        log("Mission Accomplished!\n");
        return nfQAC;
    }

    private void fetchThreads(int fromPage, int toPage, int pageSize) {
        List<Long> allQIDs = questionRepository.findAll().stream().map(Question::getQuestionId).toList();
        for (int page=fromPage; page<=toPage; page++) {
            log("[Page"+(page)+"]\n");
            int q_count = 0;
            for (Map<String, Object> item : grabItemMapOfAPI("questions?page="+page+"&pagesize="+pageSize+"&order=desc&sort=activity&tagged=java")) {
                Question question = mapToQuestion(item); log("T"+(++q_count));
                if (!allQIDs.contains(question.getQuestionId())) {
                    questionRepository.save(question); nfQAC[0]++; log("|");
                    fetchAnswers(question.getQuestionId());
                    fetchCommentsForQuestion(question.getQuestionId()); log("\n");
                } else log("~\n");
            }
        } log("Collection Completed.\n");
    }

    private void fetchAnswers(Long questionId) {
        for (Map<String, Object> item : grabItemMapOfAPI("questions/"+questionId+"/answers?order=desc&sort=activity")) {
            Answer answer = mapToAnswer(item, questionId);
            answerRepository.save(answer); nfQAC[1]++; log("*");
            fetchCommentsForAnswer(answer.getAnswerId());
        }
    }

    private void fetchCommentsForAnswer(Long answerId) {
        for (Map<String, Object> item : grabItemMapOfAPI("answers/"+answerId+"/comments?order=desc&sort=creation")) {
            Comment comment = mapToComment(item, answerId, 1);
            commentRepository.save(comment); nfQAC[2]++; log(".");
        }
    }

    private void fetchCommentsForQuestion(Long questionId) {
        for (Map<String, Object> item : grabItemMapOfAPI("questions/"+questionId+"/comments?order=desc&sort=creation")) {
            Comment comment = mapToComment(item, questionId, 0);
            commentRepository.save(comment); nfQAC[2]++; log(",");
        }
    }

    public void updateAnswerOrder() {
        log("Updating Answer Order...\n");
        List<Long> questionIds = questionRepository.findAll()
                .stream()
                .map(Question::getQuestionId)
                .toList();
        int q_total = questionIds.size();
        log("ready...");
        for (int i = 0; i < q_total; i++) {
            log("\rT "+i+"/"+q_total);
            List<Answer> answers = answerRepository.findByQuestionIdOrderByCreationDateAsc(questionIds.get(i));
            for (int o = 0; o < answers.size(); o++) {
                Answer answer = answers.get(o);
                answer.setOrder_oq(o+1);
                answerRepository.save(answer);
            }
        } log("\nUpdate Completed.\n");
    }

    private Question mapToQuestion(Map<String, Object> item) {
        Question question = new Question();
        question.setQuestionId(getLongFromMap(item, "question_id"));
        question.setTitle(getStringFromMap(item, "title"));
        question.setTags(String.join(",", getTagListFromMap(item)));

        question.setViewCount(getIntFromMap(item, "view_count"));
        question.setAnswerCount(getIntFromMap(item, "answer_count"));
        question.setIsAnswered(getBooleanFromMap(item, "is_answered"));
        question.setAcceptedAnswerId(getLongFromMap(item, "accepted_answer_id"));

        question.setLastEditDate(getInstantFromMap(item, "last_edit_date"));
        question.setLastActivityDate(getInstantFromMap(item, "last_activity_date"));
        setPostAttributes(question, item);
        return question;
    }

    private Answer mapToAnswer(Map<String, Object> item, Long questionId) {
        Answer answer = new Answer();
        answer.setAnswerId(getLongFromMap(item, "answer_id"));
        answer.setQuestionId(questionId);
        answer.setIsAccepted(getBooleanFromMap(item, "is_accepted"));
        answer.setLastEditDate(getInstantFromMap(item, "last_edit_date"));

        answer.setLastActivityDate(getInstantFromMap(item, "last_activity_date"));
        setPostAttributes(answer, item);
        return answer;
    }

    private Comment mapToComment(Map<String, Object> item, Long postId, int postTo) {
        Comment comment = new Comment();
        comment.setCommentId(getLongFromMap(item, "comment_id"));
        comment.setPostId(postId);
        comment.setPostTo(postTo);
        setPostAttributes(comment, item);
        return comment;
    }

    private void setPostAttributes(Post post, Map<String, Object> item) {
        post.setBody(getStringFromMap(item, "body"));
        post.setScore(getIntFromMap(item, "score"));

        Map<String, Object> owner = getOwnerMapFromMap(item);
        post.setOwnerUserId(getLongFromMap(owner, "user_id"));
        post.setOwnerDisplayName(getStringFromMap(owner, "display_name"));
        post.setOwnerReputation(getIntFromMap(owner, "reputation"));
        post.setOwnerAcceptRate(getIntFromMap(owner, "accept_rate"));

        post.setCreationDate(getInstantFromMap(item, "creation_date"));
    }

    private List<String> getTagListFromMap(Map<String, Object> map) {
        Object value = map.get("tags");
        if (value instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<String> list_ = (List<String>) value;
            return list_;
        } return List.of();
    }
    private Map<String, Object> getOwnerMapFromMap(Map<String, Object> map) {
        Object value = map.get("owner");
        if (value instanceof Map<?,?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map_ = (Map<String, Object>) value;
            return map_;
        } return Map.of();
    }

    private static final int strMax = 1000000;
    private String getStringFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        String str = value.toString();
        return (str.length() < strMax) ? str : str.substring(0, strMax);
    }
    private Integer getIntFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return (value instanceof Integer) ? (Integer) value : (value instanceof Number) ? ((Number) value).intValue() : null;
    }
    private Long getLongFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return (value instanceof Long) ? (Long) value : (value instanceof Number) ? ((Number) value).longValue() : null;
    }
    private Boolean getBooleanFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : null;
    }
    private Instant getInstantFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number ? Instant.ofEpochSecond(((Number) value).longValue()) : null;
    }

    private List<Map<String, Object>> grabItemMapOfAPI(String sub) {
        try {Thread.sleep(10);} catch (InterruptedException e) {throw new RuntimeException(e);}
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "https://api.stackexchange.com/2.3/"+sub+"&site=stackoverflow&filter=withbody&key="+KEY,
                HttpMethod.GET, createHttpEntity(), new ParameterizedTypeReference<>() {});
        if (response.getBody() != null && response.getBody().containsKey("items"))
            if (response.getBody().get("items") instanceof List<?> items) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list_map_ = (List<Map<String, Object>>) items;
                return list_map_;
            }
        return List.of();
    }
    private static final String KEY = "rl_JNLQmhuxoZHxgRk7zDL9gsTi2";

    private HttpEntity<Void> createHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0");
        headers.set("Accept", "application/json");
        headers.set("Connection", "keep-alive");
        return new HttpEntity<>(headers);
    }

    private void countTotal() {
        nfQAC[3] = questionRepository.count();
        nfQAC[4] = answerRepository.count();
        nfQAC[5] = commentRepository.count();
    }

    /**
     * T: thread
     *
     */
    private void log(String message) {System.out.print(message);}

//    public long[] refreshComments() {
//        List<Long> idsRecorded = blotRepository.findAll().stream().map(Blot::getId).toList();
//        answerRepository.findAll()
//                .stream().filter(e -> !idsRecorded.contains(e.getAnswerId()))
//                .forEach(answer -> fetchCommentsForAnswer(answer.getAnswerId()));
//        countTotal();
//        return nfQAC;
//    }
//
//    private void CLEARALL() {
//        questionRepository.deleteAll();
//        answerRepository.deleteAll();
//        commentRepository.deleteAll();
//    }
//    private void test4save() {
//        Question q = new Question();
//        q.setQuestionId(233L);
//        q.setTitle("Why is the sky blue?");
//        q.setBody("This is said to be caused by Rayleigh scattering, can anyone explain it in plain English?");
//        questionRepository.save(q);
//    }

}
