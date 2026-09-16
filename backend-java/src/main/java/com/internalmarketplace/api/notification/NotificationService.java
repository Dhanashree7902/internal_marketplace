package com.internalmarketplace.api.notification;

import com.internalmarketplace.api.common.exception.NotFoundException;
import com.internalmarketplace.api.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<NotificationResponse> listForUser(String userId) {
        return notificationRepository.listForUser(userId);
    }

    public void markRead(String id, String userId) {
        NotificationResponse notification = notificationRepository.findById(id)
                .filter(n -> userId.equals(n.userId()))
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        notificationRepository.markRead(notification.id());
    }

    public void delete(String id, String userId) {
        NotificationResponse notification = notificationRepository.findById(id)
                .filter(n -> userId.equals(n.userId()))
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        notificationRepository.delete(notification.id());
    }
}
