package io.github.erfangc.assets.models

data class Allocations(
        val assetClass: AssetClassAllocation = AssetClassAllocation(),
        val gicsSectors: GicsAllocation = GicsAllocation(),
        val bondRatings: BondRatingsAllocation = BondRatingsAllocation()
)