package com.quizhub.controller;

import com.quizhub.dto.Dtos.*;
import com.quizhub.service.QuizService;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class QuizController {
    private final QuizService svc;
    public QuizController(QuizService svc) { this.svc = svc; }

    @GetMapping("/quizzes")              public List<QuizSummary> quizzes() { return svc.list(); }
    @GetMapping("/quizzes/{id}/start")   public QuizDetail start(@PathVariable Long id) { return svc.start(id); }
    @PostMapping("/quizzes/{id}/submit") public ResultResponse submit(@PathVariable Long id, @RequestBody SubmitRequest body, Principal p) {
        return svc.submit(id, p.getName(), body.answers());
    }
    @GetMapping("/results/me")           public List<HistoryItem> history(Principal p) { return svc.history(p.getName()); }
    @GetMapping("/results/stats")        public List<SubjectStat> stats(Principal p) { return svc.stats(p.getName()); }
    @GetMapping("/leaderboard")          public List<LeaderboardRow> leaderboard() { return svc.leaderboard(); }
}
