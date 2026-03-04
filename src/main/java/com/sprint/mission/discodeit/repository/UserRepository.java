package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

// 데이터 관련 로직(저장, 조회, 삭제 등등) 담당
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);


    @Query(value = "SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END\n" +
            "FROM User AS u\n" +
            "WHERE u.email = :email\n" +
            "  AND u.id != :userId\n")
    boolean isEmailUsedByOther(@Param("userId") UUID userId, @Param("email") String newEmail);

    @Query(value = "SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END\n" +
            "FROM User AS u\n" +
            "WHERE u.username = :username\n" +
            "  AND u.id != :userId\n")
    boolean isUserNameUsedByOther(@Param("userId") UUID userId, @Param("username") String newUsername);
}
