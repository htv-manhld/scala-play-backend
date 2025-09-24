package application.user.commands

import domain.user.Email

case class CreateUserCommand(
  email: Email,
  name: String,
  age: Int
)