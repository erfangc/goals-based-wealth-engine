package io.github.erfangc.assets

data class BondRatingsAllocation(
        val usGovernment: Double = 0.0,
        val aaa: Double = 0.0,
        val aa: Double = 0.0,
        val a: Double = 0.0,
        val bbb: Double = 0.0,
        val bb: Double = 0.0,
        val b: Double = 0.0,
        val belowB: Double = 0.0,
        val others: Double = 0.0
)