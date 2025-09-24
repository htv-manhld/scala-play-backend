package infrastructure.persistence

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import domain.user._
import java.time.LocalDateTime

/**
 * Slick implementation of UserRepository
 */
@Singleton
class UserRepositoryImpl @Inject()(
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) extends UserRepository {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  // Database entity for mapping to table
  case class UserEntity(
    id: Long,
    email: String,
    name: String,
    age: Int,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    version: Long
  )

  private class UserTable(tag: Tag) extends Table[UserEntity](tag, "users") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def email = column[String]("email", O.Unique)
    def name = column[String]("name")
    def age = column[Int]("age")
    def createdAt = column[LocalDateTime]("created_at")
    def updatedAt = column[LocalDateTime]("updated_at")
    def version = column[Long]("version")

    def * = (id, email, name, age, createdAt, updatedAt, version) <> ((UserEntity.apply _).tupled, UserEntity.unapply)
  }

  private val users = TableQuery[UserTable]

  // Mapping between domain and entity
  private def entityToDomain(entity: UserEntity): User = {
    User(
      id = UserId(entity.id),
      email = Email(entity.email),
      profile = UserProfile(entity.name, entity.age),
      createdAt = entity.createdAt,
      updatedAt = entity.updatedAt,
      version = entity.version
    )
  }

  private def domainToEntity(user: User): UserEntity = {
    UserEntity(
      id = user.id.value,
      email = user.email.value,
      name = user.profile.name,
      age = user.profile.age,
      createdAt = user.createdAt,
      updatedAt = user.updatedAt,
      version = user.version
    )
  }

  override def findById(id: UserId): Future[Option[User]] = db.run {
    users.filter(_.id === id.value).result.headOption.map(_.map(entityToDomain))
  }

  override def findByEmail(email: Email): Future[Option[User]] = db.run {
    users.filter(_.email === email.value).result.headOption.map(_.map(entityToDomain))
  }

  override def save(user: User): Future[User] = {
    val entity = domainToEntity(user)

    val action = if (entity.id == 0L) {
      // Insert new user
      val now = LocalDateTime.now()
      val insertAction = (users.map(u => (u.email, u.name, u.age, u.createdAt, u.updatedAt, u.version))
        returning users.map(_.id)
        into ((userData, id) => entity.copy(id = id, createdAt = now, updatedAt = now))
      ) += (entity.email, entity.name, entity.age, now, now, 1L)

      insertAction.map(entityToDomain)
    } else {
      // Update existing user with optimistic locking
      val updateAction = users
        .filter(u => u.id === entity.id && u.version === entity.version)
        .update(entity.copy(version = entity.version + 1))

      updateAction.flatMap { updatedRows =>
        if (updatedRows > 0) {
          DBIO.successful(entityToDomain(entity.copy(version = entity.version + 1)))
        } else {
          DBIO.failed(new RuntimeException("Concurrency conflict - user was modified by another transaction"))
        }
      }
    }

    db.run(action.transactionally)
  }

  override def delete(id: UserId): Future[Boolean] = db.run {
    users.filter(_.id === id.value).delete.map(_ > 0)
  }

  override def findAll(): Future[Seq[User]] = db.run {
    users.result.map(_.map(entityToDomain))
  }

  override def nextId(): Future[UserId] = {
    // In a real implementation, this might use a sequence or UUID
    // For simplicity, we'll return a placeholder
    Future.successful(UserId(0L))
  }
}

/**
 * Implementation of UserDomainService
 */
@Singleton
class UserDomainServiceImpl @Inject()(
  userRepository: UserRepository
)(implicit ec: ExecutionContext) extends UserDomainService {

  override def isEmailUnique(email: Email, excludeUserId: Option[UserId] = None): Future[Boolean] = {
    userRepository.findByEmail(email).map {
      case Some(existingUser) =>
        excludeUserId.exists(_ == existingUser.id)
      case None =>
        true
    }
  }
}