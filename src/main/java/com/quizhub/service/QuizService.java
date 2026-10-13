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
    private final QuizRepository quizzes;
    private final QuestionRepository questions;
    private final ResultRepository results;
    private final UserRepository users;

    public QuizService(QuizRepository quizzes, QuestionRepository questions, ResultRepository results, UserRepository users) {
        this.quizzes = quizzes;
        this.questions = questions;
        this.results = results;
        this.users = users;
    }

    private Quiz find(Long id) {
        return quizzes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
    }

    private QuizSummary summary(Quiz q) {
        int n = q.getQuestions().size();
        int minutes = q.getDurationMinutes() != null && q.getDurationMinutes() > 0 ? q.getDurationMinutes() : Math.max(2, (n * 30 + 59) / 60);
        int seconds = minutes * 60;
        return new QuizSummary(
                q.getId(),
                q.getTitle(),
                q.getSubject(),
                q.getDescription(),
                q.getDifficulty(),
                n,
                minutes,
                seconds,
                q.getIsPyq(),
                q.getYear()
        );
    }

    private static int percent(int score, int total) {
        return total == 0 ? 0 : Math.round(score * 100f / total);
    }

    private static String primaryBadge(int p, boolean speedBonus) {
        if (p == 100) return speedBonus ? "⚡ Speed Century" : "🏆 Century Club";
        if (p >= 80) return "🥇 Gold Scholar";
        if (p >= 60) return "🥈 Silver Master";
        if (p >= 40) return "🥉 Bronze Learner";
        return "🌱 Aspiring Thinker";
    }

    // ---------- student endpoints ----------
    @Transactional(readOnly = true)
    public List<QuizSummary> list() {
        return quizzes.findAll().stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public QuizDetail start(Long id) {
        Quiz q = find(id);
        List<Question> list = new ArrayList<>(q.getQuestions());
        if (list.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This quiz has no questions yet");
        Collections.shuffle(list);
        int minutes = q.getDurationMinutes() != null && q.getDurationMinutes() > 0 ? q.getDurationMinutes() : Math.max(2, (list.size() * 30 + 59) / 60);
        return new QuizDetail(
                q.getId(),
                q.getTitle(),
                q.getSubject(),
                minutes,
                minutes * 60,
                q.getIsPyq(),
                q.getYear(),
                list.stream().map(x -> new QuestionDto(x.getId(), x.getText(), x.options())).toList()
        );
    }

    @Transactional
    public ResultResponse submit(Long id, String username, SubmitRequest req) {
        Quiz q = find(id);
        User u = users.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if ("ADMIN".equalsIgnoreCase(u.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins cannot participate in student quizzes. Use Admin Hub to manage quizzes.");
        }

        Map<Long, Integer> given = (req != null && req.answers() != null) ? req.answers() : Map.of();
        int timeTaken = (req != null && req.timeTakenSeconds() != null) ? req.timeTakenSeconds() : 0;
        int totalSeconds = (q.getDurationMinutes() != null ? q.getDurationMinutes() : 5) * 60;
        boolean speedBonus = timeTaken > 0 && timeTaken <= (totalSeconds / 2);

        int score = 0;
        List<ReviewItem> review = new ArrayList<>();
        for (Question x : q.getQuestions()) {
            Integer picked = given.get(x.getId());
            if (picked != null && picked == x.getCorrectOption()) score++;
            review.add(new ReviewItem(x.getText(), x.options(), picked, x.getCorrectOption()));
        }

        int total = q.getQuestions().size();
        int p = percent(score, total);

        // Gamification calculations
        int xpEarned = score * 20;
        if (p == 100) xpEarned += 50; // Perfect score bonus
        if (speedBonus) xpEarned += 25; // Speed bonus
        if (score == 0 && total > 0) xpEarned = 5; // Encouragement XP for participating

        String badge = primaryBadge(p, speedBonus);

        // Update user XP
        u.addXp(xpEarned);
        users.save(u);

        Result r = results.save(new Result(u, q, score, total, timeTaken, xpEarned, badge));

        long quizCount = results.countByUserUsername(username);
        List<String> unlockedBadges = computeUserBadges(u, quizCount, p, speedBonus);

        return new ResultResponse(
                r.getId(),
                q.getTitle(),
                q.getSubject(),
                score,
                total,
                p,
                badge,
                xpEarned,
                u.getXp(),
                u.getLevel(),
                u.getRank(),
                unlockedBadges,
                review
        );
    }

    private List<String> computeUserBadges(User u, long quizCount, int lastPercent, boolean lastSpeedBonus) {
        List<String> list = new ArrayList<>();
        if (quizCount >= 1) list.add("🎯 First Flight");
        if (quizCount >= 5) list.add("🔥 Quiz Veteran");
        if (quizCount >= 10) list.add("👑 Trivia Titan");
        if (lastPercent == 100) list.add("🏆 Century Club");
        if (lastPercent >= 80) list.add("🌟 High Achiever");
        if (lastSpeedBonus) list.add("⚡ Speed Demon");
        if (u.getXp() >= 1000) list.add("💎 Diamond Mind");
        if (u.getXp() >= 1500) list.add("🎖️ Grandmaster Scholar");
        return list;
    }

    @Transactional(readOnly = true)
    public List<HistoryItem> history(String username) {
        User u = users.findByUsername(username).orElse(null);
        if (u != null && "ADMIN".equalsIgnoreCase(u.getRole())) {
            return List.of();
        }
        return results.findByUserUsernameOrderByTakenAtDesc(username).stream()
                .map(r -> new HistoryItem(
                        r.getId(),
                        r.getQuiz().getTitle(),
                        r.getQuiz().getSubject(),
                        r.getScore(),
                        r.getTotal(),
                        percent(r.getScore(), r.getTotal()),
                        r.getTimeTakenSeconds(),
                        r.getXpEarned(),
                        r.getBadge() != null ? r.getBadge() : primaryBadge(percent(r.getScore(), r.getTotal()), false),
                        r.getTakenAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubjectStat> stats(String username) {
        User u = users.findByUsername(username).orElse(null);
        if (u != null && "ADMIN".equalsIgnoreCase(u.getRole())) {
            return List.of();
        }
        return results.subjectStats(username).stream()
                .map(o -> new SubjectStat((String) o[0], Math.round(((Number) o[1]).doubleValue()), ((Number) o[2]).longValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaderboardRow> leaderboard() {
        return results.leaderboard().stream()
                .map(o -> {
                    String name = (String) o[0];
                    long count = ((Number) o[1]).longValue();
                    long points = ((Number) o[2]).longValue();
                    long userXp = o[4] != null ? ((Number) o[4]).longValue() : points * 20;

                    int level = 1;
                    if (userXp >= 1500) level = 6;
                    else if (userXp >= 1000) level = 5;
                    else if (userXp >= 600) level = 4;
                    else if (userXp >= 300) level = 3;
                    else if (userXp >= 100) level = 2;

                    String rank = (level == 6) ? "Grandmaster" : (level == 5) ? "Master" : (level == 4) ? "Diamond" : (level == 3) ? "Platinum" : (level == 2) ? "Gold" : "Bronze";
                    String topBadge = (points >= 15) ? "🏆 Elite Scholar" : (points >= 8) ? "🌟 Rising Star" : "🌱 Challenger";

                    return new LeaderboardRow(name, count, userXp, level, rank, topBadge);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public UserProfile getUserProfile(String username) {
        User u = users.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if ("ADMIN".equalsIgnoreCase(u.getRole())) {
            return new UserProfile(
                    u.getId(),
                    u.getUsername(),
                    u.getEmail(),
                    "ADMIN",
                    0L,
                    1,
                    "Administrator",
                    List.of("🛡️ System Administrator"),
                    0,
                    0.0
            );
        }
        List<Result> userResults = results.findByUserUsernameOrderByTakenAtDesc(username);
        long count = userResults.size();
        double avg = userResults.isEmpty() ? 0 : userResults.stream().mapToInt(r -> percent(r.getScore(), r.getTotal())).average().orElse(0.0);

        List<String> badges = new ArrayList<>();
        if (count >= 1) badges.add("🎯 First Flight");
        if (count >= 5) badges.add("🔥 Quiz Veteran");
        if (count >= 10) badges.add("👑 Trivia Titan");
        if (userResults.stream().anyMatch(r -> r.getScore() == r.getTotal() && r.getTotal() > 0)) badges.add("🏆 Century Club");
        if (userResults.stream().anyMatch(r -> r.getTimeTakenSeconds() > 0 && r.getTimeTakenSeconds() < 120)) badges.add("⚡ Speed Demon");
        if (u.getXp() >= 500) badges.add("🌟 High Achiever");
        if (u.getXp() >= 1500) badges.add("🎖️ Grandmaster");

        return new UserProfile(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getRole(),
                u.getXp(),
                u.getLevel(),
                u.getRank(),
                badges,
                count,
                Math.round(avg * 10.0) / 10.0
        );
    }

    // ---------- admin endpoints ----------
    @Transactional
    public QuizSummary createQuiz(QuizRequest r) {
        if (quizzes.existsByTitle(r.title())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A quiz with this title already exists");
        }
        Quiz q = new Quiz(
                r.title(),
                r.subject(),
                r.description(),
                r.difficulty(),
                r.durationMinutes(),
                r.isPyq(),
                r.year()
        );
        return summary(quizzes.save(q));
    }

    private AdminQuestion adminDto(Question x) {
        return new AdminQuestion(x.getId(), x.getText(), x.options(), x.getCorrectOption());
    }

    @Transactional
    public AdminQuestion addQuestion(QuestionRequest r) {
        Quiz q = find(r.quizId());
        List<String> o = r.options();
        Question x = new Question(r.text(), o.get(0), o.get(1), o.get(2), o.get(3), r.correctOption());
        q.addQuestion(x);
        return adminDto(questions.save(x));
    }

    @Transactional(readOnly = true)
    public List<AdminQuestion> questionsOf(Long quizId) {
        return questions.findByQuizId(quizId).stream().map(this::adminDto).toList();
    }

    @Transactional
    public void deleteQuestion(Long id) {
        if (!questions.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        questions.deleteById(id);
    }

    @Transactional
    public void deleteQuiz(Long id) {
        if (!quizzes.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found");
        quizzes.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<StudentSummary> getRegisteredStudents() {
        List<User> students = users.findByRole("STUDENT");
        List<StudentSummary> list = new ArrayList<>();
        for (User u : students) {
            List<Result> userResults = results.findByUserUsernameOrderByTakenAtDesc(u.getUsername());
            long count = userResults.size();
            double avg = userResults.isEmpty() ? 0.0 :
                    Math.round(userResults.stream().mapToInt(r -> percent(r.getScore(), r.getTotal())).average().orElse(0.0) * 10.0) / 10.0;
            list.add(new StudentSummary(u.getId(), u.getUsername(), u.getEmail(), u.getLevel(), u.getXp(), u.getRank(), count, avg));
        }
        return list;
    }

    @Transactional(readOnly = true)
    public StudentDetailAnalytics getStudentAnalytics(String email, String username) {
        User u = null;
        if (email != null && !email.isBlank()) {
            u = users.findByEmail(email.trim()).orElse(null);
        }
        if (u == null && username != null && !username.isBlank()) {
            u = users.findByUsername(username.trim()).orElse(null);
        }
        if (u == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found with specified email or username");
        }

        List<Result> userResults = results.findByUserUsernameOrderByTakenAtDesc(u.getUsername());
        long totalQuizzes = userResults.size();
        double overallAvg = userResults.isEmpty() ? 0.0 :
                Math.round(userResults.stream().mapToInt(r -> percent(r.getScore(), r.getTotal())).average().orElse(0.0) * 10.0) / 10.0;

        // Group by subject for subject-by-subject performance
        Map<String, List<Result>> bySubject = new LinkedHashMap<>();
        for (Result r : userResults) {
            String sub = (r.getQuiz().getSubject() != null && !r.getQuiz().getSubject().isBlank())
                    ? r.getQuiz().getSubject() : "General";
            bySubject.computeIfAbsent(sub, k -> new ArrayList<>()).add(r);
        }

        List<SubjectPerformance> subjectStats = new ArrayList<>();
        for (Map.Entry<String, List<Result>> entry : bySubject.entrySet()) {
            String subject = entry.getKey();
            List<Result> subResults = entry.getValue();
            long count = subResults.size();
            int totalQ = subResults.stream().mapToInt(Result::getTotal).sum();
            int correctQ = subResults.stream().mapToInt(Result::getScore).sum();
            double acc = totalQ == 0 ? 0.0 : Math.round((correctQ * 100.0 / totalQ) * 10.0) / 10.0;
            String mastery = acc >= 85.0 ? "🏆 Master" : acc >= 70.0 ? "⚡ Proficient" : acc >= 50.0 ? "📈 Developing" : "🌱 Needs Practice";
            subjectStats.add(new SubjectPerformance(subject, count, totalQ, correctQ, acc, mastery));
        }

        List<String> badges = computeUserBadges(u, totalQuizzes, (int) overallAvg, false);

        List<HistoryItem> history = userResults.stream().map(r -> new HistoryItem(
                r.getId(),
                r.getQuiz().getTitle(),
                r.getQuiz().getSubject(),
                r.getScore(),
                r.getTotal(),
                percent(r.getScore(), r.getTotal()),
                r.getTimeTakenSeconds(),
                r.getXpEarned(),
                r.getBadge() != null ? r.getBadge() : primaryBadge(percent(r.getScore(), r.getTotal()), false),
                r.getTakenAt()
        )).toList();

        return new StudentDetailAnalytics(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getLevel(),
                u.getXp(),
                u.getRank(),
                totalQuizzes,
                overallAvg,
                badges,
                subjectStats,
                history
        );
    }

    @Transactional(readOnly = true)
    public AdminDashboardStats getAdminDashboardStats() {
        long totalStudents = users.findByRole("STUDENT").size();
        long totalQuizzes = quizzes.count();
        long totalQuestions = questions.count();
        long totalSubmissions = results.count();

        List<AdminSubmissionItem> recent = results.findTop20ByOrderByTakenAtDesc().stream().map(r -> new AdminSubmissionItem(
                r.getId(),
                r.getUser().getUsername(),
                r.getUser().getEmail(),
                r.getQuiz().getTitle(),
                r.getQuiz().getSubject(),
                r.getScore(),
                r.getTotal(),
                percent(r.getScore(), r.getTotal()),
                r.getBadge(),
                r.getTakenAt()
        )).toList();

        return new AdminDashboardStats(totalStudents, totalQuizzes, totalQuestions, totalSubmissions, recent);
    }
}
