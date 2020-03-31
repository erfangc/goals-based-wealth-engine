package io.github.erfangc.assets

import io.github.erfangc.assets.models.Asset
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/apis/assets")
class SearchAssetController(private val searchAssetService: SearchAssetService) {
    @GetMapping("_search")
    fun search(@RequestParam term: String): List<Asset> {
        return searchAssetService.search(term)
    }

    @PostMapping("_sync-with-dynamo-db")
    fun syncWithDynamoDB(): String {
        searchAssetService.syncWithDynamoDB()
        return "Ok"
    }
}