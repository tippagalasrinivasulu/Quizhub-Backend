package com.quizhub.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
public class Quiz {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 100) private String title;
    @Column(length = 255) private String description;
    @Column(length = 20) private String difficulty;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();

    protected Quiz() {}
    public Quiz(String title, String description, String difficulty) {
        this.title = title; this.description = description; this.difficulty = difficulty;
    }
    public void addQuestion(Question q) { q.setQuiz(this); questions.add(q); }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDifficulty() { return difficulty; }
    public List<Question> getQuestions() { return questions; }
}
