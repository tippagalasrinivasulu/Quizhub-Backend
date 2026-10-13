package com.quizhub.controller;

import com.quizhub.dto.Dtos.*;
import com.quizhub.entity.Quiz;
import com.quizhub.service.AiAgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiAgentService aiAgentService;

    public AiController(AiAgentService aiAgentService) {
        this.aiAgentService = aiAgentService;
    }

    /**
     * Upload PYQ exam paper PDF -> AI Agent extracts questions and creates quiz.
     */
    @PostMapping("/upload-pyq-pdf")
    public ResponseEntity<QuizSummary> uploadPyqPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "difficulty", defaultValue = "Medium") String difficulty,
            @RequestParam(value = "durationMinutes", defaultValue = "10") Integer durationMinutes,
            @RequestParam(value = "questionCount", defaultValue = "10") Integer questionCount,
            @RequestParam(value = "year", required = false) String year
    ) {
        String text = aiAgentService.extractTextFromPdf(file);
        List<GeneratedQuestionDto> questions = aiAgentService.processQuestionsFromText(text, questionCount);

        String finalTitle = (title != null && !title.isBlank())
                ? title
                : (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank())
                ? file.getOriginalFilename().replaceFirst("[.][^.]+$", "") + " (AI PYQ)"
                : "PYQ AI Assessment";

        AiGenerateRequest req = new AiGenerateRequest(
                (subject != null && !subject.isBlank()) ? subject : "PYQ Exam",
                finalTitle,
                difficulty,
                durationMinutes,
                text,
                true,
                (year != null && !year.isBlank()) ? year : "2024",
                questions.size()
        );

        Quiz quiz = aiAgentService.createQuizFromAi(req, questions);
        return ResponseEntity.ok(new QuizSummary(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getSubject(),
                quiz.getDescription(),
                quiz.getDifficulty(),
                quiz.getQuestions().size(),
                quiz.getDurationMinutes(),
                quiz.getDurationMinutes() * 60,
                quiz.getIsPyq(),
                quiz.getYear()
        ));
    }

    /**
     * Generate quiz from pasted PYQ text or concept notes using AI Agent.
     */
    @PostMapping("/generate-quiz")
    public ResponseEntity<QuizSummary> generateQuizFromText(@RequestBody AiGenerateRequest req) {
        int count = req.questionCount() != null && req.questionCount() > 0 ? req.questionCount() : 6;
        List<GeneratedQuestionDto> questions = aiAgentService.processQuestionsFromText(req.rawText(), count);

        Quiz quiz = aiAgentService.createQuizFromAi(req, questions);
        return ResponseEntity.ok(new QuizSummary(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getSubject(),
                quiz.getDescription(),
                quiz.getDifficulty(),
                quiz.getQuestions().size(),
                quiz.getDurationMinutes(),
                quiz.getDurationMinutes() * 60,
                quiz.getIsPyq(),
                quiz.getYear()
        ));
    }
}
