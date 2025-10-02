package application.user.queries

// Simple query - returns all users up to limit (no pagination metadata)
case class GetAllUsersQuery(
  limit: Int = 10000
)

// Paginated query - returns users with pagination metadata
case class GetUsersPaginatedQuery(
  page: Int = 0,
  size: Int = 20
)