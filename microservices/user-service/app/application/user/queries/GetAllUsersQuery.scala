package application.user.queries

// Simple query - returns all users up to limit (no pagination metadata)
case class GetAllUsersQuery(
  limit: Int = 10000,
  search: Option[String] = None,
  orderBy: Option[String] = None,
  orderDirection: Option[String] = None,
  ignoreId: Option[Long] = None,
  status: Option[Int] = None,
  createdFrom: Option[String] = None,
  createdTo: Option[String] = None
)

// Paginated query - returns users with pagination metadata
case class GetUsersPaginatedQuery(
  page: Int = 0,
  size: Int = 20,
  search: Option[String] = None,
  orderBy: Option[String] = None,
  orderDirection: Option[String] = None,
  ignoreId: Option[Long] = None,
  status: Option[Int] = None,
  createdFrom: Option[String] = None,
  createdTo: Option[String] = None
)