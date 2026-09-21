package com.internalmarketplace.api.report;

import com.internalmarketplace.api.notification.event.ReportCreatedEvent;
import com.internalmarketplace.api.post.PostRepository;
import com.internalmarketplace.api.post.dto.PostResponse;
import com.internalmarketplace.api.report.dto.ReportResponse;
import com.internalmarketplace.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        //
        // Resolving this per report used to mean up to 3 sequential Firestore
        // round trips per report (reporter lookup, target post lookup, post
        // author lookup) -- O(N) round trips end to end for N pending reports.
        // Instead, resolve every report in two batched phases: phase 1 looks up
        // all distinct reporters needing contact info AND all distinct target
        // posts in one combined round trip (they don't depend on each other);
        // phase 2 looks up all distinct post authors, which do depend on phase
        // 1's post results. That's 2 round trips total regardless of N, each
        // one bounded by the slowest single lookup rather than the sum of them.
        Set<String> reporterIdsNeedingContact = new LinkedHashSet<>();
        Set<String> postTargetIds = new LinkedHashSet<>();
        for (ReportResponse report : reports) {
            if (report.reporterEmail() == null || report.reporterName() == null) {
                reporterIdsNeedingContact.add(report.reporterId());
            }
            if ("post".equals(report.targetType())) {
                postTargetIds.add(report.targetId());
            }
        }

        Map<String, UserRepository.UserSummary> reporterByUid = userRepository.findSummariesByIds(reporterIdsNeedingContact);
        Map<String, PostResponse> postById = postRepository.findByIds(postTargetIds);

        Set<String> postAuthorIds = new LinkedHashSet<>();
        for (PostResponse post : postById.values()) {
            postAuthorIds.add(post.userId());
        }
        Map<String, UserRepository.UserSummary> postAuthorByUid = userRepository.findSummariesByIds(postAuthorIds);

        return reports.stream()
                .map(report -> withReporterContact(report, reporterByUid))
                .map(report -> withTarget(report, postById, postAuthorByUid))
                .toList();
    }

    private ReportResponse withReporterContact(ReportResponse report, Map<String, UserRepository.UserSummary> reporterByUid) {
        if (report.reporterEmail() != null && report.reporterName() != null) {
            return report;
        }
        UserRepository.UserSummary user = reporterByUid.get(report.reporterId());
        if (user == null) {
            return report;
        }
        return report.withReporterContact(
                report.reporterEmail() != null ? report.reporterEmail() : user.email(),
                report.reporterName() != null ? report.reporterName() : firstNonNull(user.name(), user.email()));
    }

    // So admins can identify the reported post and its creator at a glance instead of bare ids.
    private ReportResponse withTarget(ReportResponse report, Map<String, PostResponse> postById,
                                       Map<String, UserRepository.UserSummary> postAuthorByUid) {
        if (!"post".equals(report.targetType())) {
            return report;
        }
        PostResponse post = postById.get(report.targetId());
        if (post == null) {
            return report.withTarget("[deleted post]", null);
        }
        UserRepository.UserSummary author = postAuthorByUid.get(post.userId());
        String creatorName = author != null ? firstNonNull(author.name(), author.email()) : "Unknown";
        return report.withTarget(post.title(), creatorName);
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}
