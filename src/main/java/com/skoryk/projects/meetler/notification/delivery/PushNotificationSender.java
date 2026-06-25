package com.skoryk.projects.meetler.notification.delivery;

import com.skoryk.projects.meetler.notification.Notification;
import com.skoryk.projects.meetler.notification.device.UserDevice;

public interface PushNotificationSender {
  PushDeliveryResult send(UserDevice device, Notification notification);
}
