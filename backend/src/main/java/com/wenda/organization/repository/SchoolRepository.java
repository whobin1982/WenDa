package com.wenda.organization.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SchoolRepository {

    public record SchoolRow(UUID id, String schoolCode, String name, String shortName,
                            String status, String contactEmail, String contactPhone,
                            String address, String description, UUID tenantId,
                            OffsetDateTime createdAt, OffsetDateTime updatedAt, long version) {}

    private final JdbcTemplate jdbc;

    public SchoolRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean existsByCode(String code) {
        Integer c = jdbc.query("SELECT 1 FROM schools WHERE school_code = ?",
                rs -> rs.next() ? 1 : 0, code);
        return c != null && c == 1;
    }

    public SchoolRow create(String code, String name, String shortName,
                            String contactEmail, String contactPhone, String address,
                            String description, UUID tenantId, UUID createdBy) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO schools (id, school_code, name, short_name, contact_email, contact_phone, "
                        + "address, description, tenant_id, created_by, updated_by, version) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,0)",
                id, code, name, shortName, contactEmail, contactPhone, address, description,
                tenantId, createdBy, createdBy);
        return findById(id).orElseThrow();
    }

    public Optional<SchoolRow> findById(UUID id) {
        var rows = jdbc.query(
                "SELECT id, school_code, name, short_name, status, contact_email, contact_phone, "
                        + "address, description, tenant_id, created_at, updated_at, version "
                        + "FROM schools WHERE id = ? AND archived_at IS NULL",
                (rs, i) -> map(rs),
                id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Optional<SchoolRow> findByCode(String code) {
        var rows = jdbc.query(
                "SELECT id, school_code, name, short_name, status, contact_email, contact_phone, "
                        + "address, description, tenant_id, created_at, updated_at, version "
                        + "FROM schools WHERE school_code = ? AND archived_at IS NULL",
                (rs, i) -> map(rs),
                code);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public int update(UUID id, String name, String shortName, String contactEmail,
                      String contactPhone, String address, String description, long expectedVersion,
                      UUID updatedBy) {
        return jdbc.update(
                "UPDATE schools SET name = ?, short_name = ?, contact_email = ?, contact_phone = ?, "
                        + "address = ?, description = ?, updated_by = ?, updated_at = now(), "
                        + "version = version + 1 "
                        + "WHERE id = ? AND version = ? AND archived_at IS NULL",
                name, shortName, contactEmail, contactPhone, address, description, updatedBy,
                id, expectedVersion);
    }

    private static SchoolRow map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new SchoolRow(
                (UUID) rs.getObject("id"),
                rs.getString("school_code"),
                rs.getString("name"),
                rs.getString("short_name"),
                rs.getString("status"),
                rs.getString("contact_email"),
                rs.getString("contact_phone"),
                rs.getString("address"),
                rs.getString("description"),
                (UUID) rs.getObject("tenant_id"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class),
                rs.getLong("version"));
    }
}
