package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserStatusRepository extends JpaRepository<UserStatus, UUID> {
    @Query(value = "SELECT us FROM UserStatus us " +
            "LEFT JOIN FETCH us.user " +
            "WHERE us.id = :id")
    Optional<UserStatus> findByIdWithUser(@Param("id") UUID id);

    @Query(value = "SELECT us FROM UserStatus us " +
            "LEFT JOIN FETCH us.user " +
            "WHERE us.user.id = :userId")
    Optional<UserStatus> findByUserIdWithUser(@Param("userId") UUID userId);

    @Query(value = "SELECT us FROM UserStatus us " +
            "LEFT JOIN FETCH us.user")
    List<UserStatus> findAllWithUser();

    @Modifying
    @Query(value = "DELETE FROM UserStatus AS us WHERE us.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    void deleteByUser_Id(UUID userId);

    boolean existsUserStatusByUserId(UUID userId);
}
