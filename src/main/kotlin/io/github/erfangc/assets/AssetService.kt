package io.github.erfangc.assets

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

@Service
class AssetService {

    val assetLookup = jacksonObjectMapper()
            .readValue<Map<String, Asset>>(ClassPathResource("assets.json").inputStream)

    fun getAssets(assetIds: List<String>): List<Asset> {
        return assetIds.map {
            assetLookup[it] ?: throw RuntimeException("cannot find assetId $it")
        }
    }

    fun getAssetsByPublicIdentifiers(publicIdentifiers: List<PublicIdentifier>): List<Asset> {
        TODO()
    }

}
