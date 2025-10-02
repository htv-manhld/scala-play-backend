package application.user.commands

import domain.user.UserId
import java.time.LocalDate

case class UpdateUserProfileCommand(
  userId: UserId,
  name: String,
  birthdate: Option[LocalDate] = None
)