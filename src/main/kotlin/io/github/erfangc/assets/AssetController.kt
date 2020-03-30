package io.github.erfangc.assets

import io.github.erfangc.assets.models.Asset
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/apis/assets")
class AssetController(private val assetService: AssetService) {
    @PostMapping("_bulk-get-assets")
    fun getAssets(@RequestBody assetIds: List<String>): Map<String, Asset> {
        return assetService.getAssets(assetIds).associateBy { it.id }
    }
    @GetMapping("{id}")
    fun getAsset(@PathVariable id: String): Asset? {
        return assetService.getAssets(listOf(id)).firstOrNull()
    }
}