package infrastructure.email

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger

/**
 * Email Service Interface
 */
trait EmailService {
  def sendPasswordResetEmail(to: String, resetToken: String, resetUrl: String): Future[Boolean]
}

/**
 * Mock Email Service Implementation
 * In production, integrate with SendGrid, AWS SES, or SMTP server
 */
@Singleton
class MockEmailService @Inject()()(implicit ec: ExecutionContext) extends EmailService {

  private val logger = Logger(this.getClass)

  override def sendPasswordResetEmail(
    to: String,
    resetToken: String,
    resetUrl: String
  ): Future[Boolean] = Future {
    // Mock implementation - just log the email
    val resetLink = s"$resetUrl?token=$resetToken"

    logger.info(s"""
      |==================== PASSWORD RESET EMAIL ====================
      |To: $to
      |Subject: Reset Your Password
      |
      |Hello,
      |
      |You requested to reset your password. Click the link below to reset it:
      |
      |$resetLink
      |
      |This link will expire in 1 hour.
      |
      |If you didn't request this, please ignore this email.
      |
      |==============================================================
      |""".stripMargin)

    // Return true to indicate email sent successfully
    true
  }
}

/**
 * Production Email Service using SMTP
 * Uncomment and configure when ready for production
 */
/*
@Singleton
class SmtpEmailService @Inject()(
  config: Configuration
)(implicit ec: ExecutionContext) extends EmailService {

  private val smtpHost = config.get[String]("email.smtp.host")
  private val smtpPort = config.get[Int]("email.smtp.port")
  private val smtpUser = config.get[String]("email.smtp.user")
  private val smtpPassword = config.get[String]("email.smtp.password")
  private val fromEmail = config.get[String]("email.from")

  override def sendPasswordResetEmail(
    to: String,
    resetToken: String,
    resetUrl: String
  ): Future[Boolean] = Future {
    val props = new Properties()
    props.put("mail.smtp.host", smtpHost)
    props.put("mail.smtp.port", smtpPort.toString)
    props.put("mail.smtp.auth", "true")
    props.put("mail.smtp.starttls.enable", "true")

    val session = Session.getInstance(props, new Authenticator() {
      override def getPasswordAuthentication: PasswordAuthentication = {
        new PasswordAuthentication(smtpUser, smtpPassword)
      }
    })

    val message = new MimeMessage(session)
    message.setFrom(new InternetAddress(fromEmail))
    message.setRecipients(Message.RecipientType.TO, to)
    message.setSubject("Reset Your Password")

    val resetLink = s"$resetUrl?token=$resetToken"
    val htmlContent = s"""
      |<html>
      |<body>
      |  <h2>Password Reset Request</h2>
      |  <p>You requested to reset your password.</p>
      |  <p>Click the link below to reset it:</p>
      |  <p><a href="$resetLink">Reset Password</a></p>
      |  <p>This link will expire in 1 hour.</p>
      |  <p>If you didn't request this, please ignore this email.</p>
      |</body>
      |</html>
      |""".stripMargin

    message.setContent(htmlContent, "text/html; charset=utf-8")
    Transport.send(message)

    true
  }
}
*/
