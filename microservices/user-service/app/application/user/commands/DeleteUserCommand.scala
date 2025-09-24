package application.user.commands

import domain.user.UserId

case class DeleteUserCommand(
  userId: UserId
)