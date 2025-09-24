package application.user.queries

import domain.user.Email

case class GetUserByEmailQuery(
  email: Email
)