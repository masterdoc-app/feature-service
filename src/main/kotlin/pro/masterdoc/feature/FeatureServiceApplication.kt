package pro.masterdoc.feature

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FeatureServiceApplication

fun main(args: Array<String>) {
    runApplication<FeatureServiceApplication>(*args)
}
