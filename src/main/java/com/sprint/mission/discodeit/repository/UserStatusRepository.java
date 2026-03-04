package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserStatusRepository extends JpaRepository<UserStatus, UUID> {
    Optional<UserStatus> findByUserId(UUID userId);

    @Modifying
    @Query(value = "DELETE FROM UserStatus AS us WHERE us.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    void deleteByUser_Id(UUID userId);

    boolean existsUserStatusByUser_Id(UUID userId);
}
