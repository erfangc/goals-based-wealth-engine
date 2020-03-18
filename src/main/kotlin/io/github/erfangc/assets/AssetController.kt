package io.github.erfangc.assets

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/apis/assets")
class AssetController(private val assetService: AssetService) {
    @PostMapping("_bulk-get-assets")
    fun getAssets(@RequestBody assetIds: List<String>): Map<String, Asset> {
        return assetService.getAssets(assetIds).associateBy { it.id }
    }
}