package com.quizhub.repository;

import com.quizhub.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findByUserUsernameOrderByTakenAtDesc(String username);

    /** [quiz title, average percent, attempts] for one user */
    @Query("select r.quiz.title, avg(r.score * 100.0 / r.total), count(r) " +
           "from Result r where r.user.username = :u group by r.quiz.title")
    List<Object[]> subjectStats(@Param("u") String username);

    /** [username, quizzes taken, total points] ordered by points */
    @Query("select r.user.username, count(r), sum(r.score) " +
           "from Result r group by r.user.username order by sum(r.score) desc")
    List<Object[]> leaderboard();
}
