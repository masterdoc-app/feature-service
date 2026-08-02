package pro.masterdoc.feature.roles

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import pro.masterdoc.feature.features.FeatureCatalog

@Service
class ProductRoleService(
    private val repository: ProductRoleRepository,
) {
    companion object {
        private val ROLE_IDS = setOf("admin", "dispatcher", "engineer", "manager", "requester")
        private val DEFAULTS =
            listOf(
                ProductRole("admin", "Админ", listOf("admin", "black_box", "equipment", "asset_qr")),
                ProductRole("dispatcher", "Диспетчер", listOf("map", "board", "ai", "charts")),
                ProductRole("engineer", "Инженер", listOf("engineer")),
                ProductRole("manager", "Менеджер", listOf("reports")),
                ProductRole("requester", "Заявитель", listOf("tickets")),
            )
    }

    @Transactional
    fun listWithSeed(orgId: String): List<ProductRole> {
        requireOrgId(orgId)
        if (repository.count(orgId) == 0) {
            repository.insertAll(orgId, DEFAULTS)
        } else {
            val existingIds = repository.list(orgId).map { it.roleId }.toSet()
            val missing = DEFAULTS.filter { it.roleId !in existingIds }
            if (missing.isNotEmpty()) {
                repository.insertAll(orgId, missing)
            }
        }
        val roles = repository.list(orgId)
        val admin = roles.firstOrNull { it.roleId == "admin" }
        if (admin != null && "asset_qr" !in admin.features) {
            repository.update(
                orgId,
                admin.copy(features = (admin.features + "asset_qr").distinct().sorted()),
            )
            return repository.list(orgId)
        }
        return roles
    }

    @Transactional
    fun update(orgId: String, roleId: String, titleRu: String?, features: List<String>): ProductRole {
        requireOrgId(orgId)
        validateRoleId(roleId)
        if (features.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "features must not be empty")
        }
        val unknown = features.firstOrNull { it !in FeatureCatalog.ALL }
        if (unknown != null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown feature: $unknown")
        }
        if (roleId == "admin" && "admin" !in features) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "admin role must include feature admin",
            )
        }

        val current = listWithSeed(orgId).firstOrNull { it.roleId == roleId }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown role: $roleId")
        val updated = current.copy(titleRu = titleRu ?: current.titleRu, features = features.distinct().sorted())
        repository.update(orgId, updated)
        return updated
    }

    private fun validateRoleId(roleId: String) {
        if (roleId !in ROLE_IDS) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown role: $roleId")
        }
    }

    private fun requireOrgId(orgId: String) {
        if (orgId.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Org-Id required")
        }
    }
}
