package com.BlackDot.Finance.Tracker.UserActivity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserActivityRepository extends JpaRepository<UserActivity, UUID> {

    List<UserActivity> findByUserIdAndActivityDateBetweenOrderByActivityDate(
            UUID userId, LocalDate from, LocalDate to);

    @Modifying
    @Query(value = """
            INSERT INTO user_activity
                (id, user_id, activity_date, transaction_count, login_count,
                 created_at, updated_at, version)
            VALUES (:id, :userId, :activityDate, 1, 0, now(), now(), 0)
            ON CONFLICT (user_id, activity_date)
            DO UPDATE SET transaction_count = user_activity.transaction_count + 1,
                          updated_at = now(),
                          version = user_activity.version + 1
            """, nativeQuery = true)
    void incrementTransaction(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("activityDate") LocalDate activityDate);

    @Modifying
    @Query(value = """
            INSERT INTO user_activity
                (id, user_id, activity_date, transaction_count, login_count,
                 created_at, updated_at, version)
            VALUES (:id, :userId, :activityDate, 0, 1, now(), now(), 0)
            ON CONFLICT (user_id, activity_date)
            DO UPDATE SET login_count = user_activity.login_count + 1,
                          updated_at = now(),
                          version = user_activity.version + 1
            """, nativeQuery = true)
    void recordLogin(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("activityDate") LocalDate activityDate);

    @Modifying
    @Query(value = """
            UPDATE user_activity
            SET transaction_count = transaction_count - 1,
                updated_at = now(),
                version = version + 1
            WHERE user_id = :userId
              AND activity_date = :activityDate
              AND transaction_count > 0
            """, nativeQuery = true)
    void decrementTransaction(
            @Param("userId") UUID userId,
            @Param("activityDate") LocalDate activityDate);

    @Modifying
    @Query(value = """
            DELETE FROM user_activity
            WHERE user_id = :userId
              AND activity_date = :activityDate
              AND transaction_count = 0
              AND login_count = 0
            """, nativeQuery = true)
    void deleteIfInactive(
            @Param("userId") UUID userId,
            @Param("activityDate") LocalDate activityDate);
}
