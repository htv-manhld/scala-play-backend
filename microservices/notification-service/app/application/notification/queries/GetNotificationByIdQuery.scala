package application.notification.queries

import domain.notification.NotificationId

case class GetNotificationByIdQuery(
  notificationId: NotificationId
)