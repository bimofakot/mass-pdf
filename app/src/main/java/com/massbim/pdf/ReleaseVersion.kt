package com.massbim.pdf

internal object ReleaseVersion {
    fun isNewer(candidate: String, current: String): Boolean {
        val candidateParts = parse(candidate) ?: return false
        val currentParts = parse(current) ?: return false

        for (index in 0 until maxOf(candidateParts.size, currentParts.size)) {
            val candidatePart = candidateParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (candidatePart != currentPart) return candidatePart > currentPart
        }
        return false
    }

    private fun parse(version: String): List<Int>? {
        val components = version.removePrefix("v").split(".")
        if (components.size < 3) return null
        return components.map { it.toIntOrNull() ?: return null }
    }
}
