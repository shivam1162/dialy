package com.dialy.app.core.notification

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object AppNotificationManager {
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var storageFile: File? = null

    fun initialize(context: Context) {
        if (storageFile == null) {
            storageFile = File(context.applicationContext.filesDir, "app_notifications.json")
            loadFromFile()
        }
    }

    fun postNotification(
        title: String,
        message: String,
        type: NotificationType = NotificationType.INFO
    ) {
        val newNotification = AppNotification(
            title = title,
            message = message,
            type = type,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        val currentList = _notifications.value.toMutableList()
        currentList.add(0, newNotification)
        if (currentList.size > 50) {
            currentList.subList(50, currentList.size).clear()
        }
        _notifications.value = currentList
        updateUnreadCount()
        saveToFile()
    }

    fun postWarning(title: String, message: String) {
        postNotification(title, message, NotificationType.WARNING)
    }

    fun postError(title: String, message: String) {
        postNotification(title, message, NotificationType.ERROR)
    }

    fun postSuccess(title: String, message: String) {
        postNotification(title, message, NotificationType.SUCCESS)
    }

    fun postInfo(title: String, message: String) {
        postNotification(title, message, NotificationType.INFO)
    }

    fun markAllAsRead() {
        val current = _notifications.value
        if (current.any { !it.isRead }) {
            _notifications.value = current.map { it.copy(isRead = true) }
            _unreadCount.value = 0
            saveToFile()
        }
    }

    fun removeNotification(id: String) {
        val updated = _notifications.value.filterNot { it.id == id }
        _notifications.value = updated
        updateUnreadCount()
        saveToFile()
    }

    fun clearAll() {
        _notifications.value = emptyList()
        _unreadCount.value = 0
        saveToFile()
    }

    private fun updateUnreadCount() {
        _unreadCount.value = _notifications.value.count { !it.isRead }
    }

    private fun saveToFile() {
        val file = storageFile ?: return
        try {
            val jsonArray = JSONArray()
            for (item in _notifications.value) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("message", item.message)
                    put("type", item.type.name)
                    put("timestamp", item.timestamp)
                    put("isRead", item.isRead)
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadFromFile() {
        val file = storageFile ?: return
        if (!file.exists()) return
        try {
            val content = file.readText()
            if (content.isBlank()) return
            val jsonArray = JSONArray(content)
            val list = mutableListOf<AppNotification>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val typeName = obj.optString("type", NotificationType.INFO.name)
                val type = runCatching { NotificationType.valueOf(typeName) }.getOrDefault(NotificationType.INFO)
                list.add(
                    AppNotification(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        message = obj.getString("message"),
                        type = type,
                        timestamp = obj.getLong("timestamp"),
                        isRead = obj.optBoolean("isRead", false)
                    )
                )
            }
            _notifications.value = list
            updateUnreadCount()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
