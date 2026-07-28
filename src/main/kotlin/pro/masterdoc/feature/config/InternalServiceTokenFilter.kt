package pro.masterdoc.feature.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Fail-closed shared-secret gate for service-to-service routes (host-mapped ports).
 * Header: [HEADER] — same name as black-box / gateway internal calls.
 */
class InternalServiceTokenFilter(
    private val expectedToken: String,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(InternalServiceTokenFilter::class.java)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath ?: "")
        return !USER_FEATURES_PATH.matches(path)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (expectedToken.isBlank()) {
            log.warn("event=auth_denied path={} reason=token_not_configured", request.requestURI)
            response.sendError(HttpStatus.UNAUTHORIZED.value())
            return
        }
        val got = request.getHeader(HEADER)
        when {
            got.isNullOrBlank() -> {
                log.warn("event=auth_denied path={} reason=missing", request.requestURI)
                response.sendError(HttpStatus.UNAUTHORIZED.value())
            }
            got != expectedToken -> {
                log.warn("event=auth_denied path={} reason=mismatch", request.requestURI)
                response.sendError(HttpStatus.UNAUTHORIZED.value())
            }
            else -> filterChain.doFilter(request, response)
        }
    }

    companion object {
        const val HEADER = "X-Internal-Token"
        private val USER_FEATURES_PATH = Regex("^/users/[^/]+/features$")
    }
}
