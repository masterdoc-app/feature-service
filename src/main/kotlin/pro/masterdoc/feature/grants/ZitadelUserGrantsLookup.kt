package pro.masterdoc.feature.grants

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

@Configuration
class UserGrantsLookupConfig(
    @Value("\${zitadel.issuer:}") private val issuer: String,
    @Value("\${zitadel.mgmt-token:}") private val mgmtToken: String,
    @Value("\${zitadel.project-id:}") private val projectId: String,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(UserGrantsLookupConfig::class.java)

    @Bean
    fun userGrantsLookup(): UserGrantsLookup {
        if (mgmtToken.isBlank() || projectId.isBlank() || issuer.isBlank()) {
            log.info("event=user_grants_lookup mode=empty reason=zitadel_mgmt_not_configured")
            return UserGrantsLookup { orgId, userId ->
                log.warn("event=user_grants_unconfigured orgId={} userId={}", orgId, userId)
                emptyList()
            }
        }
        log.info("event=user_grants_lookup mode=zitadel")
        return ZitadelUserGrantsLookup(issuer, mgmtToken, projectId, objectMapper)
    }
}

internal class ZitadelUserGrantsLookup(
    issuer: String,
    private val mgmtToken: String,
    private val projectId: String,
    private val objectMapper: ObjectMapper,
) : UserGrantsLookup {
    private val log = LoggerFactory.getLogger(ZitadelUserGrantsLookup::class.java)
    private val baseUrl = issuer.trimEnd('/')
    private val client =
        RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer $mgmtToken")
            .build()

    override fun grantKeys(orgId: String, userId: String): List<String> =
        try {
            val body =
                mapOf(
                    "query" to mapOf("offset" to 0, "limit" to 100, "asc" to true),
                    "queries" to
                        listOf(
                            mapOf("projectIdQuery" to mapOf("projectId" to projectId)),
                            mapOf("userIdQuery" to mapOf("userId" to userId)),
                        ),
                )
            val response =
                client
                    .post()
                    .uri("/management/v1/users/grants/_search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-zitadel-orgid", orgId)
                    .body(body)
                    .retrieve()
                    .body(String::class.java)
                    ?: return emptyList()
            val parsed = objectMapper.readValue(response, GrantsSearchResponse::class.java)
            parsed.result.flatMap { it.roleKeys }.distinct()
        } catch (e: Exception) {
            log.warn(
                "event=user_grants_lookup_failed orgId={} userId={} cause={}",
                orgId,
                userId,
                e.message,
            )
            emptyList()
        }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GrantsSearchResponse(
        val result: List<GrantRow> = emptyList(),
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GrantRow(
        val roleKeys: List<String> = emptyList(),
    )
}
