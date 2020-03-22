package com.randomlychosenbytes.openfoodfactsdumper

data class Portion(var name: String, var weight: Float) {
    override fun toString(): String {
        return """"$name":${weight}"""
    }
}
