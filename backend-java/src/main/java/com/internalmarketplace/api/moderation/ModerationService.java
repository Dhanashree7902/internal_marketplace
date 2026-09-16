package com.internalmarketplace.api.moderation;

import com.internalmarketplace.api.common.exception.NotFoundException;
import com.internalmarketplace.api.notification.NotificationRepository;
import com.internalmarketplace.api.post.PostRepository;
import com.internalmarketplace.api.post.dto.PostResponse;
import com.internalmarketplace.api.report.ReportRepository;
import com.internalmarketplace.api.report.dto.ReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Admin moderation of any post/report, distinct from the owner-scoped
 * PATCH /posts/{id}. A direct port of moderation.controller.js (which has no
 * separate service file in the Node app; this class exists purely to keep
 * ModerationController thin without changing any behavior).
 */
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final PostRepository postRepository;
    private final ReportRepository reportRepository;
    private final NotificationRepository notificationRepository;

    public PostResponse moderatePost(String postId, String status) {
        PostResponse post = postRepository.findById(postId).orElseThrow(() -> new NotFoundException("Post not found"));
        postRepository.updateStatus(postId, status);

        String message = "Your post \"" + post.title() + "\" was set to " + status + " by a moderator.";
        notificationRepository.create(post.userId(), "POST_MODERATED", postId, message);

        return post;
    }

    public ReportResponse reviewReport(String reportId, String status, String reviewedBy) {
        ReportResponse report = reportRepository.findById(reportId).orElseThrow(() -> new NotFoundException("Report not found"));
        reportRepository.updateReview(reportId, status, reviewedBy);
        return report;
    }
}
