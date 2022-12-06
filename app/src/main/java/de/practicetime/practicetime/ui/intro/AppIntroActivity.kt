/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Michael Prommersberger
 * Additions and modifications, author Matthias Emde 
 */

package de.practicetime.practicetime.ui.intro

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.SlideBackgroundColorHolder
import de.practicetime.practicetime.BuildConfig
import de.practicetime.practicetime.R

const val FRAGMENT_TYPE_KEY = "introfragment"

enum class IntroFragmentType {
    FRAGMENT_LIBRARY,
    FRAGMENT_GOAL,
    FRAGMENT_SESSION,
}

class AppIntroActivity : AppIntro() {

    fun changeSlide() {
        goToNextSlide()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isWizardMode = true
        isColorTransitionsEnabled = true
        isSystemBackButtonLocked = true

        setImmersiveMode()
//        showStatusBar(false)
        setScrollDurationFactor(5)
        setDoneText(R.string.intro_text_done)

        // Call addSlide passing your Fragments.
        // You can use AppIntroFragment to use a pre-built fragment
        class WelcomeSlide : Fragment(R.layout.fragment_intro_welcome), SlideBackgroundColorHolder {
            override val defaultBackgroundColor = 0
            override val defaultBackgroundColorRes = R.color.primary_light

            override fun setBackgroundColor(backgroundColor: Int) {
                requireView().setBackgroundColor(backgroundColor)
            }
        }

        addSlide(WelcomeSlide())

        // Library
        val lBundle = Bundle()
        lBundle.putSerializable(FRAGMENT_TYPE_KEY, IntroFragmentType.FRAGMENT_LIBRARY)
        val libFragment = IntroFragment(R.color.library_item_color_10)
        libFragment.arguments = lBundle
        addSlide(libFragment)

        // Goal
        val gBundle = Bundle()
        gBundle.putSerializable(FRAGMENT_TYPE_KEY, IntroFragmentType.FRAGMENT_GOAL)
        val goalFragment = IntroFragment(R.color.library_item_color_5)
        goalFragment.arguments = gBundle
        addSlide(goalFragment)

        // Session
        val sBundle = Bundle()
        sBundle.putSerializable(FRAGMENT_TYPE_KEY, IntroFragmentType.FRAGMENT_SESSION)
        val sessionFragment = IntroFragment(R.color.library_item_color_9)
        sessionFragment.arguments = sBundle
        addSlide(sessionFragment)
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        // Decide what to do when the user clicks on "Skip"
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        // Decide what to do when the user clicks on "Done"
        if (!BuildConfig.DEBUG) {
//            val prefs = getSharedPreferences(
//                getString(R.string.filename_shared_preferences), Context.MODE_PRIVATE)
//            prefs.edit().putBoolean(PracticeTime.PREFERENCES_KEY_APPINTRO_DONE, true).apply()
        }
        finish()
    }
}
