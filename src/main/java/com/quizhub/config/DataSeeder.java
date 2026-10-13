package com.quizhub.config;

import com.quizhub.entity.*;
import com.quizhub.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Initializes the system:
 * - Guarantees admin account (tippagalasrinivasulu@gmail.com / srinu123).
 * - Clears all dummy test students (priya, arjun, demo, sneha) and their dummy results.
 * - Seeds starter quizzes for students to take if no quizzes exist.
 */
@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final QuizRepository quizzes;
    private final ResultRepository results;
    private final PasswordEncoder enc;

    public DataSeeder(UserRepository users, QuizRepository quizzes, ResultRepository results, PasswordEncoder enc) {
        this.users = users;
        this.quizzes = quizzes;
        this.results = results;
        this.enc = enc;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Purge all default dummy students and their results
        clearDummyStudents();

        // 2. Setup the official Admin account
        setupAdminUser();

        // 3. Seed starter quizzes (without any dummy results) if empty
        if (quizzes.count() == 0) {
            seedStarterQuizzes();
        }
    }

    private void clearDummyStudents() {
        // Clear all dummy/demo accounts and their results
        List<String> dummyNames = List.of("demo", "priya", "arjun", "sneha", "ram", "student", "test");
        for (User u : users.findAll()) {
            if ("ADMIN".equalsIgnoreCase(u.getRole())) continue;
            String lowerUser = u.getUsername().toLowerCase();
            String lowerEmail = (u.getEmail() != null ? u.getEmail() : "").toLowerCase();
            boolean isDummy = dummyNames.stream().anyMatch(d -> lowerUser.contains(d) || lowerEmail.contains(d));
            if (isDummy) {
                List<Result> userResults = results.findByUserUsernameOrderByTakenAtDesc(u.getUsername());
                results.deleteAll(userResults);
                users.delete(u);
            }
        }
    }

    private User setupAdminUser() {
        String adminEmail = "tippagalasrinivasulu@gmail.com";
        String adminPass = "srinu123";

        User u = users.findByEmail(adminEmail)
                .or(() -> users.findByUsername("srinu"))
                .or(() -> users.findByUsername("admin"))
                .orElse(null);

        if (u == null) {
            u = new User("srinu", adminEmail, enc.encode(adminPass), "ADMIN");
        } else {
            u.setUsername("srinu");
            u.setEmail(adminEmail);
            u.setPassword(enc.encode(adminPass));
            u.setRole("ADMIN");
        }
        u.setXp(0L);
        users.save(u);

        // Permanently clear ANY past quiz attempts and results under admin accounts
        List<Result> adminResults = results.findByUserUsernameOrderByTakenAtDesc(u.getUsername());
        if (!adminResults.isEmpty()) {
            results.deleteAll(adminResults);
        }
        List<Result> adminEmailResults = results.findByUserEmailOrderByTakenAtDesc(adminEmail);
        if (!adminEmailResults.isEmpty()) {
            results.deleteAll(adminEmailResults);
        }
        return u;
    }

    private static void q(Quiz z, String text, int correct, String a, String b, String c, String d) {
        z.addQuestion(new Question(text, a, b, c, d, correct));
    }

    private void seedStarterQuizzes() {
        // 1. Subject: Computer Science - Java
        Quiz java = new Quiz("Java Advanced & Core", "Computer Science", "Core Java, OOP, collections, multithreading.", "Medium", 5, false, null);
        q(java, "Which keyword is used to inherit a class in Java?", 1, "implements", "extends", "inherits", "super");
        q(java, "What is the size of an int in Java?", 2, "8 bits", "16 bits", "32 bits", "64 bits");
        q(java, "Which method is the entry point of a Java program?", 2, "start()", "run()", "main()", "init()");
        q(java, "Which collection does not allow duplicates?", 1, "List", "Set", "ArrayList", "Vector");
        q(java, "Which interface must be implemented for thread execution?", 0, "Runnable", "Cloneable", "Serializable", "Comparable");

        // 2. Subject: Computer Science - Database Systems
        Quiz dbms = new Quiz("Database Management & SQL", "Computer Science", "SQL queries, relational algebra, indexing, and normalization.", "Easy", 4, false, null);
        q(dbms, "Which command retrieves data from a table?", 1, "GET", "SELECT", "FETCH", "OPEN");
        q(dbms, "Which clause filters rows after grouping?", 2, "WHERE", "ORDER BY", "HAVING", "LIMIT");
        q(dbms, "A PRIMARY KEY column must contain:", 2, "Duplicates", "NULL values", "Unique non-null values", "Only numbers");
        q(dbms, "Which join returns only rows with matches in both tables?", 0, "INNER JOIN", "LEFT JOIN", "CROSS JOIN", "FULL JOIN");

        // 3. Subject: Computer Science - Operating Systems
        Quiz os = new Quiz("Operating Systems & Concurrency", "Computer Science", "Process scheduling, deadlocks, virtual memory, and page replacement.", "Hard", 6, false, null);
        q(os, "Which condition is NOT one of Coffman's four deadlock conditions?", 3, "Mutual Exclusion", "Hold and Wait", "No Preemption", "Starvation");
        q(os, "Which scheduling algorithm is non-preemptive?", 1, "Round Robin", "First Come First Served (FCFS)", "SRTF", "Multilevel Feedback");
        q(os, "Thrashing occurs when:", 2, "CPU is idling", "A deadlock is resolved", "Page fault rate is excessively high", "RAM is fully wiped");

        // 4. Subject: Data Structures - Algorithms
        Quiz dsa = new Quiz("Data Structures & Algorithms", "Data Structures", "Trees, graphs, dynamic programming, and complexity analysis.", "Hard", 8, false, null);
        q(dsa, "What is the average time complexity of QuickSort?", 1, "O(n)", "O(n log n)", "O(n^2)", "O(log n)");
        q(dsa, "Which data structure is best for Breadth-First Search (BFS)?", 2, "Stack", "Priority Queue", "Queue", "Heap");
        q(dsa, "Which algorithm finds the Single-Source Shortest Path with non-negative weights?", 3, "Kruskal's", "Prim's", "Floyd-Warshall", "Dijkstra's");

        // 5. Subject: Electronics - Digital Electronics
        Quiz digital = new Quiz("Digital Electronics & Logic", "Electronics", "Logic gates, Boolean algebra, flip-flops, and counters.", "Medium", 5, false, null);
        q(digital, "Which gate outputs 1 only when both inputs are 1?", 3, "OR", "XOR", "NOT", "AND");
        q(digital, "The Boolean expression A + A' equals:", 0, "1", "0", "A", "A'");
        q(digital, "A flip-flop stores:", 2, "Continuous wave", "Analog voltage", "One bit of binary data", "No data");

        // 6. Subject: PYQ Papers - GATE CSE 2024
        Quiz gatePyq = new Quiz("GATE CSE 2024 Previous Paper", "PYQ Papers", "Official previous year paper questions from Computer Science GATE exam.", "Hard", 10, true, "GATE 2024");
        q(gatePyq, "Consider a computer with 48-bit virtual address and 4 KB page size. How many pages are in the virtual address space?", 2, "2^32", "2^40", "2^36", "2^48");
        q(gatePyq, "In relational algebra, which operation is NOT primitive?", 1, "Selection", "Natural Join", "Projection", "Cartesian Product");
        q(gatePyq, "TCP uses which protocol mechanism to avoid network congestion?", 0, "Slow Start & Congestion Avoidance", "Token Ring", "ALOHA", "Stop-and-Wait ARQ");

        quizzes.save(java);
        quizzes.save(dbms);
        quizzes.save(os);
        quizzes.save(dsa);
        quizzes.save(digital);
        quizzes.save(gatePyq);
    }
}
