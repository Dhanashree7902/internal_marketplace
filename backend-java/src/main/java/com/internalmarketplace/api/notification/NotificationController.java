package com.internalmarketplace.api.notification;

import com.internalmarketplace.api.common.web.DataResponse;
import com.internalmarketplace.api.notification.dto.NotificationResponse;
import com.internalmarketplace.api.security.CurrentUser;
import com.internalmarketplace.api.security.FirebaseUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // GET /api/v1/notifications
    @GetMapping("/notifications")
    public DataResponse<List<NotificationResponse>> listNotifications(@CurrentUser FirebaseUserPrincipal user) {
        return DataResponse.of(notificationService.listForUser(user.uid()));
    }

    // POST /api/v1/notifications/{id}/read
    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable String id, @CurrentUser FirebaseUserPrincipal user) {
        notificationService.markRead(id, user.uid());
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/v1/notifications/{id}
    @DeleteMapping("/notifications/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, @CurrentUser FirebaseUserPrincipal user) {
        notificationService.delete(id, user.uid());
        return ResponseEntity.noContent().build();
    }
}
