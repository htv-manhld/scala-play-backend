package domain.shared

/**
 * Generic paginated response container
 * @param data The items in the current page
 * @param pagination Pagination metadata
 * @tparam T The type of items
 */
case class PaginatedResponse[T](
  data: Seq[T],
  pagination: PaginationInfo
)

case class PaginationInfo(
  page: Int,
  size: Int,
  total: Long,
  totalPages: Int
)

object PaginationInfo {
  def apply(page: Int, size: Int, total: Long): PaginationInfo = {
    val totalPages = if (size > 0) Math.ceil(total.toDouble / size).toInt else 0
    PaginationInfo(page, size, total, totalPages)
  }
}
