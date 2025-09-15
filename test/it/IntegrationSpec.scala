package it

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

class IntegrationSpec extends PlaySpec with GuiceOneServerPerSuite {

  "Application" should {

    "work from within a server" in {
      val response = await(wsClient.url(s"http://localhost:$port").get())
      response.status mustBe OK
    }

    "handle API endpoints" in {
      val userData = Json.obj(
        "name" -> "Integration Test User",
        "email" -> "integration@test.com",
        "age" -> 28
      )

      val response = await(
        wsClient
          .url(s"http://localhost:$port/api/users")
          .withHttpHeaders("Content-Type" -> "application/json")
          .post(userData)
      )

      response.status mustBe CREATED
      (response.json \ "name").as[String] mustBe "Integration Test User"
    }
  }
}