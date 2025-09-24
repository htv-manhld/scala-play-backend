package application.notification.commands

import domain.notification.{UserId, NotificationChannel}

case class SendNotificationCommand(
  userId: UserId,
  channel: NotificationChannel,
  subject: String,
  content: String,
  recipient: String
)