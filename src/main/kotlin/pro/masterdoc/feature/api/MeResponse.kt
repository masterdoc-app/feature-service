package pro.masterdoc.feature.api

data class UserInfoDto(
    val id: String,
    val givenName: String?,
    val familyName: String?,
    val email: String?,
    val orgName: String? = null,
)

data class MeResponse(
    val userInfo: UserInfoDto,
    val features: List<String>,
)
