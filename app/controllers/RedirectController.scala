package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._

/**
 * Simple redirect controller for the main app
 * Since business logic has been moved to microservices,
 * this just provides information about the microservices architecture
 */
@Singleton
class RedirectController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def index(): Action[AnyContent] = Action { implicit request =>
    Ok(views.html.index(
      "DDD + Microservices Backend",
      s"""
      |<div class="container">
      |  <h1>🏗️ DDD + Microservices Architecture</h1>
      |  <p>This backend has been transformed into a microservices architecture with Domain-Driven Design patterns.</p>
      |
      |  <h2>📋 Available Services</h2>
      |  <ul>
      |    <li><strong>API Gateway</strong> (Port 9000) - <a href="http://localhost:9000/health">http://localhost:9000</a></li>
      |    <li><strong>User Service</strong> (Port 9001) - <a href="http://localhost:9001/health">http://localhost:9001</a></li>
      |    <li><strong>Notification Service</strong> (Port 9002) - <a href="http://localhost:9002/health">http://localhost:9002</a></li>
      |    <li><strong>Analytics Service</strong> (Port 9003) - <a href="http://localhost:9003/health">http://localhost:9003</a></li>
      |  </ul>
      |
      |  <h2>🌐 API Endpoints</h2>
      |  <p>All API requests should go through the API Gateway at <strong>http://localhost:9000</strong></p>
      |  <ul>
      |    <li><code>GET /api/users</code> - List all users</li>
      |    <li><code>GET /api/users/{id}</code> - Get user by ID</li>
      |    <li><code>POST /api/users</code> - Create new user</li>
      |    <li><code>PUT /api/users/{id}</code> - Update user</li>
      |    <li><code>DELETE /api/users/{id}</code> - Delete user</li>
      |  </ul>
      |
      |  <h2>🔧 Infrastructure</h2>
      |  <ul>
      |    <li><strong>Consul</strong> - <a href="http://localhost:8500">http://localhost:8500</a> (Service Discovery)</li>
      |    <li><strong>Prometheus</strong> - <a href="http://localhost:9090">http://localhost:9090</a> (Metrics)</li>
      |    <li><strong>Grafana</strong> - <a href="http://localhost:3000">http://localhost:3000</a> (Monitoring)</li>
      |  </ul>
      |</div>
      """.stripMargin
    ))
  }

  def health(): Action[AnyContent] = Action {
    Ok(play.api.libs.json.Json.obj(
      "status" -> "ok",
      "service" -> "legacy-redirector",
      "message" -> "Please use microservices via API Gateway on port 9000",
      "timestamp" -> java.time.LocalDateTime.now().toString
    ))
  }
}