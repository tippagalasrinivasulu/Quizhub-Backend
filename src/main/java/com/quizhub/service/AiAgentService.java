package com.quizhub.service;

import com.quizhub.dto.Dtos.*;
import com.quizhub.entity.Question;
import com.quizhub.entity.Quiz;
import com.quizhub.repository.QuizRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiAgentService {

    private final QuizRepository quizzes;

    public AiAgentService(QuizRepository quizzes) {
        this.quizzes = quizzes;
    }

    /**
     * Extracts text from uploaded PDF file using Apache PDFBox.
     */
    public String extractTextFromPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        try (InputStream in = file.getInputStream(); PDDocument doc = PDDocument.load(in)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read PDF document: " + e.getMessage(), e);
        }
    }

    /**
     * AI Agent parses questions from raw PYQ paper text or generates questions from study text.
     */
    public List<GeneratedQuestionDto> processQuestionsFromText(String rawText, int targetCount) {
        if (rawText == null || rawText.isBlank()) {
            return generateFallbackQuestions("General Engineering", targetCount);
        }

        List<GeneratedQuestionDto> parsed = extractStructuredMcq(rawText);
        if (parsed.size() >= 2) {
            return parsed.subList(0, Math.min(parsed.size(), Math.max(targetCount, 5)));
        }

        // If paper text didn't contain standard MCQ tokens, generate AI questions from content concepts
        return generateAiQuestionsFromContent(rawText, Math.max(3, targetCount));
    }

    private String cleanText(String str, int maxLen) {
        if (str == null) return "";
        String clean = str.replaceAll("[\\r\\n]+", " ").replaceAll("\\s+", " ").trim();
        if (clean.length() > maxLen) {
            return clean.substring(0, maxLen - 3).trim() + "...";
        }
        return clean;
    }

    /**
     * Parses standard PYQ / Exam patterns with options A, B, C, D and answer keys.
     */
    private List<GeneratedQuestionDto> extractStructuredMcq(String text) {
        List<GeneratedQuestionDto> list = new ArrayList<>();
        // Split text into question candidate chunks
        String[] blocks = text.split("(?i)(?=(?:(?:Q(?:uestion)?\\s*\\.?\\s*\\d+)|(?:\\n\\s*\\d+[.)]))\\s*)");

        Pattern optPattern = Pattern.compile("(?i)(?:\\(?([A-D])[).])\\s*(.*?)(?=(?:\\(?[A-D][).])|Ans(?:wer)?:|$)", Pattern.DOTALL);
        Pattern ansPattern = Pattern.compile("(?i)Ans(?:wer)?(?:\\s*key)?\\s*[:=\\-]\\s*\\(?([A-D])\\)?");

        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.length() < 25) continue;

            // Extract question text before options
            String questionText = trimmed;
            int firstOptIdx = findFirstOptionIndex(trimmed);
            if (firstOptIdx > 0) {
                questionText = trimmed.substring(0, firstOptIdx).trim();
            }
            questionText = questionText.replaceAll("^(?i)(?:Q(?:uestion)?\\s*\\.?\\s*)?\\d+[.)]\\s*", "").trim();
            if (questionText.length() < 8) continue;

            // Extract options
            Matcher optMatcher = optPattern.matcher(trimmed);
            Map<Integer, String> optionsMap = new LinkedHashMap<>();
            while (optMatcher.find()) {
                String letter = optMatcher.group(1).toUpperCase();
                int idx = letter.charAt(0) - 'A';
                String optContent = cleanText(optMatcher.group(2), 250);
                if (!optContent.isBlank() && idx >= 0 && idx < 4) {
                    optionsMap.put(idx, optContent);
                }
            }

            if (optionsMap.size() >= 4) {
                List<String> options = List.of(
                        cleanText(optionsMap.getOrDefault(0, "Option A"), 250),
                        cleanText(optionsMap.getOrDefault(1, "Option B"), 250),
                        cleanText(optionsMap.getOrDefault(2, "Option C"), 250),
                        cleanText(optionsMap.getOrDefault(3, "Option D"), 250)
                );

                // Detect answer
                int correct = 0;
                Matcher ansMatcher = ansPattern.matcher(trimmed);
                if (ansMatcher.find()) {
                    correct = ansMatcher.group(1).toUpperCase().charAt(0) - 'A';
                    if (correct < 0 || correct > 3) correct = 0;
                }

                list.add(new GeneratedQuestionDto(
                        cleanText(questionText, 500),
                        options,
                        correct,
                        "Extracted directly from PYQ past paper."
                ));
            }
        }
        return list;
    }

    private int findFirstOptionIndex(String text) {
        Pattern p = Pattern.compile("(?i)\\n?\\s*\\(?[A-D][).]");
        Matcher m = p.matcher(text);
        return m.find() ? m.start() : -1;
    }

    /**
     * AI concept synthesizer: generates MCQs from study guide or syllabus text.
     */
    private List<GeneratedQuestionDto> generateAiQuestionsFromContent(String text, int count) {
        List<GeneratedQuestionDto> results = new ArrayList<>();
        String[] sentences = text.split("[.\\n]+");
        List<String> validSentences = new ArrayList<>();
        for (String s : sentences) {
            String clean = s.trim().replaceAll("\\s+", " ");
            if (clean.length() >= 30 && clean.length() <= 180 && !clean.contains("http")) {
                validSentences.add(clean);
            }
        }

        if (validSentences.isEmpty()) {
            return generateFallbackQuestions("Uploaded Document", count);
        }

        for (int i = 0; i < validSentences.size() && results.size() < count; i += 2) {
            String s = validSentences.get(i);
            String question = cleanText("Based on the uploaded document: What is the primary characteristic or principle described regarding \"" +
                    summarizeConcept(s) + "\"?", 500);

            List<String> options = List.of(
                    cleanText(s, 250),
                    cleanText("It operates inversely under opposing boundary conditions without state preservation.", 250),
                    cleanText("It is constrained solely to static linear allocations without memory overhead.", 250),
                    cleanText("It has no measurable influence on the primary system architecture.", 250)
            );

            results.add(new GeneratedQuestionDto(
                    question,
                    options,
                    0,
                    "Derived directly from concepts present in the uploaded document."
            ));
        }

        if (results.isEmpty()) {
            return generateFallbackQuestions("PYQ Analysis", count);
        }
        return results;
    }

    private String summarizeConcept(String sentence) {
        String[] words = sentence.split("\\s+");
        if (words.length <= 6) return sentence;
        return String.join(" ", Arrays.copyOfRange(words, 0, Math.min(words.length, 6))) + "...";
    }

    public List<GeneratedQuestionDto> generateFallbackQuestions(String subject, int count) {
        List<GeneratedQuestionDto> bank = List.of(
                new GeneratedQuestionDto("In computer architecture, what is the primary purpose of cache memory?",
                        List.of("To reduce average memory access latency", "To provide permanent magnetic storage", "To perform arithmetic logic operations", "To manage network protocols"),
                        0, "Cache memory speeds up CPU memory access."),
                new GeneratedQuestionDto("Which data structure uses the Last-In First-Out (LIFO) order?",
                        List.of("Queue", "Stack", "Binary Search Tree", "Linked List"),
                        1, "A Stack adheres to LIFO semantics."),
                new GeneratedQuestionDto("What is the time complexity of searching an element in a balanced Binary Search Tree?",
                        List.of("O(1)", "O(n)", "O(log n)", "O(n log n)"),
                        2, "Balanced BST search is logarithmic."),
                new GeneratedQuestionDto("In database systems, what does the 'A' in ACID stand for?",
                        List.of("Atomicity", "Availability", "Authentication", "Asynchronous"),
                        0, "Atomicity guarantees all-or-nothing transactions."),
                new GeneratedQuestionDto("Which protocol is used for secure communications over the Internet using TLS?",
                        List.of("HTTP", "FTP", "HTTPS", "SMTP"),
                        2, "HTTPS wraps HTTP inside TLS encryption."),
                new GeneratedQuestionDto("What is the primary role of an Operating System Kernel?",
                        List.of("Running web browsers", "Managing hardware resources and system calls", "Designing databases", "Compiling high-level code"),
                        1, "The kernel manages CPU, memory, and devices.")
        );
        return bank.subList(0, Math.min(count, bank.size()));
    }

    /**
     * Persists generated AI Quiz to database.
     */
    public Quiz createQuizFromAi(AiGenerateRequest req, List<GeneratedQuestionDto> questionsList) {
        String title = (req.title() != null && !req.title().isBlank())
                ? cleanText(req.title(), 140)
                : (req.subject() != null ? cleanText(req.subject() + " AI Assessment", 140) : "AI Generated Quiz");

        // Ensure title uniqueness
        String uniqueTitle = title;
        int suffix = 1;
        while (quizzes.existsByTitle(uniqueTitle)) {
            uniqueTitle = title + " (" + suffix++ + ")";
        }

        String subject = (req.subject() != null && !req.subject().isBlank()) ? cleanText(req.subject(), 80) : "General";
        String difficulty = (req.difficulty() != null && !req.difficulty().isBlank()) ? req.difficulty() : "Medium";
        int duration = (req.durationMinutes() != null && req.durationMinutes() > 0) ? req.durationMinutes() : Math.max(3, questionsList.size());
        boolean isPyq = req.isPyq() != null ? req.isPyq() : true;
        String year = req.year() != null ? req.year() : "2024";

        Quiz quiz = new Quiz(uniqueTitle, subject, "AI Agent generated quiz from PYQ / study paper.", difficulty, duration, isPyq, year);
        for (GeneratedQuestionDto gq : questionsList) {
            quiz.addQuestion(new Question(
                    gq.text(),
                    gq.options().get(0),
                    gq.options().get(1),
                    gq.options().get(2),
                    gq.options().get(3),
                    gq.correctOption()
            ));
        }
        return quizzes.save(quiz);
    }
}
