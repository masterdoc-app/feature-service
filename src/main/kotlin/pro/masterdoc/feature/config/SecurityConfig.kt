package pro.masterdoc.feature.config

import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoders
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig(
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private val issuerUri: String,
    @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private val jwkSetUri: String,
) {
    private val log = LoggerFactory.getLogger(SecurityConfig::class.java)

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val issuerSet = issuerUri.isNotBlank()
        val jwkSetUriSet = jwkSetUri.isNotBlank()
        val jwtConfigured = issuerSet || jwkSetUriSet

        log.info(
            "event=startup jwtConfigured={} issuerSet={} jwkSetUriSet={}",
            jwtConfigured,
            issuerSet,
            jwkSetUriSet,
        )

        val authFailedEntryPoint = AuthenticationEntryPoint { request, response, authException ->
            log.warn(
                "event=auth_failed path={} reason={}",
                request.requestURI,
                authException.javaClass.simpleName,
            )
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
        }

        http {
            csrf { disable() }
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }
            authorizeHttpRequests {
                authorize("/actuator/health", permitAll)
                authorize("/actuator/health/**", permitAll)
                // Internal service-to-service (dashboard assignee eligibility).
                authorize("/users/*/features", permitAll)
                authorize(anyRequest, authenticated)
            }
            exceptionHandling {
                authenticationEntryPoint = authFailedEntryPoint
            }
            if (jwtConfigured) {
                oauth2ResourceServer {
                    jwt { }
                    authenticationEntryPoint = authFailedEntryPoint
                }
            }
        }
        return http.build()
    }

    @Bean
    @ConditionalOnExpression(
        "!'\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}'.isBlank() " +
            "|| !'\${spring.security.oauth2.resourceserver.jwt.issuer-uri:}'.isBlank()",
    )
    fun jwtDecoder(): JwtDecoder =
        when {
            jwkSetUri.isNotBlank() -> NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
            else -> JwtDecoders.fromIssuerLocation(issuerUri)
        }
}
