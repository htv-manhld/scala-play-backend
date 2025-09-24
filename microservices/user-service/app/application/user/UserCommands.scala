package application.user

import domain.user.{UserId, Email}
import application.shared.{Command, Query}

// Commands (write operations)
sealed trait UserCommand extends Command

case class CreateUserCommand(
  name: String,
  email: Email,
  age: Int
) extends UserCommand

case class UpdateUserCommand(
  userId: UserId,
  name: String,
  age: Int
) extends UserCommand

case class ChangeUserEmailCommand(
  userId: UserId,
  newEmail: Email
) extends UserCommand

case class DeleteUserCommand(
  userId: UserId
) extends UserCommand

// Queries (read operations)
sealed trait UserQuery extends Query

case class GetUserByIdQuery(
  userId: UserId
) extends UserQuery

case class GetUserByEmailQuery(
  email: Email
) extends UserQuery

case class GetAllUsersQuery(
  page: Int = 0,
  size: Int = 20
) extends UserQuery