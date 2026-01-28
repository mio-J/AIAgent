package com.ts.phi.utils

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "AppSettings"
        private const val KEY_2AXIS_PERF_ONLY = "key_2axis_performance_only"

        @Volatile
        private var instance: SettingsRepository? = null

        // 单例获取（推荐）
        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context).also { instance = it }
            }
        }
    }

    /**
     * 2軸性能調整のみ の状態
     * 使用: settings.is2AxisPerformanceOnly = true
     */
    var is2AxisPerformanceOnly: Boolean
        get() = prefs.getBoolean(KEY_2AXIS_PERF_ONLY, false)
        set(value) = prefs.edit().putBoolean(KEY_2AXIS_PERF_ONLY, value).apply()

    /**
     * 监听设置变化（可选）
     */
    fun registerChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    /**
     * 清除所有设置（调试用）
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}