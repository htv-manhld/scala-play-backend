package services

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import services.user.UserService
import models.domain.{User, UserCreateRequest}
import scala.concurrent.duration._

class UserServiceSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "UserService" should {

    "create and retrieve user" in {
      val userService = inject[UserService]
      val createRequest = UserCreateRequest("Test User", "test@example.com", 25)

      val createdUser = await(userService.createUser(createRequest), 5.seconds)
      createdUser.name mustBe "Test User"
      createdUser.email mustBe "test@example.com"
      createdUser.age mustBe 25

      val retrievedUser = await(userService.getUser(createdUser.id), 5.seconds)
      retrievedUser mustBe Some(createdUser)
    }

    "return None for non-existent user" in {
      val userService = inject[UserService]
      val result = await(userService.getUser(999L), 5.seconds)
      result mustBe None
    }
  }
}