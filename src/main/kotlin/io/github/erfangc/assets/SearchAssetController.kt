package io.github.erfangc.assets

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/apis/assets/_search")
class SearchAssetController(private val searchAssetService: SearchAssetService) {
    @GetMapping
    fun search(@RequestParam term: String): List<Asset> {
        return searchAssetService.search(term)
    }
}