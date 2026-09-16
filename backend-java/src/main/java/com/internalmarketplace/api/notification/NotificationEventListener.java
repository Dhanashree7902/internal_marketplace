package com.internalmarketplace.api.notification;

import com.internalmarketplace.api.notification.event.CategoryRequestCreatedEvent;
import com.internalmarketplace.api.notification.event.CommentCreatedEvent;
import com.internalmarketplace.api.notification.event.ReportCreatedEvent;
import com.internalmarketplace.api.post.PostRepository;
import com.internalmarketplace.api.post.dto.PostResponse;
import com.internalmarketplace.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Replaces the Firestore-triggered Cloud Functions in functions/src/index.js
 * with in-process, async domain-event listeners: same fan-out logic, same
 * notification documents, but no separate deployment target and no
 * cross-service trigger latency. Runs on the virtual-thread executor
 * configured in AsyncConfig, so it never competes with request-handling
 * threads.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PostRepository postRepository;

    // CATEGORY_REQUESTED -> notify admins (spec Section 11).
    @Async
    @EventListener
    public void onCategoryRequestCreated(CategoryRequestCreatedEvent event) {
        List<String> admins = userRepository.findActiveAdminUids();
        String message = event.requestedBy() + " requested a new category: \"" + event.proposedName() + "\".";
        notificationRepository.createForMany(admins, "CATEGORY_REQUESTED", event.requestId(), message);
    }

    // POST_REPORTED -> create/notify a moderation case for admins.
    @Async
    @EventListener
    public void onReportCreated(ReportCreatedEvent event) {
        List<String> admins = userRepository.findActiveAdminUids();
        String message;
        Optional<PostResponse> postOpt = "post".equals(event.targetType())
                ? postRepository.findById(event.targetId())
                : Optional.empty();
        if (postOpt.isPresent()) {
            PostResponse post = postOpt.get();
            String creatorName = userRepository.findSummaryById(post.userId())
                    .map(user -> firstNonNull(user.name(), user.email()))
                    .orElse("Unknown");
            message = "New report submitted for the post \"" + post.title() + "\" created by " + creatorName
                    + ": \"" + event.reason() + "\"";
        } else {
            message = "New report on " + event.targetType() + " " + event.targetId() + ": " + event.reason();
        }
        notificationRepository.createForMany(admins, "POST_REPORTED", event.reportId(), message);
    }

    // COMMENT_CREATED -> notify the post owner (unless commenting on their own post).
    @Async
    @EventListener
    public void onCommentCreated(CommentCreatedEvent event) {
        Optional<PostResponse> postOpt = postRepository.findById(event.postId());
        if (postOpt.isEmpty()) {
            return;
        }
        PostResponse post = postOpt.get();
        if (post.userId().equals(event.authorUid())) {
            return;
        }
        String message = "New comment on your post \"" + post.title() + "\".";
        notificationRepository.create(post.userId(), "COMMENT_CREATED", event.postId(), message);
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}
