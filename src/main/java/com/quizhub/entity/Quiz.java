package com.quizhub.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
public class Quiz {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String title;

    @Column(length = 100)
    private String subject = "General";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String difficulty = "Medium";

    @Column(nullable = false)
    private Integer durationMinutes = 5;

    @Column(nullable = false)
    private Boolean isPyq = false;

    @Column(length = 50)
    private String year;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();

    protected Quiz() {}

    public Quiz(String title, String subject, String description, String difficulty, Integer durationMinutes, Boolean isPyq, String year) {
        this.title = title;
        this.subject = (subject == null || subject.isBlank()) ? "General" : subject;
        this.description = description;
        this.difficulty = (difficulty == null || difficulty.isBlank()) ? "Medium" : difficulty;
        this.durationMinutes = (durationMinutes == null || durationMinutes <= 0) ? 5 : durationMinutes;
        this.isPyq = isPyq != null && isPyq;
        this.year = year;
    }

    public Quiz(String title, String description, String difficulty) {
        this(title, "General", description, difficulty, 5, false, null);
    }

    public void addQuestion(Question q) {
        q.setQuiz(this);
        questions.add(q);
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubject() { return subject == null ? "General" : subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public Integer getDurationMinutes() { return durationMinutes == null || durationMinutes <= 0 ? 5 : durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public Boolean getIsPyq() { return isPyq != null && isPyq; }
    public void setIsPyq(Boolean isPyq) { this.isPyq = isPyq; }
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
    public List<Question> getQuestions() { return questions; }
}
