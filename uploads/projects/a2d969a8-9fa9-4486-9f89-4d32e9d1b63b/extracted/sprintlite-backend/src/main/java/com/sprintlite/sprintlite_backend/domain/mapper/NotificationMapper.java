package com.sprintlite.sprintlite_backend.domain.mapper;

import org.springframework.stereotype.Component;

import com.sprintlite.sprintlite_backend.domain.dto.response.NotificationResponse;
import com.sprintlite.sprintlite_backend.domain.entity.Notification;

@Component
public class NotificationMapper {

public NotificationResponse toResponse(Notification notification) {
if (notification == null) return null;

return NotificationResponse.builder()
.id(notification.getId())
.type(notification.getType())
.title(notification.getTitle())
.content(notification.getContent())
.isRead(notification.getIsRead())
.createdAt(notification.getCreatedAt())
.build();
}
}