package io.github.erfangc.assets

data class Allocations(
        val assetClass: AssetClassAllocation,
        val gicsSectors: GicsAllocation,
        val bondRatingsAllocation: BondRatingsAllocation
)