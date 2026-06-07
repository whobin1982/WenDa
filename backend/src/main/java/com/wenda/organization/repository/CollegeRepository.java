package com.wenda.organization.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CollegeRepository {

    public record CollegeRow(UUID id, UUID schoolId, UUID tenantId, String collegeCode,
                             String name, String shortName, String description, String status,
                             OffsetDateTime createdAt, OffsetDateTime updatedAt, long version) {}

    private final JdbcTemplate jdbc;

    public CollegeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public CollegeRow create(UUID schoolId, UUID tenantId, String code, String name, String shortName,
                             String description, UUID createdBy) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO colleges (id, school_id, tenant_id, college_code, name, short_name, "
                        + "description, created_by, updated_by, version) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,0)",
                id, schoolId, tenantId, code, name, shortName, description, createdBy, createdBy);
        return findById(id).orElseThrow();
    }

    public Optional<CollegeRow> findById(UUID id) {
        var rows = jdbc.query(
                "SELECT id, school_id, tenant_id, college_code, name, short_name, description, status, "
                        + "created_at, updated_at, version FROM colleges WHERE id = ? AND archived_at IS NULL",
                (rs, i) -> map(rs),
                id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public List<CollegeRow> listBySchool(UUID schoolId, int page, int pageSize) {
        return jdbc.query(
                "SELECT id, school_id, tenant_id, college_code, name, short_name, description, status, "
                        + "created_at, updated_at, version FROM colleges "
                        + "WHERE school_id = ? AND archived_at IS NULL "
                        + "ORDER BY created_at DESC LIMIT ? OFFSET ?",
                (rs, i) -> map(rs),
                schoolId, pageSize, (page - 1) * pageSize);
    }

    public long countBySchool(UUID schoolId) {
        Long n = jdbc.queryForObject(
                "SELECT count(*) FROM colleges WHERE school_id = ? AND archived_at IS NULL",
                Long.class, schoolId);
        return n == null ? 0 : n;
    }

    public int update(UUID id, String name, String shortName, String description, long expectedVersion,
                      UUID updatedBy) {
        return jdbc.update(
                "UPDATE colleges SET name = ?, short_name = ?, description = ?, updated_by = ?, "
                        + "updated_at = now(), version = version + 1 "
                        + "WHERE id = ? AND version = ? AND archived_at IS NULL",
                name, shortName, description, updatedBy, id, expectedVersion);
    }

    private static CollegeRow map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new CollegeRow(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("school_id"),
                (UUID) rs.getObject("tenant_id"),
                rs.getString("college_code"),
                rs.getString("name"),
                rs.getString("short_name"),
                rs.getString("description"),
                rs.getString("status"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class),
                rs.getLong("version"));
    }
}
