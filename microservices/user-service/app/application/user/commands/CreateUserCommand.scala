package application.user.commands

import domain.user.Email
import java.time.LocalDate

case class CreateUserCommand(
  email: Email,
  name: String,
  password: Option[String] = None,
  birthdate: Option[LocalDate] = None
)