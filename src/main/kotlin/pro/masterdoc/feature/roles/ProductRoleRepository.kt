package pro.masterdoc.feature.roles

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ProductRoleRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
) {
    private val stringListType = object : TypeReference<List<String>>() {}

    fun count(orgId: String): Int =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM product_roles WHERE org_id = ?",
            Int::class.java,
            orgId,
        ) ?: 0

    fun list(orgId: String): List<ProductRole> =
        jdbcTemplate.query(
            "SELECT role_id, title_ru, features FROM product_roles WHERE org_id = ? ORDER BY role_id",
            { rs, _ ->
                ProductRole(
                    roleId = rs.getString("role_id"),
                    titleRu = rs.getString("title_ru"),
                    features = objectMapper.readValue(rs.getString("features"), stringListType),
                )
            },
            orgId,
        )

    fun insertAll(orgId: String, roles: List<ProductRole>) {
        jdbcTemplate.batchUpdate(
            "INSERT INTO product_roles (org_id, role_id, title_ru, features) VALUES (?, ?, ?, ?)",
            roles.map { role ->
                arrayOf<Any>(
                    orgId,
                    role.roleId,
                    role.titleRu,
                    objectMapper.writeValueAsString(role.features),
                )
            },
        )
    }

    fun update(orgId: String, role: ProductRole): Boolean =
        jdbcTemplate.update(
            "UPDATE product_roles SET title_ru = ?, features = ? WHERE org_id = ? AND role_id = ?",
            role.titleRu,
            objectMapper.writeValueAsString(role.features),
            orgId,
            role.roleId,
        ) > 0
}
