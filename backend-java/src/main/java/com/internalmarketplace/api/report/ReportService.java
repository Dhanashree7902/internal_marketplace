package com.internalmarketplace.api.report;

import com.internalmarketplace.api.notification.event.ReportCreatedEvent;
import com.internalmarketplace.api.post.PostRepository;
import com.internalmarketplace.api.report.dto.ReportResponse;
import com.internalmarketplace.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

/** Business rules for reports, a direct port of reports.service.js. */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;

    public String createReport(String reporterId, String reporterEmail, String reporterName, String targetType,
                                String targetId, String reason) {
        String id = reportRepository.create(reporterId, reporterEmail, reporterName, targetType, targetId, reason);
        eventPublisher.publishEvent(new ReportCreatedEvent(id, targetType, targetId, reason));
        return id;
    }

    public List<ReportResponse> listPendingReports() {
        List<ReportResponse> reports = reportRepository.listPending();

        // Older reports (or ones from accounts with no stored email/name at the
        // time) won't have reporter_email/reporter_name set -- resolve live.
        return reports.stream()
                .map(this::withReporterContact)
                .map(this::withTarget)
                .toList();
    }

    private ReportResponse withReporterContact(ReportResponse report) {
        if (report.reporterEmail() != null && report.reporterName() != null) {
            return report;
        }
        return userRepository.findSummaryById(report.reporterId())
                .map(user -> report.withReporterContact(
                        report.reporterEmail() != null ? report.reporterEmail() : user.email(),
                        report.reporterName() != null ? report.reporterName()
                                : firstNonNull(user.name(), user.email())))
                .orElse(report);
    }

    // So admins can identify the reported post and its creator at a glance instead of bare ids.
    private ReportResponse withTarget(ReportResponse report) {
        if (!"post".equals(report.targetType())) {
            return report;
        }
        return postRepository.findById(report.targetId())
                .map(post -> {
                    String creatorName = userRepository.findSummaryById(post.userId())
                            .map(user -> firstNonNull(user.name(), user.email()))
                            .orElse("Unknown");
                    return report.withTarget(post.title(), creatorName);
                })
                .orElse(report.withTarget("[deleted post]", null));
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}
