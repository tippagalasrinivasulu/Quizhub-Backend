package com.quizhub.service;

import com.quizhub.dto.Dtos.*;
import com.quizhub.entity.*;
import com.quizhub.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class QuizService {
    static final int SECONDS_PER_QUESTION = 30;
    private final QuizRepository quizzes;
    private final QuestionRepository questions;
    private final ResultRepository results;
    private final UserRepository users;

    public QuizService(QuizRepository quizzes, QuestionRepository questions, ResultRepository results, UserRepository users) {
        this.quizzes = quizzes; this.questions = questions; this.results = results; this.users = users;
    }

    private Quiz find(Long id) {
        return quizzes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
    }
    private QuizSummary summary(Quiz q) {
        int n = q.getQuestions().size();
        return new QuizSummary(q.getId(), q.getTitle(), q.getDescription(), q.getDifficulty(), n, n * SECONDS_PER_QUESTION);
    }
    private static int percent(int score, int total) { return total == 0 ? 0 : Math.round(score * 100f / total); }
    private static String badge(int p) { return p == 100 ? "Perfect score" : p >= 75 ? "High scorer" : p >= 50 ? "Getting there" : "Keep practicing"; }

    // ---------- student ----------
    @Transactional(readOnly = true)
    public List<QuizSummary> list() { return quizzes.findAll().stream().map(this::summary).toList(); }

    @Transactional(readOnly = true)
    public QuizDetail start(Long id) {
        Quiz q = find(id);
        List<Question> list = new ArrayList<>(q.getQuestions());
        if (list.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This quiz has no questions yet");
        Collections.shuffle(list);
        return new QuizDetail(q.getId(), q.getTitle(), list.size() * SECONDS_PER_QUESTION,
                list.stream().map(x -> new QuestionDto(x.getId(), x.getText(), x.options())).toList());
    }

    @Transactional
    public ResultResponse submit(Long id, String username, Map<Long, Integer> answers) {
        Quiz q = find(id);
        User u = users.findByUsername(username).orElseThrow();
        Map<Long, Integer> given = answers == null ? Map.of() : answers;
        int score = 0;
        List<ReviewItem> review = new ArrayList<>();
        for (Question x : q.getQuestions()) {
            Integer picked = given.get(x.getId());
            if (picked != null && picked == x.getCorrectOption()) score++;
            review.add(new ReviewItem(x.getText(), x.options(), picked, x.getCorrectOption()));
        }
        int total = q.getQuestions().size();
        Result r = results.save(new Result(u, q, score, total));
        int p = percent(score, total);
        return new ResultResponse(r.getId(), q.getTitle(), score, total, p, badge(p), review);
    }

    @Transactional(readOnly = true)
    public List<HistoryItem> history(String username) {
        return results.findByUserUsernameOrderByTakenAtDesc(username).stream()
                .map(r -> new HistoryItem(r.getId(), r.getQuiz().getTitle(), r.getScore(), r.getTotal(),
                        percent(r.getScore(), r.getTotal()), r.getTakenAt())).toList();
    }

    @Transactional(readOnly = true)
    public List<SubjectStat> stats(String username) {
        return results.subjectStats(username).stream()
                .map(o -> new SubjectStat((String) o[0], Math.round(((Number) o[1]).doubleValue()), ((Number) o[2]).longValue())).toList();
    }

    @Transactional(readOnly = true)
    public List<LeaderboardRow> leaderboard() {
        return results.leaderboard().stream()
                .map(o -> new LeaderboardRow((String) o[0], ((Number) o[1]).longValue(), ((Number) o[2]).longValue())).toList();
    }

    // ---------- admin ----------
    @Transactional
    public QuizSummary createQuiz(QuizRequest r) {
        if (quizzes.existsByTitle(r.title())) throw new ResponseStatusException(HttpStatus.CONFLICT, "A quiz with this title already exists");
        return summary(quizzes.save(new Quiz(r.title(), r.description(), r.difficulty())));
    }

    private AdminQuestion adminDto(Question x) { return new AdminQuestion(x.getId(), x.getText(), x.options(), x.getCorrectOption()); }

    @Transactional
    public AdminQuestion addQuestion(QuestionRequest r) {
        Quiz q = find(r.quizId());
        List<String> o = r.options();
        Question x = new Question(r.text(), o.get(0), o.get(1), o.get(2), o.get(3), r.correctOption());
        q.addQuestion(x);
        return adminDto(questions.save(x));
    }

    @Transactional(readOnly = true)
    public List<AdminQuestion> questionsOf(Long quizId) { return questions.findByQuizId(quizId).stream().map(this::adminDto).toList(); }

    @Transactional
    public void deleteQuestion(Long id) {
        if (!questions.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        questions.deleteById(id);
    }
}
