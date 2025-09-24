package infrastructure.persistence

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._
import java.time.LocalDateTime
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import domain.user.{User, UserId, Email, UserProfile, UserRepository}
import domain.shared.DomainError

@Singleton
class UserRepositoryImpl @Inject()(
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) extends UserRepository {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._

  // Slick table definition
  class UsersTable(tag: Tag) extends Table[UserRow](tag, "users") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def email = column[String]("email")
    def age = column[Int]("age")
    def createdAt = column[LocalDateTime]("created_at")
    def updatedAt = column[LocalDateTime]("updated_at")

    def * = (id, name, email, age, createdAt, updatedAt).mapTo[UserRow]
  }

  private val users = TableQuery[UsersTable]

  // Data transfer object for Slick
  case class UserRow(
    id: Long,
    name: String,
    email: String,
    age: Int,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime
  )

  // Conversion between domain User and UserRow
  private def toDomain(row: UserRow): User = {
    User(
      id = UserId(row.id),
      email = Email(row.email),
      profile = UserProfile(row.name, row.age),
      createdAt = row.createdAt,
      updatedAt = row.updatedAt,
      version = 0 // Version will be handled separately if needed
    )
  }

  private def toRow(user: User): UserRow = {
    UserRow(
      id = user.id.value,
      name = user.profile.name,
      email = user.email.value,
      age = user.profile.age,
      createdAt = user.createdAt,
      updatedAt = user.updatedAt
    )
  }

  override def findById(id: UserId): Future[Option[User]] = {
    val query = users.filter(_.id === id.value)

    db.run(query.result.headOption).map(_.map(toDomain)).recover {
      case _ => None
    }
  }

  override def findByEmail(email: Email): Future[Option[User]] = {
    val query = users.filter(_.email === email.value)

    db.run(query.result.headOption).map(_.map(toDomain)).recover {
      case _ => None
    }
  }

  override def findAll(page: Int = 0, size: Int = 20): Future[Seq[User]] = {
    val query = users
      .sortBy(_.id.asc)
      .drop(page * size)
      .take(size)

    db.run(query.result).map(_.map(toDomain)).recover {
      case _ => Seq.empty
    }
  }

  override def save(user: User): Future[Either[DomainError, User]] = {
    if (user.id.value == 0) {
      // Create new user
      val insertAction = (users returning users.map(_.id)) += toRow(user).copy(id = 0L)

      db.run(insertAction).map { generatedId =>
        val savedUser = user.copy(id = UserId(generatedId))
        // Preserve uncommitted events from original user
        user.uncommittedEvents.foreach(event => savedUser.addEvent(event))
        Right(savedUser)
      }.recover {
        case ex: java.sql.SQLException if ex.getMessage.contains("duplicate key") =>
          Left(DomainError.DuplicateError("User with this email already exists"))
        case ex: Exception =>
          Left(DomainError.ValidationError(s"Database error: ${ex.getMessage}"))
      }
    } else {
      // Update existing user
      val updateAction = users
        .filter(_.id === user.id.value)
        .map(u => (u.name, u.email, u.age, u.updatedAt))
        .update((user.profile.name, user.email.value, user.profile.age, LocalDateTime.now()))

      db.run(updateAction).flatMap { rowsAffected =>
        if (rowsAffected > 0) {
          findById(user.id).map {
            case Some(updatedUser) => Right(updatedUser)
            case None => Left(DomainError.NotFound(s"User with id ${user.id.value} not found"))
          }
        } else {
          Future.successful(Left(DomainError.NotFound(s"User with id ${user.id.value} not found")))
        }
      }.recover {
        case ex: java.sql.SQLException if ex.getMessage.contains("duplicate key") =>
          Left(DomainError.DuplicateError("User with this email already exists"))
        case ex: Exception =>
          Left(DomainError.ValidationError(s"Database error: ${ex.getMessage}"))
      }
    }
  }

  override def delete(id: UserId): Future[Either[DomainError, Unit]] = {
    val deleteAction = users.filter(_.id === id.value).delete

    db.run(deleteAction).map { rowsAffected =>
      if (rowsAffected > 0) {
        Right(())
      } else {
        Left(DomainError.NotFound(s"User with id ${id.value} not found"))
      }
    }.recover {
      case ex: Exception =>
        Left(DomainError.ValidationError(s"Database error: ${ex.getMessage}"))
    }
  }

  override def nextIdentity(): Future[UserId] = {
    // For PostgreSQL, we'll let the database generate the ID
    // This is a placeholder - actual ID will be generated on insert
    Future.successful(UserId(0L))
  }
}