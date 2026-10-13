package com.quizhub.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "results")
public class Result {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int total;

    @Column(nullable = false)
    private int timeTakenSeconds = 0;

    @Column(nullable = false)
    private int xpEarned = 0;

    @Column(length = 100)
    private String badge;

    @Column(nullable = false)
    private LocalDateTime takenAt = LocalDateTime.now();

    protected Result() {}

    public Result(User user, Quiz quiz, int score, int total, int timeTakenSeconds, int xpEarned, String badge) {
        this.user = user;
        this.quiz = quiz;
        this.score = score;
        this.total = total;
        this.timeTakenSeconds = Math.max(0, timeTakenSeconds);
        this.xpEarned = xpEarned;
        this.badge = badge;
        this.takenAt = LocalDateTime.now();
    }

    public Result(User user, Quiz quiz, int score, int total) {
        this(user, quiz, score, total, 0, score * 20, score == total ? "Perfect score" : "Quiz Finished");
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Quiz getQuiz() { return quiz; }
    public int getScore() { return score; }
    public int getTotal() { return total; }
    public int getTimeTakenSeconds() { return timeTakenSeconds; }
    public int getXpEarned() { return xpEarned; }
    public String getBadge() { return badge; }
    public LocalDateTime getTakenAt() { return takenAt; }
}
