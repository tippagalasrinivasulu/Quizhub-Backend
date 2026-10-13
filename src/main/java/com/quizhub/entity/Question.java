package com.quizhub.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String optionA;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String optionB;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String optionC;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String optionD;

    @Column(nullable = false)
    private int correctOption; // 0=A, 1=B, 2=C, 3=D

    protected Question() {}

    public Question(String text, String a, String b, String c, String d, int correctOption) {
        this.text = (text != null && !text.isBlank()) ? text.trim() : "Question text";
        this.optionA = (a != null && !a.isBlank()) ? a.trim() : "Option A";
        this.optionB = (b != null && !b.isBlank()) ? b.trim() : "Option B";
        this.optionC = (c != null && !c.isBlank()) ? c.trim() : "Option C";
        this.optionD = (d != null && !d.isBlank()) ? d.trim() : "Option D";
        this.correctOption = Math.max(0, Math.min(3, correctOption));
    }

    public List<String> options() {
        return List.of(optionA, optionB, optionC, optionD);
    }

    public Long getId() { return id; }
    public String getText() { return text; }
    public int getCorrectOption() { return correctOption; }
    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }
}
