package controllers.api

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import models.domain.{User, Message, ApiResponse, UserCreateRequest, UserUpdateRequest}
import utils.JsonFormats._
import java.time.Instant

/**
 * API Controller for frontend integration
 */
@Singleton
class ApiController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  // Sample users data
  private var users = List(
    User(1L, "John Doe", "john@example.com", 30),
    User(2L, "Jane Smith", "jane@example.com", 25),
    User(3L, "Bob Johnson", "bob@example.com", 35)
  )

  /**
   * Get all users
   */
  def getUsers() = Action {
    val response = ApiResponse.success(users, Some("Users retrieved successfully"))
    Ok(Json.toJson(response))
  }

  /**
   * Get user by ID
   */
  def getUser(id: Long) = Action {
    users.find(_.id == id) match {
      case Some(user) => 
        val response = ApiResponse.success(user, Some("User retrieved successfully"))
        Ok(Json.toJson(response))
      case None => 
        val response = ApiResponse.error[User]("User not found", Some("No user found with the given ID"))
        NotFound(Json.toJson(response))
    }
  }

  /**
   * Create a new user
   */
  def createUser() = Action(parse.json) { (request: Request[JsValue]) =>
    request.body.validate[UserCreateRequest] match {
      case JsSuccess(userRequest, _) => 
        val newId = if (users.isEmpty) 1L else users.map(_.id).max + 1L
        val newUser = User(
          id = newId,
          name = userRequest.name,
          email = userRequest.email,
          age = userRequest.age
        )
        users = users :+ newUser
        val response = ApiResponse.success(newUser, Some("User created successfully"))
        Created(Json.toJson(response))
      case JsError(errors) => 
        val response = ApiResponse.error[User]("Invalid user data", Some(errors.toString))
        BadRequest(Json.toJson(response))
    }
  }

  /**
   * Update a user
   */
  def updateUser(id: Long) = Action(parse.json) { (request: Request[JsValue]) =>
    request.body.validate[UserUpdateRequest] match {
      case JsSuccess(updateRequest, _) =>
        users.find(_.id == id) match {
          case Some(existingUser) =>
            val updatedUser = existingUser.copy(
              name = updateRequest.name.getOrElse(existingUser.name),
              email = updateRequest.email.getOrElse(existingUser.email),
              age = updateRequest.age.getOrElse(existingUser.age)
            )
            users = users.map(u => if (u.id == id) updatedUser else u)
            val response = ApiResponse.success(updatedUser, Some("User updated successfully"))
            Ok(Json.toJson(response))
          case None =>
            val response = ApiResponse.error[User]("User not found", Some("No user found with the given ID"))
            NotFound(Json.toJson(response))
        }
      case JsError(errors) =>
        val response = ApiResponse.error[User]("Invalid update data", Some(errors.toString))
        BadRequest(Json.toJson(response))
    }
  }

  /**
   * Delete a user
   */
  def deleteUser(id: Long) = Action {
    users.find(_.id == id) match {
      case Some(_) =>
        users = users.filter(_.id != id)
        val response = ApiResponse.success[String](s"User $id deleted", Some("User deleted successfully"))
        Ok(Json.toJson(response))
      case None =>
        val response = ApiResponse.error[String]("User not found", Some("No user found with the given ID"))
        NotFound(Json.toJson(response))
    }
  }

  /**
   * Health check endpoint
   */
  def health() = Action {
    val healthData = Json.obj(
      "status" -> "ok",
      "message" -> "Backend is running",
      "version" -> "1.0.0",
      "environment" -> "development"
    )
    val response = ApiResponse.success(healthData, Some("Backend is healthy"))
    Ok(Json.toJson(response))
  }

  /**
   * Echo endpoint for testing
   */
  def echo() = Action(parse.json) { (request: Request[JsValue]) =>
    val echoData = Json.obj(
      "message" -> "Echo from backend",
      "received" -> request.body
    )
    val response = ApiResponse.success(echoData, Some("Echo successful"))
    Ok(Json.toJson(response))
  }
}
