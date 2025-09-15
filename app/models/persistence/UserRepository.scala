package models.persistence

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import models.domain.User

@Singleton
class UserRepository @Inject()(dbConfigProvider: DatabaseConfigProvider) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  private class UserTable(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def email = column[String]("email")
    def age = column[Int]("age")
    def createdAt = column[Option[java.time.LocalDateTime]]("created_at")
    def updatedAt = column[Option[java.time.LocalDateTime]]("updated_at")

    def * = (id, name, email, age, createdAt, updatedAt) <> ((User.apply _).tupled, User.unapply)
  }

  private val users = TableQuery[UserTable]

  def findById(id: Long): Future[Option[User]] = db.run {
    users.filter(_.id === id).result.headOption
  }

  def findByEmail(email: String): Future[Option[User]] = db.run {
    users.filter(_.email === email).result.headOption
  }

  def create(user: User): Future[User] = db.run {
    val now = Some(java.time.LocalDateTime.now())
    (users.map(u => (u.name, u.email, u.age, u.createdAt, u.updatedAt))
      returning users.map(_.id)
      into ((userData, id) => User(id, userData._1, userData._2, userData._3, userData._4, userData._5))
    ) += (user.name, user.email, user.age, now, now)
  }

  def update(id: Long, user: User): Future[Int] = db.run {
    val now = Some(java.time.LocalDateTime.now())
    users.filter(_.id === id).update(user.copy(id = id, updatedAt = now))
  }

  def delete(id: Long): Future[Int] = db.run {
    users.filter(_.id === id).delete
  }

  def list(): Future[Seq[User]] = db.run {
    users.result
  }
}