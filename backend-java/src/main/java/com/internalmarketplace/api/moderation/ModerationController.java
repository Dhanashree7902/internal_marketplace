package com.internalmarketplace.api.moderation;

import com.internalmarketplace.api.common.audit.AuditService;
import com.internalmarketplace.api.moderation.dto.ModeratePostRequest;
import com.internalmarketplace.api.moderation.dto.ReviewReportRequest;
import com.internalmarketplace.api.security.CurrentUser;
import com.internalmarketplace.api.security.FirebaseUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;
    private final AuditService auditService;

    // PATCH /api/v1/admin/posts/{id}/moderate
    @PatchMapping("/posts/{id}/moderate")
    public ResponseEntity<Void> moderatePost(@PathVariable String id, @Valid @RequestBody ModeratePostRequest body,
                                              @CurrentUser FirebaseUserPrincipal user) {
        moderationService.moderatePost(id, body.status().name());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("status", body.status().name());
        metadata.put("note", body.note() != null ? body.note() : "");
        auditService.recordAudit(user.uid(), "POST_MODERATED", "post", id, metadata);

        return ResponseEntity.noContent().build();
    }

    // PATCH /api/v1/admin/reports/{id}
    @PatchMapping("/reports/{id}")
    public ResponseEntity<Void> reviewReport(@PathVariable String id, @Valid @RequestBody ReviewReportRequest body,
                                              @CurrentUser FirebaseUserPrincipal user) {
        moderationService.reviewReport(id, body.status().name(), user.uid());

        auditService.recordAudit(user.uid(), "REPORT_REVIEWED", "report", id, Map.of("status", body.status().name()));

        return ResponseEntity.noContent().build();
    }
}
