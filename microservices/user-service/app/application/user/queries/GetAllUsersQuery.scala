package application.user.queries

case class GetAllUsersQuery(
  page: Int = 0,
  size: Int = 20
)