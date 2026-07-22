package pro.masterdoc.feature.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import pro.masterdoc.feature.features.FeatureCatalog
import pro.masterdoc.feature.features.FeatureDefinition

data class FeaturesResponse(
    val items: List<FeatureDefinition>,
)

@RestController
class FeaturesController(
    private val featureCatalog: FeatureCatalog,
) {
    @GetMapping("/features")
    fun getFeatures(): FeaturesResponse = FeaturesResponse(items = featureCatalog.catalog())
}
