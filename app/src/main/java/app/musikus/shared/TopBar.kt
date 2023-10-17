/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 * Additions and modifications, author Michael Prommersberger
 */

package app.musikus.shared

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import app.musikus.Musikus
import app.musikus.R
import app.musikus.ui.overflowitems.AboutActivity
import com.google.android.material.appbar.MaterialToolbar

interface TopBarUiState {
    val title: String
    val showBackButton: Boolean
}

fun setCommonToolbar(
    context: Activity,
    toolbar: MaterialToolbar,
    uncommonItemHandler: (itemId : Int) -> Unit,
) {
    toolbar.apply {
        inflateMenu(R.menu.common_menu)

        setToolbarIcons(context, toolbar)
        setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.commonToolbarThemeSwitchAuto -> {
//                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
//                    Musikus.prefs.edit().putInt(
//                        Musikus.PREFERENCES_KEY_THEME,
//                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM).apply()
                }
                R.id.commonToolbarThemeSwitchDark -> {
//                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
//                    Musikus.prefs.edit().putInt(
//                        Musikus.PREFERENCES_KEY_THEME,
//                        AppCompatDelegate.MODE_NIGHT_YES).apply()
                }
                R.id.commonToolbarThemeSwitchLight -> {
//                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
//                    Musikus.prefs.edit().putInt(
//                        Musikus.PREFERENCES_KEY_THEME,
//                        AppCompatDelegate.MODE_NIGHT_NO).apply()
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

private fun setToolbarIcons(context: Activity, toolbar: MaterialToolbar) {
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
        DrawableCompat.setTint(iconDrawable, Musikus.getThemeColor(R.attr.colorOnSurfaceLowerContrast, context))
        icon = iconDrawable
    }
}
