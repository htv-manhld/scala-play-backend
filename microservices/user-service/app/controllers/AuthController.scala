package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import application.user.UserAuthService
import controllers.dto._
import domain.user.Email
import domain.shared.DomainError

@Singleton
class AuthController @Inject()(
  cc: ControllerComponents,
  authService: UserAuthService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  /**
   * Login endpoint
   * POST /api/auth/login
   */
  def login(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[LoginRequestDto].fold(
      errors => {
        Future.successful(BadRequest(Json.obj(
          "success" -> false,
          "message" -> "Invalid request format",
          "errors" -> JsError.toJson(errors)
        )))
      },
      loginRequest => {
        try {
          val email = Email(loginRequest.email)

          authService.login(email, loginRequest.password).map {
            case Right((user, token)) =>
              val userDto = UserResponseDto.fromDomain(user)
              val loginResponse = LoginResponseDto(
                user = userDto,
                token = token.value,
                expiresAt = token.expiresAt.toString
              )
              Ok(Json.obj(
                "success" -> true,
                "data" -> Json.toJson(loginResponse),
                "message" -> "Login successful"
              ))

            case Left(error) => error match {
              case DomainError.AuthenticationFailed(msg) =>
                Unauthorized(Json.obj(
                  "success" -> false,
                  "message" -> msg
                ))
              case DomainError.ValidationError(msg) =>
                BadRequest(Json.obj(
                  "success" -> false,
                  "message" -> msg
                ))
              case _ =>
                InternalServerError(Json.obj(
                  "success" -> false,
                  "message" -> "An error occurred during login"
                ))
            }
          }
        } catch {
          case ex: IllegalArgumentException =>
            Future.successful(BadRequest(Json.obj(
              "success" -> false,
              "message" -> ex.getMessage
            )))
          case ex: Exception =>
            Future.successful(InternalServerError(Json.obj(
              "success" -> false,
              "message" -> "An unexpected error occurred"
            )))
        }
      }
    )
  }

  /**
   * Verify token endpoint
   * GET /api/auth/verify
   */
  def verifyToken(): Action[AnyContent] = Action.async { implicit request =>
    request.headers.get("Authorization") match {
      case Some(authHeader) if authHeader.startsWith("Bearer ") =>
        val token = authHeader.substring(7)

        authService.verifyToken(token).map {
          case Right(user) =>
            val userDto = UserResponseDto.fromDomain(user)
            Ok(Json.obj(
              "success" -> true,
              "data" -> Json.toJson(userDto),
              "message" -> "Token is valid"
            ))

          case Left(error) => error match {
            case DomainError.AuthenticationFailed(msg) =>
              Unauthorized(Json.obj(
                "success" -> false,
                "message" -> msg
              ))
            case DomainError.NotFound(msg) =>
              NotFound(Json.obj(
                "success" -> false,
                "message" -> msg
              ))
            case _ =>
              InternalServerError(Json.obj(
                "success" -> false,
                "message" -> "An error occurred during token verification"
              ))
          }
        }

      case _ =>
        Future.successful(Unauthorized(Json.obj(
          "success" -> false,
          "message" -> "Missing or invalid Authorization header"
        )))
    }
  }

  /**
   * Refresh token endpoint
   * POST /api/auth/refresh
   */
  def refreshToken(): Action[AnyContent] = Action.async { implicit request =>
    request.headers.get("Authorization") match {
      case Some(authHeader) if authHeader.startsWith("Bearer ") =>
        val token = authHeader.substring(7)

        authService.refreshToken(token).map {
          case Right((user, newToken)) =>
            val userDto = UserResponseDto.fromDomain(user)
            val refreshResponse = LoginResponseDto(
              user = userDto,
              token = newToken.value,
              expiresAt = newToken.expiresAt.toString
            )
            Ok(Json.obj(
              "success" -> true,
              "data" -> Json.toJson(refreshResponse),
              "message" -> "Token refreshed successfully"
            ))

          case Left(error) => error match {
            case DomainError.AuthenticationFailed(msg) =>
              Unauthorized(Json.obj(
                "success" -> false,
                "message" -> msg
              ))
            case DomainError.NotFound(msg) =>
              NotFound(Json.obj(
                "success" -> false,
                "message" -> msg
              ))
            case _ =>
              InternalServerError(Json.obj(
                "success" -> false,
                "message" -> "An error occurred during token refresh"
              ))
          }
        }

      case _ =>
        Future.successful(Unauthorized(Json.obj(
          "success" -> false,
          "message" -> "Missing or invalid Authorization header"
        )))
    }
  }

  /**
   * Logout endpoint - Blacklist the token
   * POST /api/auth/logout
   */
  def logout(): Action[AnyContent] = Action.async { implicit request =>
    request.headers.get("Authorization") match {
      case Some(authHeader) if authHeader.startsWith("Bearer ") =>
        val token = authHeader.substring(7)

        authService.logout(token).map {
          case Right(_) =>
            Ok(Json.obj(
              "success" -> true,
              "message" -> "Logged out successfully"
            ))

          case Left(error) => error match {
            case DomainError.AuthenticationFailed(msg) =>
              Unauthorized(Json.obj(
                "success" -> false,
                "message" -> msg
              ))
            case _ =>
              InternalServerError(Json.obj(
                "success" -> false,
                "message" -> "An error occurred during logout"
              ))
          }
        }

      case _ =>
        Future.successful(Unauthorized(Json.obj(
          "success" -> false,
          "message" -> "Missing or invalid Authorization header"
        )))
    }
  }

  /**
   * Change password endpoint
   * PUT /api/auth/change-password
   */
  def changePassword(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.headers.get("Authorization") match {
      case Some(authHeader) if authHeader.startsWith("Bearer ") =>
        val token = authHeader.substring(7)

        // Parse request body
        request.body.validate[ChangePasswordRequestDto].fold(
          errors => {
            Future.successful(BadRequest(Json.obj(
              "success" -> false,
              "message" -> "Invalid request format",
              "errors" -> JsError.toJson(errors)
            )))
          },
          changePasswordRequest => {
            // First verify token to get userId
            authService.verifyToken(token).flatMap {
              case Right(user) =>
                val userId = user.id

                authService.changePassword(
                  userId,
                  changePasswordRequest.oldPassword,
                  changePasswordRequest.newPassword,
                  token
                ).map {
                  case Right(_) =>
                    Ok(Json.obj(
                      "success" -> true,
                      "message" -> "Password changed successfully. Please login again."
                    ))

                  case Left(error) => error match {
                    case DomainError.AuthenticationFailed(msg) =>
                      Unauthorized(Json.obj(
                        "success" -> false,
                        "message" -> msg
                      ))
                    case DomainError.NotFound(msg) =>
                      NotFound(Json.obj(
                        "success" -> false,
                        "message" -> msg
                      ))
                    case DomainError.ValidationError(msg) =>
                      BadRequest(Json.obj(
                        "success" -> false,
                        "message" -> msg
                      ))
                    case _ =>
                      InternalServerError(Json.obj(
                        "success" -> false,
                        "message" -> "An error occurred during password change"
                      ))
                  }
                }

              case Left(error) => error match {
                case DomainError.AuthenticationFailed(msg) =>
                  Future.successful(Unauthorized(Json.obj(
                    "success" -> false,
                    "message" -> msg
                  )))
                case _ =>
                  Future.successful(InternalServerError(Json.obj(
                    "success" -> false,
                    "message" -> "An error occurred"
                  )))
              }
            }
          }
        )

      case _ =>
        Future.successful(Unauthorized(Json.obj(
          "success" -> false,
          "message" -> "Missing or invalid Authorization header"
        )))
    }
  }

  /**
   * Forgot password endpoint - Request password reset
   * POST /api/auth/forgot-password
   */
  def forgotPassword(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[ForgotPasswordRequestDto].fold(
      errors => {
        Future.successful(BadRequest(Json.obj(
          "success" -> false,
          "message" -> "Invalid request format",
          "errors" -> JsError.toJson(errors)
        )))
      },
      forgotPasswordRequest => {
        try {
          val email = Email(forgotPasswordRequest.email)

          // Reset URL (frontend should handle this)
          val resetUrl = forgotPasswordRequest.resetUrl.getOrElse("http://localhost:3000/reset-password")

          authService.forgotPassword(email, resetUrl).map {
            case Right(_) =>
              Ok(Json.obj(
                "success" -> true,
                "message" -> "If your email exists in our system, you will receive a password reset link"
              ))

            case Left(error) => error match {
              case DomainError.AuthenticationFailed(msg) =>
                BadRequest(Json.obj(
                  "success" -> false,
                  "message" -> msg
                ))
              case DomainError.InvalidOperation(msg) =>
                InternalServerError(Json.obj(
                  "success" -> false,
                  "message" -> msg
                ))
              case _ =>
                InternalServerError(Json.obj(
                  "success" -> false,
                  "message" -> "An error occurred"
                ))
            }
          }
        } catch {
          case ex: IllegalArgumentException =>
            Future.successful(BadRequest(Json.obj(
              "success" -> false,
              "message" -> ex.getMessage
            )))
        }
      }
    )
  }

  /**
   * Reset password endpoint - Reset password with token
   * POST /api/auth/reset-password
   */
  def resetPassword(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[ResetPasswordRequestDto].fold(
      errors => {
        Future.successful(BadRequest(Json.obj(
          "success" -> false,
          "message" -> "Invalid request format",
          "errors" -> JsError.toJson(errors)
        )))
      },
      resetPasswordRequest => {
        authService.resetPassword(
          resetPasswordRequest.token,
          resetPasswordRequest.newPassword
        ).map {
          case Right(_) =>
            Ok(Json.obj(
              "success" -> true,
              "message" -> "Password reset successfully. You can now login with your new password."
            ))

          case Left(error) => error match {
            case DomainError.AuthenticationFailed(msg) =>
              BadRequest(Json.obj(
                "success" -> false,
                "message" -> msg
              ))
            case DomainError.NotFound(msg) =>
              NotFound(Json.obj(
                "success" -> false,
                "message" -> msg
              ))
            case _ =>
              InternalServerError(Json.obj(
                "success" -> false,
                "message" -> "An error occurred during password reset"
              ))
          }
        }
      }
    )
  }
}
