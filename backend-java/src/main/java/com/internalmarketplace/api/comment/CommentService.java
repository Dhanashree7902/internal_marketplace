package com.internalmarketplace.api.comment;

import com.internalmarketplace.api.comment.dto.CommentResponse;
import com.internalmarketplace.api.notification.event.CommentCreatedEvent;
import com.internalmarketplace.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Business rules for comments, a direct port of comments.service.js. */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<CommentResponse> listComments(String postId) {
        List<CommentResponse> comments = commentRepository.listByPost(postId);

        // Older comments (or ones from accounts that had no stored email at the
        // time) won't have user_email set -- resolve it live from the users
        // collection instead of showing "Unknown user" whenever it's avoidable.
        Set<String> missingUserIds = new LinkedHashSet<>();
        for (CommentResponse comment : comments) {
            if (comment.userEmail() == null) {
                missingUserIds.add(comment.userId());
            }
        }
        if (missingUserIds.isEmpty()) {
            return comments;
        }

        // One combined round trip for every commenter missing an email instead of
        // one sequential Firestore get per commenter (see UserRepository#findEmailsByIds).
        Map<String, String> emailByUid = userRepository.findEmailsByIds(missingUserIds);

        return comments.stream()
                .map(c -> c.userEmail() == null ? c.withUserEmail(emailByUid.get(c.userId())) : c)
                .toList();
    }

    public String createComment(String postId, String userId, String userEmail, String text) {
        String id = commentRepository.create(postId, userId, userEmail, text);
        eventPublisher.publishEvent(new CommentCreatedEvent(id, postId, userId));
        return id;
    }
}
