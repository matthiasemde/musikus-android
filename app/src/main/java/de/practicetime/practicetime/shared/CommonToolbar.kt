package de.practicetime.practicetime.shared

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.ui.overflowitems.AboutActivity

fun setCommonToolbar(
    context: Activity,
    toolbar: androidx.appcompat.widget.Toolbar,
    uncommonItemHandler: (itemId : Int) -> Unit,
) {
    val prefs = context.getSharedPreferences(context.getString(R.string.filename_shared_preferences), Context.MODE_PRIVATE)

    toolbar.apply {
        inflateMenu(R.menu.common_menu)

        setToolbarIcons(context, toolbar)
        setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.commonToolbarThemeSwitchAuto -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    prefs.edit().putInt(
                        PracticeTime.PREFERENCES_KEY_THEME,
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM).apply()
                }
                R.id.commonToolbarThemeSwitchDark -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    prefs.edit().putInt(
                        PracticeTime.PREFERENCES_KEY_THEME,
                        AppCompatDelegate.MODE_NIGHT_YES).apply()
                }
                R.id.commonToolbarThemeSwitchLight -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    prefs.edit().putInt(
                        PracticeTime.PREFERENCES_KEY_THEME,
                        AppCompatDelegate.MODE_NIGHT_NO).apply()
                }
                R.id.commonToolbarInfo -> {
                    context.startActivity(Intent(context, AboutActivity::class.java))
                }
                else -> uncommonItemHandler(it.itemId)
            }
            setToolbarIcons(context, toolbar)
            return@setOnMenuItemClickListener true
        }
    }

}

private fun setToolbarIcons(context: Activity, toolbar: androidx.appcompat.widget.Toolbar) {
    toolbar.menu.findItem(R.id.commonToolbarThemeSwitchAuto).icon = null
    toolbar.menu.findItem(R.id.commonToolbarThemeSwitchDark).icon = null
    toolbar.menu.findItem(R.id.commonToolbarThemeSwitchLight).icon = null

    val itemToSetIcon = when (AppCompatDelegate.getDefaultNightMode()) {
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.MODE_NIGHT_UNSPECIFIED ->
            R.id.commonToolbarThemeSwitchAuto
        AppCompatDelegate.MODE_NIGHT_NO -> R.id.commonToolbarThemeSwitchLight
        AppCompatDelegate.MODE_NIGHT_YES -> R.id.commonToolbarThemeSwitchDark
        else -> R.id.commonToolbarThemeSwitchDark
    }

    toolbar.menu.findItem(itemToSetIcon).apply {
        val iconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_check_small)!!
        // tint it like this because iconTintList requires API >=26
        DrawableCompat.setTint(iconDrawable, PracticeTime.getThemeColor(R.attr.colorOnSurfaceLowerContrast, context))
        icon = iconDrawable
    }
}