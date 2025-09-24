package controllers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc._
import play.api.libs.json._
import application.user.UserService
import application.user.commands._
import application.user.queries._
import domain.user.{UserId, Email}
import controllers.dto._

@Singleton
class UserController @Inject()(
  cc: ControllerComponents,
  userService: UserService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private def errorCode(error: domain.shared.DomainError): String = error match {
    case _: domain.shared.DomainError.ValidationError => "VALIDATION_ERROR"
    case _: domain.shared.DomainError.NotFound => "NOT_FOUND"
    case _: domain.shared.DomainError.InvalidOperation => "INVALID_OPERATION"
    case _: domain.shared.DomainError.DuplicateError => "DUPLICATE_ERROR"
  }

  // GET /api/users
  def getAllUsers(page: Int, size: Int): Action[AnyContent] = Action.async { implicit request =>
    val query = GetAllUsersQuery(page, size)

    userService.handle(query).map { users =>
      val usersDto = users.map(UserResponseDto.fromDomain)

      val response = Json.obj(
        "success" -> true,
        "data" -> usersDto,
        "message" -> "Users retrieved successfully"
      )
      Ok(response)
    }.recover {
      case ex: Exception =>
        val response = Json.obj(
          "success" -> false,
          "error" -> ex.getMessage,
          "code" -> "INTERNAL_ERROR"
        )
        InternalServerError(response)
    }
  }

  // GET /api/users/:id
  def getUser(id: String): Action[AnyContent] = Action.async { implicit request =>
    val query = GetUserByIdQuery(UserId(id.toLong))

    userService.handle(query).map {
      case Some(user) =>
        val userDto = UserResponseDto.fromDomain(user)

        val response = Json.obj(
          "success" -> true,
          "data" -> userDto,
          "message" -> "User retrieved successfully"
        )
        Ok(response)
      case None =>
        val response = Json.obj(
          "success" -> false,
          "error" -> "User not found",
          "code" -> "USER_NOT_FOUND"
        )
        NotFound(response)
    }.recover {
      case ex: Exception =>
        val response = Json.obj(
          "success" -> false,
          "error" -> ex.getMessage,
          "code" -> "INTERNAL_ERROR"
        )
        InternalServerError(response)
    }
  }

  // GET /api/users/by-email/:email
  def getUserByEmail(email: String): Action[AnyContent] = Action.async { implicit request =>
    try {
      val query = GetUserByEmailQuery(Email(email))

      userService.handle(query).map {
        case Some(user) =>
          val userDto = UserResponseDto.fromDomain(user)

          val response = Json.obj(
            "success" -> true,
            "data" -> userDto,
            "message" -> "User retrieved successfully"
          )
          Ok(response)
        case None =>
          val response = Json.obj(
            "success" -> false,
            "error" -> "User not found",
            "code" -> "USER_NOT_FOUND"
          )
          NotFound(response)
      }.recover {
        case ex: Exception =>
          val response = Json.obj(
            "success" -> false,
            "error" -> ex.getMessage,
            "code" -> "INTERNAL_ERROR"
          )
          InternalServerError(response)
      }
    } catch {
      case ex: IllegalArgumentException =>
        val response = Json.obj(
          "success" -> false,
          "error" -> ex.getMessage,
          "code" -> "VALIDATION_ERROR"
        )
        Future.successful(BadRequest(response))
    }
  }

  // POST /api/users
  def createUser(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[CreateUserRequestDto] match {
      case JsSuccess(dto, _) =>
        try {
          val command = CreateUserCommand(
            email = Email(dto.email),
            name = dto.name,
            age = dto.age
          )

          userService.handle(command).map {
            case Right(user) =>
              val userDto = UserResponseDto.fromDomain(user)

              val response = Json.obj(
                "success" -> true,
                "data" -> userDto,
                "message" -> "User created successfully"
              )
              Created(response)
            case Left(error) =>
              val response = Json.obj(
                "success" -> false,
                "error" -> error.message,
                "code" -> errorCode(error)
              )
              BadRequest(response)
          }
        } catch {
          case ex: IllegalArgumentException =>
            val response = Json.obj(
              "success" -> false,
              "error" -> ex.getMessage,
              "code" -> "VALIDATION_ERROR"
            )
            Future.successful(BadRequest(response))
        }
      case JsError(errors) =>
        val errorMessage = errors.map { case (path, validationErrors) =>
          s"${path.toString()}: ${validationErrors.map(_.message).mkString(", ")}"
        }.mkString("; ")

        val response = Json.obj(
          "success" -> false,
          "error" -> s"Invalid request: $errorMessage",
          "code" -> "INVALID_REQUEST"
        )
        Future.successful(BadRequest(response))
    }
  }

  // PUT /api/users/:id
  def updateUser(id: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[UpdateUserRequestDto] match {
      case JsSuccess(dto, _) =>
        val command = UpdateUserProfileCommand(
          userId = UserId(id.toLong),
          name = dto.name,
          age = dto.age
        )

        userService.handle(command).map {
          case Right(user) =>
            val userDto = UserResponseDto.fromDomain(user)

            val response = Json.obj(
              "success" -> true,
              "data" -> userDto,
              "message" -> "User updated successfully"
            )
            Ok(response)
          case Left(error) =>
            val code = errorCode(error)
            val response = Json.obj(
              "success" -> false,
              "error" -> error.message,
              "code" -> code
            )
            code match {
              case "NOT_FOUND" => NotFound(response)
              case _ => BadRequest(response)
            }
        }
      case JsError(errors) =>
        val errorMessage = errors.map { case (path, validationErrors) =>
          s"${path.toString()}: ${validationErrors.map(_.message).mkString(", ")}"
        }.mkString("; ")

        val response = Json.obj(
          "success" -> false,
          "error" -> s"Invalid request: $errorMessage",
          "code" -> "INVALID_REQUEST"
        )
        Future.successful(BadRequest(response))
    }
  }

  // PUT /api/users/:id/email
  def changeUserEmail(id: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[ChangeEmailRequestDto] match {
      case JsSuccess(dto, _) =>
        try {
          val command = ChangeUserEmailCommand(
            userId = UserId(id.toLong),
            newEmail = Email(dto.email)
          )

          userService.handle(command).map {
            case Right(user) =>
              val userDto = UserResponseDto.fromDomain(user)

              val response = Json.obj(
                "success" -> true,
                "data" -> userDto,
                "message" -> "User email changed successfully"
              )
              Ok(response)
            case Left(error) =>
              val code = errorCode(error)
              val response = Json.obj(
                "success" -> false,
                "error" -> error.message,
                "code" -> code
              )
              code match {
                case "NOT_FOUND" => NotFound(response)
                case "DUPLICATE_ERROR" => Conflict(response)
                case _ => BadRequest(response)
              }
          }
        } catch {
          case ex: IllegalArgumentException =>
            val response = Json.obj(
              "success" -> false,
              "error" -> ex.getMessage,
              "code" -> "VALIDATION_ERROR"
            )
            Future.successful(BadRequest(response))
        }
      case JsError(errors) =>
        val errorMessage = errors.map { case (path, validationErrors) =>
          s"${path.toString()}: ${validationErrors.map(_.message).mkString(", ")}"
        }.mkString("; ")

        val response = Json.obj(
          "success" -> false,
          "error" -> s"Invalid request: $errorMessage",
          "code" -> "INVALID_REQUEST"
        )
        Future.successful(BadRequest(response))
    }
  }

  // DELETE /api/users/:id
  def deleteUser(id: String): Action[AnyContent] = Action.async { implicit request =>
    val command = DeleteUserCommand(UserId(id.toLong))

    userService.handle(command).map {
      case Right(_) =>
        val response = Json.obj(
          "success" -> true,
          "message" -> "User deleted successfully"
        )
        Ok(response)
      case Left(error) =>
        val code = errorCode(error)
        val response = Json.obj(
          "success" -> false,
          "error" -> error.message,
          "code" -> code
        )
        code match {
          case "NOT_FOUND" => NotFound(response)
          case _ => InternalServerError(response)
        }
    }
  }
}