package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

class UserControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "UserController GET" should {

    "return 404 when user not found" in {
      val request = FakeRequest(GET, "/users/999")
      val result = route(app, request).get

      status(result) mustBe NOT_FOUND
      contentType(result) mustBe Some("application/json")
      (contentAsJson(result) \ "error").as[String] mustBe "User not found"
    }
  }

  "UserController POST" should {

    "create user with valid data" in {
      val userData = Json.obj(
        "name" -> "John Doe",
        "email" -> "john@example.com",
        "age" -> 30
      )

      val request = FakeRequest(POST, "/users")
        .withHeaders("Content-Type" -> "application/json")
        .withBody(userData)

      val result = route(app, request).get

      status(result) mustBe CREATED
      contentType(result) mustBe Some("application/json")
    }

    "return bad request with invalid data" in {
      val invalidData = Json.obj("invalid" -> "data")

      val request = FakeRequest(POST, "/users")
        .withHeaders("Content-Type" -> "application/json")
        .withBody(invalidData)

      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
    }
  }
}