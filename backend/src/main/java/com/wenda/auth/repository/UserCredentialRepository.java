package com.wenda.auth.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserCredentialRepository {

    public record CredentialRow(UUID userId, String passwordHash, String passwordAlgo,
                                OffsetDateTime lastChangedAt, int failedAttempts,
                                OffsetDateTime lockedUntil) {}

    private final JdbcTemplate jdbc;

    public UserCredentialRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<CredentialRow> findByUserId(UUID userId) {
        var rows = jdbc.query(
                "SELECT user_id, password_hash, password_algo, last_changed_at, failed_attempts, locked_until "
                        + "FROM user_credentials WHERE user_id = ?",
                (rs, i) -> new CredentialRow(
                        (UUID) rs.getObject("user_id"),
                        rs.getString("password_hash"),
                        rs.getString("password_algo"),
                        rs.getObject("last_changed_at", OffsetDateTime.class),
                        rs.getInt("failed_attempts"),
                        rs.getObject("locked_until", OffsetDateTime.class)),
                userId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public void incrementFailedAttempts(UUID userId) {
        jdbc.update(
                "UPDATE user_credentials SET failed_attempts = failed_attempts + 1, "
                        + "locked_until = CASE WHEN failed_attempts + 1 >= 5 "
                        + "THEN now() + interval '15 minutes' ELSE locked_until END WHERE user_id = ?",
                userId);
    }

    public void resetFailedAttempts(UUID userId) {
        jdbc.update(
                "UPDATE user_credentials SET failed_attempts = 0, locked_until = NULL, last_changed_at = last_changed_at "
                        + "WHERE user_id = ?",
                userId);
    }

    public void updatePasswordHash(UUID userId, String hash) {
        jdbc.update(
                "UPDATE user_credentials SET password_hash = ?, password_algo = 'BCRYPT', "
                        + "failed_attempts = 0, locked_until = NULL, last_changed_at = now() WHERE user_id = ?",
                hash, userId);
    }
}
