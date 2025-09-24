package application.shared

import play.api.Logging
import scala.concurrent.{ExecutionContext, Future}
import domain.shared.DomainError

/**
 * Base class for all Application Services
 * Provides common functionality like logging, error handling
 */
abstract class ApplicationBase extends Logging {

  /**
   * Log service operation start
   */
  protected def logOperationStart(operation: String, params: Map[String, Any] = Map.empty): Unit = {
    val paramsStr = if (params.nonEmpty) s" with params: $params" else ""
    logger.info(s"Starting $operation$paramsStr")
  }

  /**
   * Log service operation success
   */
  protected def logOperationSuccess(operation: String, result: Any = ""): Unit = {
    val resultStr = if (result.toString.nonEmpty) s" - Result: $result" else ""
    logger.info(s"Successfully completed $operation$resultStr")
  }

  /**
   * Log service operation error
   */
  protected def logOperationError(operation: String, error: Throwable): Unit = {
    logger.error(s"Error in $operation: ${error.getMessage}", error)
  }

  /**
   * Log domain error
   */
  protected def logDomainError(operation: String, error: DomainError): Unit = {
    logger.warn(s"Domain error in $operation: ${error.message}")
  }

  /**
   * Execute operation with automatic logging
   */
  protected def executeWithLogging[T](
    operation: String,
    params: Map[String, Any] = Map.empty
  )(
    block: => Future[Either[DomainError, T]]
  )(implicit ec: ExecutionContext): Future[Either[DomainError, T]] = {

    logOperationStart(operation, params)

    block.map { result =>
      result match {
        case Right(value) =>
          logOperationSuccess(operation, value)
          result
        case Left(error) =>
          logDomainError(operation, error)
          result
      }
    }.recover { error =>
      logOperationError(operation, error)
      Left(DomainError.ValidationError(s"Internal error in $operation: ${error.getMessage}"))
    }
  }
}