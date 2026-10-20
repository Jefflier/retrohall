package com.retrohall.core

/** Video filters offered by LibretroDroid. */
enum class ShaderOption(val label: String) {
    DEFAULT("原始像素"),
    SHARP("锐利平滑"),
    CRT("CRT 扫描线"),
    LCD("LCD 网格"),
    SMOOTH("高清平滑"),
}

enum class OrientationOption(val label: String) {
    AUTO("跟随重力感应"),
    LANDSCAPE("横屏"),
    PORTRAIT("竖屏"),
}

enum class SortOrder(val label: String) {
    TITLE("按名称"),
    RECENT("最近游玩"),
    ADDED("最近添加"),
    PLAY_TIME("游玩时长"),
}

object Formatters {
    fun duration(millis: Long): String {
        val minutes = millis / 60_000
        return when {
            minutes < 1 -> "不到 1 分钟"
            minutes < 60 -> "$minutes 分钟"
            else -> {
                val rest = minutes % 60
                if (rest == 0L) "${minutes / 60} 小时" else "${minutes / 60} 小时 $rest 分钟"
            }
        }
    }

    fun size(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
    }
}
