package com.ts.phi.views

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.ts.phi.utils.SettingsRepository

class SettingDialog(private val context: Context) {

    private var dialog: AlertDialog? = null
    private var performanceSwitch: Switch? = null
    private var onConfirmListener: ((Boolean) -> Unit)? = null

    private val settings = SettingsRepository.getInstance(context)

    // 深色主题配色
    private val colorBackground = Color.parseColor("#1E1E1E")
    private val colorSurface = Color.parseColor("#252525")      // Switch容器背景
    private val colorTitle = Color.parseColor("#90CAF9")        // 浅蓝强调
    private val colorTextPrimary = Color.parseColor("#E0E0E0")
    private val colorTextSecondary = Color.parseColor("#9E9E9E")
    private val colorAccent = Color.parseColor("#90CAF9")       // Switch打开颜色
    private val colorSwitchTrackOff = Color.parseColor("#4A4A4A") // Switch轨道关闭颜色
    private val colorSwitchThumbOff = Color.parseColor("#888888") // Switch按钮关闭颜色

    fun show() {
        // 根容器
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(20), dpToPx(24), dpToPx(8))
        }

        // 标题
        val titleText = TextView(context).apply {
            text = "性能設定"
            textSize = 22f
            setTextColor(colorTitle)
            paint.isFakeBoldText = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(20)
            }
        }

        // Switch 容器
        val switchContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            background = createRoundedRectDrawable(colorSurface, dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(16)
            }
        }

        performanceSwitch = Switch(context).apply {
            isChecked = settings.is2AxisPerformanceOnly

            // 初始化颜色
            updateSwitchColor(isChecked)

            // 监听状态变化，实时更新颜色
            setOnCheckedChangeListener { _, isChecked ->
                updateSwitchColor(isChecked)
            }

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val switchLabel = TextView(context).apply {
            text = "2軸性能調整のみ"
            textSize = 16f
            setTextColor(colorTextPrimary)
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            ).apply {
                marginStart = dpToPx(16)
            }
        }

        switchContainer.apply {
            addView(performanceSwitch)
            addView(switchLabel)
        }

        // 描述文字
        val descText = TextView(context).apply {
            text = ""
            textSize = 14f
            setTextColor(colorTextSecondary)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(8)
            }
        }

        container.apply {
            addView(titleText)
            addView(switchContainer)
            addView(descText)
        }

        // 创建对话框
        dialog = AlertDialog.Builder(context)
            .setView(container)
            .setPositiveButton("確定", null)
            .setNegativeButton("キャンセル", null)
            .create()

        dialog?.window?.apply {
            setBackgroundDrawable(createDialogBackground())
            setDimAmount(0.6f)
        }

        dialog?.show()

        // 设置按钮
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            setTextColor(colorAccent)
            setOnClickListener {
                val isChecked = performanceSwitch?.isChecked ?: false
                settings.is2AxisPerformanceOnly = isChecked
                onConfirmListener?.invoke(isChecked)
                dismiss()
            }
        }

        dialog?.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            setTextColor(colorTextSecondary)
        }
    }

    // 更新Switch颜色的辅助方法
    private fun updateSwitchColor(isChecked: Boolean) {
        performanceSwitch?.apply {
            if (isChecked) {
                // 打开状态：浅蓝色
                trackTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#6690CAF9")) // 半透明轨道
                thumbTintList = android.content.res.ColorStateList.valueOf(colorAccent) // 实心按钮
            } else {
                // 关闭状态：灰色
                trackTintList = android.content.res.ColorStateList.valueOf(colorSwitchTrackOff)
                thumbTintList = android.content.res.ColorStateList.valueOf(colorSwitchThumbOff)
            }
        }
    }

    fun setOnConfirmListener(listener: (Boolean) -> Unit) {
        onConfirmListener = listener
    }

    fun get2AxisPerformanceOnly(): Boolean = settings.is2AxisPerformanceOnly

    fun set2AxisPerformanceOnly(enabled: Boolean) {
        settings.is2AxisPerformanceOnly = enabled
    }

    fun dismiss() = dialog?.dismiss()

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    private fun createRoundedRectDrawable(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = radius.toFloat()
        }
    }

    private fun createDialogBackground(): InsetDrawable {
        val backgroundDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(colorBackground)
            cornerRadius = dpToPx(24).toFloat()
        }
        return InsetDrawable(backgroundDrawable, dpToPx(16), dpToPx(24), dpToPx(16), dpToPx(24))
    }
}