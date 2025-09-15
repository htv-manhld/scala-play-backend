package services.user

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import models.domain.{User, UserCreateRequest, UserUpdateRequest}
import models.persistence.UserRepository

@Singleton
class UserService @Inject()(
  userRepository: UserRepository
)(implicit ec: ExecutionContext) {

  def getUser(id: Long): Future[Option[User]] = {
    userRepository.findById(id)
  }

  def getUserByEmail(email: String): Future[Option[User]] = {
    userRepository.findByEmail(email)
  }

  def createUser(request: UserCreateRequest): Future[User] = {
    val user = User(0L, request.name, request.email, request.age)
    userRepository.create(user)
  }

  def updateUser(id: Long, request: UserUpdateRequest): Future[Option[User]] = {
    userRepository.findById(id).flatMap {
      case Some(existingUser) =>
        val updatedUser = existingUser.copy(
          name = request.name.getOrElse(existingUser.name),
          email = request.email.getOrElse(existingUser.email),
          age = request.age.getOrElse(existingUser.age)
        )
        userRepository.update(id, updatedUser).flatMap { updated =>
          if (updated > 0) {
            userRepository.findById(id)
          } else {
            Future.successful(None)
          }
        }
      case None =>
        Future.successful(None)
    }
  }

  def deleteUser(id: Long): Future[Boolean] = {
    userRepository.delete(id).map(_ > 0)
  }

  def listUsers(): Future[Seq[User]] = {
    userRepository.list()
  }
}