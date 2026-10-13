package com.quizhub.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** All request/response objects in one place. */
public final class Dtos {
    private Dtos() {}

    // ---- auth ----
    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 4, max = 100) String password) {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {}

    public record AuthResponse(
            String token,
            String username,
            String email,
            String role,
            int level,
            long xp,
            String rank) {}

    public record UserProfile(
            Long id,
            String username,
            String email,
            String role,
            long xp,
            int level,
            String rank,
            List<String> badges,
            long quizzesCount,
            double avgPercent) {}

    // ---- quizzes ----
    public record QuizRequest(
            @NotBlank String title,
            String subject,
            String description,
            @NotBlank String difficulty,
            Integer durationMinutes,
            Boolean isPyq,
            String year) {}

    public record QuizSummary(
            Long id,
            String title,
            String subject,
            String description,
            String difficulty,
            int questionCount,
            int durationMinutes,
            int seconds,
            Boolean isPyq,
            String year) {}

    public record QuestionDto(Long id, String text, List<String> options) {}

    public record QuizDetail(
            Long id,
            String title,
            String subject,
            int durationMinutes,
            int seconds,
            Boolean isPyq,
            String year,
            List<QuestionDto> questions) {}

    // ---- submit / results ----
    public record SubmitRequest(
            Map<Long, Integer> answers,
            Integer timeTakenSeconds) {}

    public record ReviewItem(String question, List<String> options, Integer picked, int correct) {}

    public record ResultResponse(
            Long id,
            String quiz,
            String subject,
            int score,
            int total,
            int percent,
            String badge,
            int xpEarned,
            long totalXp,
            int level,
            String rank,
            List<String> unlockedBadges,
            List<ReviewItem> review) {}

    public record HistoryItem(
            Long id,
            String quiz,
            String subject,
            int score,
            int total,
            int percent,
            int timeTakenSeconds,
            int xpEarned,
            String badge,
            LocalDateTime takenAt) {}

    public record LeaderboardRow(
            String username,
            long quizzes,
            long points,
            int level,
            String rank,
            String topBadge) {}

    public record SubjectStat(String subject, double average, long attempts) {}

    // ---- admin ----
    public record QuestionRequest(
            @NotNull Long quizId,
            @NotBlank String text,
            @NotNull @Size(min = 4, max = 4) List<@NotBlank String> options,
            @Min(0) @Max(3) int correctOption) {}

    public record AdminQuestion(Long id, String text, List<String> options, int correctOption) {}

    // ---- admin student analytics & dashboard ----
    public record StudentSummary(
            Long id,
            String username,
            String email,
            int level,
            long xp,
            String rank,
            long quizCount,
            double avgPercent) {}

    public record SubjectPerformance(
            String subject,
            long quizzesTaken,
            int totalQuestions,
            int correctAnswers,
            double accuracy,
            String masteryLevel) {}

    public record StudentDetailAnalytics(
            Long id,
            String username,
            String email,
            int level,
            long xp,
            String rank,
            long totalQuizzes,
            double overallAccuracy,
            List<String> badges,
            List<SubjectPerformance> subjectPerformance,
            List<HistoryItem> recentAttempts) {}

    public record AdminSubmissionItem(
            Long id,
            String username,
            String email,
            String quizTitle,
            String subject,
            int score,
            int total,
            int percent,
            String badge,
            LocalDateTime takenAt) {}

    public record AdminDashboardStats(
            long totalStudents,
            long totalQuizzes,
            long totalQuestions,
            long totalSubmissions,
            List<AdminSubmissionItem> recentSubmissions) {}

    // ---- AI agent / PYQ generator ----
    public record AiGenerateRequest(
            String subject,
            String title,
            String difficulty,
            Integer durationMinutes,
            String rawText,
            Boolean isPyq,
            String year,
            Integer questionCount) {}

    public record GeneratedQuestionDto(
            String text,
            List<String> options,
            int correctOption,
            String explanation) {}

    public record AiQuizResponse(
            Long quizId,
            String title,
            String subject,
            int questionCount,
            int durationMinutes,
            List<GeneratedQuestionDto> questions) {}
}
