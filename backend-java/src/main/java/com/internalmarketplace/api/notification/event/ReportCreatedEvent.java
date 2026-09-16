package com.internalmarketplace.api.notification.event;

/** Published after a report is persisted; replaces functions/src/index.js's onReportCreate. */
public record ReportCreatedEvent(String reportId, String targetType, String targetId, String reason) {
}
