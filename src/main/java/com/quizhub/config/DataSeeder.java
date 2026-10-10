package com.quizhub.config;

import com.quizhub.entity.*;
import com.quizhub.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Inserts demo users, quizzes and a few results on first start (only when tables are empty). */
@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users; private final QuizRepository quizzes;
    private final ResultRepository results; private final PasswordEncoder enc;

    public DataSeeder(UserRepository users, QuizRepository quizzes, ResultRepository results, PasswordEncoder enc) {
        this.users = users; this.quizzes = quizzes; this.results = results; this.enc = enc;
    }

    private User user(String name, String pass, String role) {
        return users.findByUsername(name).orElseGet(() -> users.save(new User(name, name + "@quizhub.in", enc.encode(pass), role)));
    }
    private static void q(Quiz z, String text, int correct, String a, String b, String c, String d) {
        z.addQuestion(new Question(text, a, b, c, d, correct));
    }

    @Override
    public void run(String... args) {
        user("admin", "admin123", "ADMIN");
        User demo = user("demo", "demo123", "STUDENT");
        User priya = user("priya", "pass123", "STUDENT");
        User arjun = user("arjun", "pass123", "STUDENT");
        if (quizzes.count() > 0) return;

        Quiz java = new Quiz("Java", "Core Java, classes and collections.", "Medium");
        q(java, "Which keyword is used to inherit a class in Java?", 1, "implements", "extends", "inherits", "super");
        q(java, "What is the size of an int in Java?", 2, "8 bits", "16 bits", "32 bits", "64 bits");
        q(java, "Which method is the entry point of a Java program?", 2, "start()", "run()", "main()", "init()");
        q(java, "Which collection does not allow duplicates?", 1, "List", "Set", "ArrayList", "Vector");

        Quiz sql = new Quiz("SQL", "Queries, joins and constraints.", "Easy");
        q(sql, "Which command retrieves data from a table?", 1, "GET", "SELECT", "FETCH", "OPEN");
        q(sql, "Which clause filters rows after grouping?", 2, "WHERE", "ORDER BY", "HAVING", "LIMIT");
        q(sql, "A PRIMARY KEY column can contain:", 2, "Duplicates", "NULL values", "Unique non-null values", "Only text");
        q(sql, "Which join returns only matching rows?", 0, "INNER JOIN", "LEFT JOIN", "CROSS JOIN", "FULL JOIN");

        Quiz oop = new Quiz("OOP", "Objects, inheritance and polymorphism.", "Easy");
        q(oop, "Wrapping data and methods in one unit is called:", 1, "Inheritance", "Encapsulation", "Polymorphism", "Abstraction");
        q(oop, "Same method name, different behavior is:", 0, "Polymorphism", "Encapsulation", "Compilation", "Linking");
        q(oop, "A blueprint for creating objects is a:", 2, "Method", "Package", "Class", "Variable");
        q(oop, "Hiding implementation details is:", 0, "Abstraction", "Overloading", "Casting", "Looping");

        Quiz electronics = new Quiz("Electronics", "Diodes, transistors and basic semiconductor devices.", "Medium");
        q(electronics, "Which device allows current flow in one direction only?", 2, "Resistor", "Inductor", "Diode", "Capacitor");
        q(electronics, "A transistor is mainly used as a:", 1, "Current source", "Switch or amplifier", "Voltage regulator", "Fuse");
        q(electronics, "Which semiconductor material is widely used in ICs?", 0, "Silicon", "Copper", "Aluminum", "Gold");
        q(electronics, "The process of adding impurities to a semiconductor is called:", 3, "Biasing", "Rectification", "Coupling", "Doping");

        Quiz digital = new Quiz("Digital Electronics", "Logic gates, Boolean algebra and combinational circuits.", "Medium");
        q(digital, "Which gate outputs 1 only when both inputs are 1?", 3, "OR", "XOR", "NOT", "AND");
        q(digital, "The Boolean expression A + A' equals:", 0, "1", "0", "A", "A'");
        q(digital, "A flip-flop stores:", 2, "Only one state", "No data", "One bit of data", "A voltage source");
        q(digital, "Which logic family is known for low power consumption?", 1, "TTL", "CMOS", "RTL", "DTL");

        Quiz communication = new Quiz("Communication Systems", "Modulation, bandwidth and transmission basics.", "Hard");
        q(communication, "Amplitude modulation changes the signal's:", 1, "Frequency", "Amplitude", "Phase", "Noise level");
        q(communication, "The bandwidth of a signal indicates:", 2, "Its power", "Its resistance", "Frequency range used", "Data storage");
        q(communication, "Nyquist theorem is related to:", 0, "Sampling rate", "Diode recovery", "LC tuning", "Gate delay");
        q(communication, "The main purpose of a modulator is to:", 3, "Store data", "Amplify DC", "Stabilize voltage", "Transfer information onto a carrier");

        Quiz signals = new Quiz("Signals & Systems", "Transforms, convolution and LTI systems.", "Hard");
        q(signals, "The Fourier transform converts a time-domain signal into:", 2, "Logic form", "Frequency-domain form", "Binary code", "Power supply form");
        q(signals, "An LTI system is characterized by:", 1, "Random behavior", "Linearity and time invariance", "Non-repeating output", "High delay only");
        q(signals, "Convolution is mainly used to analyze:", 0, "System response", "Battery life", "Heat dissipation", "Signal clipping");
        q(signals, "Laplace transform is especially useful for:", 3, "Static memory reading", "Analog-to-digital conversion", "Digital logic simplification", "Solving differential equations");

        java = quizzes.save(java); sql = quizzes.save(sql); oop = quizzes.save(oop);
        electronics = quizzes.save(electronics); digital = quizzes.save(digital); communication = quizzes.save(communication); signals = quizzes.save(signals);
        results.save(new Result(demo, java, 3, 4));
        results.save(new Result(demo, sql, 4, 4));
        results.save(new Result(demo, electronics, 3, 4));
        results.save(new Result(priya, java, 4, 4));
        results.save(new Result(priya, sql, 4, 4));
        results.save(new Result(arjun, oop, 3, 4));
        results.save(new Result(arjun, digital, 2, 4));
    }
}
