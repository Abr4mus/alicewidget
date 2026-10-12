package com.alice.homewidget.widget

object WidgetSkinHelper {
    fun getWidgetBackground(theme: String): Int = WidgetRenderEngine.getThemeBackground(theme)
    fun getTileBackground(theme: String): Int = WidgetRenderEngine.getTileBackground(theme)
    fun getBtnBackground(theme: String): Int = WidgetRenderEngine.getButtonBackground(theme)
    fun getTextColorPrimary(theme: String): Int = WidgetRenderEngine.getTextPrimaryColor(theme)
    fun getTextColorSecondary(theme: String): Int = WidgetRenderEngine.getTextSecondaryColor(theme)
    fun getAccentHumidityColor(theme: String): Int = WidgetRenderEngine.getHumidityColor(theme)
}
