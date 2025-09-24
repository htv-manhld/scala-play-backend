package application.user.commands

import domain.user.UserId

case class UpdateUserProfileCommand(
  userId: UserId,
  name: String,
  age: Int
)