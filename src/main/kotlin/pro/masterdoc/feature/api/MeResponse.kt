package pro.masterdoc.feature.api

data class UserInfoDto(
    val id: String,
    val givenName: String?,
    val familyName: String?,
    val email: String?,
)

data class MeResponse(
    val userInfo: UserInfoDto,
    val features: List<String>,
)
