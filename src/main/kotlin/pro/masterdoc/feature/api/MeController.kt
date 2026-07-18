package pro.masterdoc.feature.api

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import pro.masterdoc.feature.features.MeService

@RestController
class MeController(
    private val meService: MeService,
) {
    @GetMapping("/me")
    fun getMe(@AuthenticationPrincipal jwt: Jwt): MeResponse = meService.getMe(jwt)
}
