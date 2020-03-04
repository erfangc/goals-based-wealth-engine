package io.github.erfangc.assets

import org.springframework.stereotype.Service

@Service
class AssetService {
    fun getAssets(assetIds: List<String>): List<Asset> {
        TODO()
    }

    fun getAssetsByPublicIdentifiers(publicIdentifiers: List<PublicIdentifier>): List<Asset> {
        TODO()
    }
}
