package com.example.data.model

/**
 * Real-time metrics and telemetry tracked during active VPN sessions.
 */
data class VpnStats(
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val durationSeconds: Long = 0L,
    val currentPublicIp: String? = null,
    val isFetchingIp: Boolean = false,
    val ipFetchError: String? = null
) {
    val formattedDownload: String
        get() = formatBytes(bytesIn)

    val formattedUpload: String
        get() = formatBytes(bytesOut)

    val formattedDuration: String
        get() {
            val hours = durationSeconds / 3600
            val minutes = (durationSeconds % 3600) / 60
            val seconds = durationSeconds % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    companion object {
        fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1_073_741_824L -> String.format("%.2f GB", bytes.toDouble() / 1_073_741_824L)
                bytes >= 1_048_576L -> String.format("%.2f MB", bytes.toDouble() / 1_048_576L)
                bytes >= 1_024L -> String.format("%.1f KB", bytes.toDouble() / 1_024L)
                else -> "$bytes B"
            }
        }
    }
}
