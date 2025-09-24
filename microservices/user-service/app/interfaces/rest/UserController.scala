package interfaces.rest

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc._
import play.api.libs.json._
import application.user._
import domain.user._
import interfaces.rest.common._

/**
 * REST Controller for User operations following DDD principles
 */
@Singleton
class UserController @Inject()(
  cc: ControllerComponents,
  userApplicationService: UserApplicationService,
  userQueryService: UserQueryService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // GET /api/users/:id
  def getUser(id: Long): Action[AnyContent] = Action.async { implicit request =>
    val query = GetUserByIdQuery(UserId(id))

    userQueryService.findById(query).map {
      case Some(user) => Ok(ApiResponse.success(user))
      case None => NotFound(ApiResponse.error("User not found", "USER_NOT_FOUND"))
    }
  }

  // GET /api/users
  def getAllUsers(page: Int, size: Int): Action[AnyContent] = Action.async { implicit request =>
    val query = GetAllUsersQuery(page, size)

    userQueryService.findAll(query).map { users =>
      Ok(ApiResponse.success(users))
    }
  }

  // GET /api/users/by-email/:email
  def getUserByEmail(email: String): Action[AnyContent] = Action.async { implicit request =>
    try {
      val query = GetUserByEmailQuery(Email(email))

      userQueryService.findByEmail(query).map {
        case Some(user) => Ok(ApiResponse.success(user))
        case None => NotFound(ApiResponse.error("User not found", "USER_NOT_FOUND"))
      }
    } catch {
      case _: IllegalArgumentException =>
        Future.successful(BadRequest(ApiResponse.error("Invalid email format", "INVALID_EMAIL")))
    }
  }

  // POST /api/users
  def createUser(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[CreateUserRequest] match {
      case JsSuccess(createRequest, _) =>
        try {
          val command = CreateUserCommand(
            name = createRequest.name,
            email = Email(createRequest.email),
            age = createRequest.age
          )

          userApplicationService.createUser(command).map {
            case Right(userId) =>
              Created(ApiResponse.success(Map("id" -> userId.value), "User created successfully"))
            case Left(DomainError.ValidationError(message)) =>
              BadRequest(ApiResponse.error(message, "VALIDATION_ERROR"))
            case Left(error) =>
              InternalServerError(ApiResponse.error(error.message, "INTERNAL_ERROR"))
          }
        } catch {
          case _: IllegalArgumentException =>
            Future.successful(BadRequest(ApiResponse.error("Invalid email format", "INVALID_EMAIL")))
        }
      case JsError(errors) =>
        val errorMessage = errors.map { case (path, validationErrors) =>
          s"${path.toString()}: ${validationErrors.map(_.message).mkString(", ")}"
        }.mkString("; ")
        Future.successful(BadRequest(ApiResponse.error(s"Invalid request: $errorMessage", "INVALID_REQUEST")))
    }
  }

  // PUT /api/users/:id
  def updateUser(id: Long): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[UpdateUserRequest] match {
      case JsSuccess(updateRequest, _) =>
        val command = UpdateUserCommand(
          userId = UserId(id),
          name = updateRequest.name.getOrElse(""),
          age = updateRequest.age.getOrElse(0)
        )

        userApplicationService.updateUser(command).map {
          case Right(_) =>
            Ok(ApiResponse.success(message = "User updated successfully"))
          case Left(DomainError.NotFound(message)) =>
            NotFound(ApiResponse.error(message, "USER_NOT_FOUND"))
          case Left(DomainError.ValidationError(message)) =>
            BadRequest(ApiResponse.error(message, "VALIDATION_ERROR"))
          case Left(error) =>
            InternalServerError(ApiResponse.error(error.message, "INTERNAL_ERROR"))
        }
      case JsError(errors) =>
        val errorMessage = errors.map { case (path, validationErrors) =>
          s"${path.toString()}: ${validationErrors.map(_.message).mkString(", ")}"
        }.mkString("; ")
        Future.successful(BadRequest(ApiResponse.error(s"Invalid request: $errorMessage", "INVALID_REQUEST")))
    }
  }

  // PUT /api/users/:id/email
  def changeUserEmail(id: Long): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[ChangeEmailRequest] match {
      case JsSuccess(emailRequest, _) =>
        try {
          val command = ChangeUserEmailCommand(
            userId = UserId(id),
            newEmail = Email(emailRequest.email)
          )

          userApplicationService.changeUserEmail(command).map {
            case Right(_) =>
              Ok(ApiResponse.success(message = "User email updated successfully"))
            case Left(DomainError.NotFound(message)) =>
              NotFound(ApiResponse.error(message, "USER_NOT_FOUND"))
            case Left(DomainError.ValidationError(message)) =>
              BadRequest(ApiResponse.error(message, "VALIDATION_ERROR"))
            case Left(error) =>
              InternalServerError(ApiResponse.error(error.message, "INTERNAL_ERROR"))
          }
        } catch {
          case _: IllegalArgumentException =>
            Future.successful(BadRequest(ApiResponse.error("Invalid email format", "INVALID_EMAIL")))
        }
      case JsError(errors) =>
        val errorMessage = errors.map { case (path, validationErrors) =>
          s"${path.toString()}: ${validationErrors.map(_.message).mkString(", ")}"
        }.mkString("; ")
        Future.successful(BadRequest(ApiResponse.error(s"Invalid request: $errorMessage", "INVALID_REQUEST")))
    }
  }

  // DELETE /api/users/:id
  def deleteUser(id: Long): Action[AnyContent] = Action.async { implicit request =>
    val command = DeleteUserCommand(UserId(id))

    userApplicationService.deleteUser(command).map {
      case Right(_) =>
        Ok(ApiResponse.success(message = "User deleted successfully"))
      case Left(DomainError.NotFound(message)) =>
        NotFound(ApiResponse.error(message, "USER_NOT_FOUND"))
      case Left(error) =>
        InternalServerError(ApiResponse.error(error.message, "INTERNAL_ERROR"))
    }
  }
}