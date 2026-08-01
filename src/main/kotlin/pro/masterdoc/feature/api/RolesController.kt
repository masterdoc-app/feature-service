package pro.masterdoc.feature.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import pro.masterdoc.feature.roles.ProductRole
import pro.masterdoc.feature.roles.ProductRoleService

data class ProductRoleResponse(
    val id: String,
    val titleRu: String,
    val features: List<String>,
) {
    companion object {
        fun from(role: ProductRole) = ProductRoleResponse(role.roleId, role.titleRu, role.features)
    }
}

data class ProductRolesResponse(
    val items: List<ProductRoleResponse>,
)

data class UpdateProductRoleRequest(
    val titleRu: String? = null,
    val features: List<String>,
)

@RestController
class RolesController(
    private val service: ProductRoleService,
) {
    @GetMapping("/roles")
    fun list(@RequestHeader("X-Org-Id") orgId: String): ProductRolesResponse =
        ProductRolesResponse(service.listWithSeed(orgId).map(ProductRoleResponse::from))

    @PutMapping("/roles/{roleId}")
    fun update(
        @RequestHeader("X-Org-Id") orgId: String,
        @PathVariable roleId: String,
        @RequestBody request: UpdateProductRoleRequest,
    ): ProductRoleResponse =
        ProductRoleResponse.from(service.update(orgId, roleId, request.titleRu, request.features))
}
