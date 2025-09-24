package application.notification.commands

import domain.notification.NotificationId

case class MarkAsFailedCommand(
  notificationId: NotificationId,
  reason: String
)