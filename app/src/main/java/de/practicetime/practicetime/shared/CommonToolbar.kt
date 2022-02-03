package de.practicetime.practicetime.shared

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R

fun setCommonToolbar(context: Activity, toolbar: androidx.appcompat.widget.Toolbar) {
    val prefs = context.getPreferences(Context.MODE_PRIVATE)

    toolbar.apply {
        inflateMenu(R.menu.common_menu)

        setToolbarIcons(context, toolbar)
        setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.topToolbarThemeSwitchAuto -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    prefs.edit().putInt(
                        PracticeTime.PREFERENCES_KEY_THEME,
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM).apply()
                }
                R.id.topToolbarThemeSwitchDark -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    prefs.edit().putInt(
                        PracticeTime.PREFERENCES_KEY_THEME,
                        AppCompatDelegate.MODE_NIGHT_YES).apply()
                }
                R.id.topToolbarThemeSwitchLight -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    prefs.edit().putInt(
                        PracticeTime.PREFERENCES_KEY_THEME,
                        AppCompatDelegate.MODE_NIGHT_NO).apply()
                }
            }
            setToolbarIcons(context, toolbar)
            return@setOnMenuItemClickListener true
        }
    }

}

private fun setToolbarIcons(context: Activity, toolbar: androidx.appcompat.widget.Toolbar) {
    toolbar.menu.findItem(R.id.topToolbarThemeSwitchAuto).icon = null
    toolbar.menu.findItem(R.id.topToolbarThemeSwitchDark).icon = null
    toolbar.menu.findItem(R.id.topToolbarThemeSwitchLight).icon = null

    val itemToSetIcon = when (AppCompatDelegate.getDefaultNightMode()) {
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.MODE_NIGHT_UNSPECIFIED ->
            R.id.topToolbarThemeSwitchAuto
        AppCompatDelegate.MODE_NIGHT_NO -> R.id.topToolbarThemeSwitchLight
        AppCompatDelegate.MODE_NIGHT_YES -> R.id.topToolbarThemeSwitchDark
        else -> R.id.topToolbarThemeSwitchDark
    }

    toolbar.menu.findItem(itemToSetIcon).apply {
        val iconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_check_small)!!
        // tint it like this because iconTintList requires API >=26
        DrawableCompat.setTint(iconDrawable, PracticeTime.getThemeColor(R.attr.colorOnSurfaceLowerContrast, context))
        icon = iconDrawable
    }
}