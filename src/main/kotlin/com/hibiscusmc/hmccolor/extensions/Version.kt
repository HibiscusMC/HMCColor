package com.hibiscusmc.hmccolor.extensions

import io.papermc.paper.ServerBuildInfo

@JvmRecord
data class Version(val major: Int, val minor: Int, val build: Int) {
    // Constructor to parse a version string like "1.20.1"
    private constructor(version: String) : this(fromString(version)[0], fromString(version)[1], fromString(version)[2])

    companion object {
        // Helper method to parse version string into components
        private fun fromString(version: String): IntArray {
            val parts: Array<String?> = version.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            require(parts.size == 3) { "Version string must be in format 'major.minor.build'" }
            try {
                val major = parts[0]!!.toInt()
                val minor = parts[1]!!.toInt()
                val build = parts[2]!!.toInt()
                return intArrayOf(major, minor, build)
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Invalid version number format: $version", e)
            }
        }

        private val currentVersion = Version(ServerBuildInfo.buildInfo().minecraftVersionId())

        fun atleast(version: String): Boolean {
            val other = Version(version)
            when {
                currentVersion.major > other.major -> return true
                currentVersion.major == other.major -> when {
                    currentVersion.minor > other.minor -> return true
                    currentVersion.minor == other.minor -> return currentVersion.build >= other.build
                }
            }
            return false
        }
    }
}