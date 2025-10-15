package filters

import javax.inject.Inject
import play.api.http.HttpFilters
import play.filters.cors.CORSFilter
import play.filters.headers.SecurityHeadersFilter
import play.filters.hosts.AllowedHostsFilter

/**
 * Custom Filters configuration excluding CSRF filter
 * CSRF is not needed for JWT-based API authentication
 */
class Filters @Inject()(
  corsFilter: CORSFilter,
  securityHeadersFilter: SecurityHeadersFilter,
  allowedHostsFilter: AllowedHostsFilter
) extends HttpFilters {
  override val filters = Seq(
    corsFilter,
    securityHeadersFilter,
    allowedHostsFilter
  )
}
