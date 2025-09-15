package controllers.user

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import services.user.UserService
import models.domain.{User, UserCreateRequest, UserUpdateRequest, ApiResponse}
import utils.JsonFormats._
import scala.concurrent.ExecutionContext

@Singleton
class UserController @Inject()(
  val controllerComponents: ControllerComponents,
  userService: UserService
)(implicit ec: ExecutionContext) extends BaseController {

  def getAllUsers = Action.async { implicit request =>
    userService.listUsers().map { users =>
      val response = ApiResponse.success(users, Some("Users retrieved successfully"))
      Ok(Json.toJson(response))
    }
  }

  def getUser(id: Long) = Action.async { implicit request =>
    userService.getUser(id).map {
      case Some(user) =>
        val response = ApiResponse.success(user, Some("User retrieved successfully"))
        Ok(Json.toJson(response))
      case None =>
        val response = ApiResponse.error[User]("User not found", Some(s"No user found with ID: $id"))
        NotFound(Json.toJson(response))
    }
  }

  def createUser() = Action.async(parse.json) { implicit request =>
    request.body.validate[UserCreateRequest].fold(
      errors => {
        val response = ApiResponse.error[User]("Invalid user data", Some(JsError.toJson(errors).toString))
        scala.concurrent.Future.successful(BadRequest(Json.toJson(response)))
      },
      userRequest => {
        userService.createUser(userRequest).map { user =>
          val response = ApiResponse.success(user, Some("User created successfully"))
          Created(Json.toJson(response))
        }.recover {
          case ex: Exception =>
            val response = ApiResponse.error[User]("Failed to create user", Some(ex.getMessage))
            InternalServerError(Json.toJson(response))
        }
      }
    )
  }

  def updateUser(id: Long) = Action.async(parse.json) { implicit request =>
    request.body.validate[UserUpdateRequest].fold(
      errors => {
        val response = ApiResponse.error[User]("Invalid update data", Some(JsError.toJson(errors).toString))
        scala.concurrent.Future.successful(BadRequest(Json.toJson(response)))
      },
      updateRequest => {
        userService.updateUser(id, updateRequest).map {
          case Some(user) =>
            val response = ApiResponse.success(user, Some("User updated successfully"))
            Ok(Json.toJson(response))
          case None =>
            val response = ApiResponse.error[User]("User not found", Some(s"No user found with ID: $id"))
            NotFound(Json.toJson(response))
        }.recover {
          case ex: Exception =>
            val response = ApiResponse.error[User]("Failed to update user", Some(ex.getMessage))
            InternalServerError(Json.toJson(response))
        }
      }
    )
  }

  def deleteUser(id: Long) = Action.async { implicit request =>
    userService.deleteUser(id).map { deleted =>
      if (deleted) {
        val response = ApiResponse.success(s"User $id deleted", Some("User deleted successfully"))
        Ok(Json.toJson(response))
      } else {
        val response = ApiResponse.error[String]("User not found", Some(s"No user found with ID: $id"))
        NotFound(Json.toJson(response))
      }
    }.recover {
      case ex: Exception =>
        val response = ApiResponse.error[String]("Failed to delete user", Some(ex.getMessage))
        InternalServerError(Json.toJson(response))
    }
  }
}