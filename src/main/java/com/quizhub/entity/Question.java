package com.quizhub.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "questions")
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;
    @Column(nullable = false, length = 500) private String text;
    @Column(nullable = false) private String optionA;
    @Column(nullable = false) private String optionB;
    @Column(nullable = false) private String optionC;
    @Column(nullable = false) private String optionD;
    @Column(nullable = false) private int correctOption; // 0=A, 1=B, 2=C, 3=D

    protected Question() {}
    public Question(String text, String a, String b, String c, String d, int correctOption) {
        this.text = text; this.optionA = a; this.optionB = b; this.optionC = c; this.optionD = d;
        this.correctOption = correctOption;
    }
    public List<String> options() { return List.of(optionA, optionB, optionC, optionD); }
    public Long getId() { return id; }
    public String getText() { return text; }
    public int getCorrectOption() { return correctOption; }
    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }
}
