package com.internalmarketplace.api.report.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ReportResponse(
        String id,
        String reporterId,
        String reporterEmail,
        String reporterName,
        String targetType,
        String targetId,
        String reason,
        String status,
        String reviewedBy,
        Timestamp createdAt,
        Timestamp updatedAt,
        String targetTitle,
        String targetCreatorName
) {
    public static ReportResponse fromSnapshot(DocumentSnapshot doc) {
        return new ReportResponse(
                doc.getId(),
                doc.getString("reporter_id"),
                doc.getString("reporter_email"),
                doc.getString("reporter_name"),
                doc.getString("target_type"),
                doc.getString("target_id"),
                doc.getString("reason"),
                doc.getString("status"),
                doc.getString("reviewed_by"),
                doc.getTimestamp("created_at"),
                doc.getTimestamp("updated_at"),
                null,
                null);
    }

    public ReportResponse withReporterContact(String email, String name) {
        return new ReportResponse(id, reporterId, email, name, targetType, targetId, reason, status, reviewedBy,
                createdAt, updatedAt, targetTitle, targetCreatorName);
    }

    public ReportResponse withTarget(String title, String creatorName) {
        return new ReportResponse(id, reporterId, reporterEmail, reporterName, targetType, targetId, reason, status,
                reviewedBy, createdAt, updatedAt, title, creatorName);
    }
}
