package com.peraton.cicd.repository;

import com.peraton.cicd.model.Build;
import com.peraton.cicd.model.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface BuildRepository extends JpaRepository<Build, Long> {

    List<Build> findByRepository(Repository repository);

    List<Build> findByRepositoryId(Long repositoryId);

    List<Build> findByStatus(Build.BuildStatus status);

    Optional<Build> findByGithubRunId(Long githubRunId);

    List<Build> findTop10ByRepositoryIdOrderByStartedAtDesc(Long repositoryId);

    @Query("SELECT b FROM Build b WHERE b.repository.id = :repositoryId ORDER BY b.startedAt DESC")
    List<Build> findByRepositoryIdOrderByStartedAtDesc(@Param("repositoryId") Long repositoryId);

    @Query("SELECT b FROM Build b WHERE b.status = :status AND b.repository.id = :repositoryId")
    List<Build> findByRepositoryIdAndStatus(@Param("repositoryId") Long repositoryId,
                                            @Param("status") Build.BuildStatus status);

    @Query("SELECT b FROM Build b WHERE b.startedAt BETWEEN :startDate AND :endDate ORDER BY b.startedAt DESC")
    List<Build> findBuildsBetweenDates(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT b FROM Build b WHERE b.repository.id = :repositoryId ORDER BY b.startedAt DESC")
    List<Build> findLatestBuildsByRepository(@Param("repositoryId") Long repositoryId);

    @Query("SELECT COUNT(b) FROM Build b WHERE b.repository.id = :repositoryId AND b.status = :status")
    Long countByRepositoryIdAndStatus(@Param("repositoryId") Long repositoryId,
                                      @Param("status") Build.BuildStatus status);
}
