package com.ts.phi.views

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.ts.phi.utils.SettingsRepository

class SettingDialog(private val context: Context) {

    private var dialog: Dialog? = null
    private var performanceSwitch: Switch? = null
    private var onConfirmListener: ((Boolean) -> Unit)? = null

    // 使用抽离的 Repository
    private val settings = SettingsRepository.getInstance(context)

    fun show() {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(20), dpToPx(24), dpToPx(16))
        }

        val titleText = TextView(context).apply {
            text = "性能設定"
            textSize = 20f
            setTextColor(Color.BLACK)
            paint.isFakeBoldText = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(16) }
        }

        // Switch 容器
        val switchContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(8) }
        }

        performanceSwitch = Switch(context).apply {
            isChecked = settings.is2AxisPerformanceOnly  // 使用 Repository
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val switchLabel = TextView(context).apply {
            text = "2軸性能調整のみ"
            textSize = 16f
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = dpToPx(12) }
        }

        switchContainer.apply {
            addView(performanceSwitch)
            addView(switchLabel)
        }

        val descText = TextView(context).apply {
            text = "入力を性能調整として強制判定"
            textSize = 12f
            setTextColor(Color.GRAY)
        }

        container.apply {
            addView(titleText)
            addView(switchContainer)
            addView(descText)
        }

        dialog = AlertDialog.Builder(context)
            .setView(container)
            .setPositiveButton("确定") { _, _ ->
                val isChecked = performanceSwitch?.isChecked ?: false
                settings.is2AxisPerformanceOnly = isChecked  // 保存到 Repository
                onConfirmListener?.invoke(isChecked)
            }
            .setNegativeButton("取消", null)
            .create()

        dialog?.show()
    }

    fun setOnConfirmListener(listener: (Boolean) -> Unit) {
        onConfirmListener = listener
    }

    // 便捷方法：直接通过 Repository 读取状态，无需显示 Dialog
    fun get2AxisPerformanceOnly(): Boolean = settings.is2AxisPerformanceOnly

    fun set2AxisPerformanceOnly(enabled: Boolean) {
        settings.is2AxisPerformanceOnly = enabled
    }

    fun dismiss() = dialog?.dismiss()

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}