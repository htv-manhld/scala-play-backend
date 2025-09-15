package modules

import com.google.inject.AbstractModule
import models.persistence.UserRepository
import services.user.UserService

class DatabaseModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[UserRepository])
    bind(classOf[UserService])
  }
}