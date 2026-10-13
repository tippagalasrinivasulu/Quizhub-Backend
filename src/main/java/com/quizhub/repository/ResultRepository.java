package com.quizhub.repository;

import com.quizhub.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findByUserUsernameOrderByTakenAtDesc(String username);

    List<Result> findByUserEmailOrderByTakenAtDesc(String email);

    List<Result> findTop20ByOrderByTakenAtDesc();

    /** [subject/title, average percent, attempts] for one user */
    @Query("select coalesce(r.quiz.subject, r.quiz.title), avg(r.score * 100.0 / r.total), count(r) " +
           "from Result r where r.user.username = :u group by coalesce(r.quiz.subject, r.quiz.title)")
    List<Object[]> subjectStats(@Param("u") String username);

    /** [username, quizzes taken, total points, total xp, user total xp] ordered by xp, excluding ADMIN */
    @Query("select r.user.username, count(r), sum(r.score), sum(r.xpEarned), coalesce(max(r.user.xp), 0L) " +
           "from Result r where r.user.role <> 'ADMIN' group by r.user.username order by max(r.user.xp) desc, sum(r.score) desc")
    List<Object[]> leaderboard();

    long countByUserUsername(String username);

    long countByUserEmail(String email);
}
