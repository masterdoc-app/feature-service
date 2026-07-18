package pro.masterdoc.feature.config

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
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig(
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private val issuerUri: String,
    @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private val jwkSetUri: String,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val jwtConfigured = issuerUri.isNotBlank() || jwkSetUri.isNotBlank()

        http {
            csrf { disable() }
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }
            authorizeHttpRequests {
                authorize("/actuator/health", permitAll)
                authorize("/actuator/health/**", permitAll)
                authorize(anyRequest, authenticated)
            }
            if (jwtConfigured) {
                oauth2ResourceServer {
                    jwt { }
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
