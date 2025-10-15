package infrastructure.persistence

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}
import domain.user.{PasswordResetToken, PasswordResetTokenId, PasswordResetTokenRepository, UserId}
import domain.shared.DomainError
import java.time.LocalDateTime

@Singleton
class PasswordResetTokenRepositoryImpl @Inject()(
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) extends PasswordResetTokenRepository {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  // Slick table definition
  private class PasswordResetTokensTable(tag: Tag) extends Table[PasswordResetTokenRow](tag, "password_reset_tokens") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def userId = column[Long]("user_id")
    def token = column[String]("token")
    def expiresAt = column[LocalDateTime]("expires_at")
    def used = column[Boolean]("used")
    def createdAt = column[LocalDateTime]("created_at")

    def * = (id.?, userId, token, expiresAt, used, createdAt).mapTo[PasswordResetTokenRow]
  }

  private val passwordResetTokens = TableQuery[PasswordResetTokensTable]

  // Row representation
  private case class PasswordResetTokenRow(
    id: Option[Long],
    userId: Long,
    token: String,
    expiresAt: LocalDateTime,
    used: Boolean,
    createdAt: LocalDateTime
  )

  // Conversions
  private def rowToDomain(row: PasswordResetTokenRow): PasswordResetToken = {
    PasswordResetToken(
      id = row.id match {
        case Some(id) => PasswordResetTokenId.existing(id)
        case None => PasswordResetTokenId.newId()
      },
      userId = UserId.existing(row.userId),
      token = row.token,
      expiresAt = row.expiresAt,
      used = row.used,
      createdAt = row.createdAt
    )
  }

  private def domainToRow(token: PasswordResetToken): PasswordResetTokenRow = {
    PasswordResetTokenRow(
      id = token.id.value,
      userId = token.userId.value.getOrElse(
        throw new IllegalStateException("UserId must be defined")
      ),
      token = token.token,
      expiresAt = token.expiresAt,
      used = token.used,
      createdAt = token.createdAt
    )
  }

  override def save(resetToken: PasswordResetToken): Future[Either[DomainError, PasswordResetToken]] = {
    val row = domainToRow(resetToken)

    val action = resetToken.id.value match {
      case Some(id) =>
        // Update existing
        passwordResetTokens
          .filter(_.id === id)
          .update(row)
          .flatMap(_ => passwordResetTokens.filter(_.id === id).result.head)

      case None =>
        // Insert new
        (passwordResetTokens returning passwordResetTokens.map(_.id)
          into ((token, id) => token.copy(id = Some(id)))
          ) += row
    }

    db.run(action.transactionally).map { savedRow =>
      Right(rowToDomain(savedRow))
    }.recover {
      case ex: Exception =>
        Left(DomainError.InvalidOperation(s"Failed to save reset token: ${ex.getMessage}"))
    }
  }

  override def findByToken(token: String): Future[Option[PasswordResetToken]] = {
    db.run(
      passwordResetTokens
        .filter(_.token === token)
        .result
        .headOption
    ).map(_.map(rowToDomain))
  }

  override def findActiveTokensByUserId(userId: UserId): Future[Seq[PasswordResetToken]] = {
    val now = LocalDateTime.now()
    db.run(
      passwordResetTokens
        .filter(_.userId === userId.value.get)
        .filter(_.used === false)
        .filter(_.expiresAt > now)
        .result
    ).map(_.map(rowToDomain))
  }

  override def invalidateAllTokensForUser(userId: UserId): Future[Unit] = {
    db.run(
      passwordResetTokens
        .filter(_.userId === userId.value.get)
        .map(_.used)
        .update(true)
    ).map(_ => ())
  }

  override def deleteExpiredTokens(): Future[Int] = {
    val now = LocalDateTime.now()
    db.run(
      passwordResetTokens
        .filter(_.expiresAt < now)
        .delete
    )
  }
}
