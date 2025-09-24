package application.notification.queries

import domain.notification.UserId

case class GetUserNotificationsQuery(
  userId: UserId,
  page: Int = 0,
  size: Int = 20
)