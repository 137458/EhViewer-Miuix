package com.ehviewer.desktop

data class DesktopNotification(
    val id: Long,
    val message: String,
    val timestamp: Long,
)

object DesktopNotificationManager {
    fun post(
        current: List<DesktopNotification>,
        message: String,
        timestamp: Long,
        maxKeep: Int = 3,
        idProvider: () -> Long,
    ): List<DesktopNotification> {
        val newNotification = DesktopNotification(
            id = idProvider(),
            message = message,
            timestamp = timestamp,
        )
        val combined = current + newNotification
        return if (combined.size > maxKeep) {
            combined.takeLast(maxKeep)
        } else {
            combined
        }
    }

    fun dismiss(
        current: List<DesktopNotification>,
        id: Long,
    ): List<DesktopNotification> = current.filterNot { it.id == id }

    fun expire(
        current: List<DesktopNotification>,
        currentTime: Long,
        ttlMs: Long = 2500L,
    ): List<DesktopNotification> {
        val threshold = currentTime - ttlMs
        return current.filter { it.timestamp >= threshold }
    }
}
