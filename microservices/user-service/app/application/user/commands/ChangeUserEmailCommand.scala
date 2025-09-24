package application.user.commands

import domain.user.{UserId, Email}

case class ChangeUserEmailCommand(
  userId: UserId,
  newEmail: Email
)