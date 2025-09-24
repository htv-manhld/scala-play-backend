package application.notification.commands

import domain.notification.NotificationId

case class MarkAsDeliveredCommand(
  notificationId: NotificationId
)