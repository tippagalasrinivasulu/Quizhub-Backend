package com.quizhub.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** All request/response objects in one place. */
public final class Dtos {
    private Dtos() {}

    // ---- auth ----
    public record RegisterRequest(@NotBlank @Size(min = 3, max = 50) String username,
                                  @NotBlank @Email String email,
                                  @NotBlank @Size(min = 4, max = 100) String password) {}
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record AuthResponse(String token, String username, String role) {}

    // ---- quizzes ----
    public record QuizRequest(@NotBlank String title, String description, @NotBlank String difficulty) {}
    public record QuizSummary(Long id, String title, String description, String difficulty, int questionCount, int seconds) {}
    public record QuestionDto(Long id, String text, List<String> options) {}   // correct answer is never sent to students
    public record QuizDetail(Long id, String title, int seconds, List<QuestionDto> questions) {}

    // ---- submit / results ----
    public record SubmitRequest(Map<Long, Integer> answers) {}                 // questionId -> chosen option index
    public record ReviewItem(String question, List<String> options, Integer picked, int correct) {}
    public record ResultResponse(Long id, String quiz, int score, int total, int percent, String badge, List<ReviewItem> review) {}
    public record HistoryItem(Long id, String quiz, int score, int total, int percent, LocalDateTime takenAt) {}
    public record LeaderboardRow(String username, long quizzes, long points) {}
    public record SubjectStat(String quiz, double average, long attempts) {}

    // ---- admin ----
    public record QuestionRequest(@NotNull Long quizId, @NotBlank String text,
                                  @NotNull @Size(min = 4, max = 4) List<@NotBlank String> options,
                                  @Min(0) @Max(3) int correctOption) {}
    public record AdminQuestion(Long id, String text, List<String> options, int correctOption) {}
}
