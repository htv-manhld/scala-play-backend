package application.user.queries

import domain.user.UserId

case class GetUserByIdQuery(
  userId: UserId
)