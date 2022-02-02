package de.practicetime.practicetime.ui.intro

import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.github.appintro.AppIntroPageTransformerType
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
        addSlide(AppIntroFragment.createInstance(
            title = "Welcome...",
            description = "This is the beginning of a new era",
            backgroundColorRes = R.color.md_red_300
        ))

        val lBundle = Bundle()
        lBundle.putSerializable(FRAGMENT_TYPE_KEY, IntroFragmentType.FRAGMENT_LIBRARY)
        val libFragment = IntroFragment(R.color.md_amber_300)
        libFragment.arguments = lBundle
        addSlide(libFragment)

        this.goToNextSlide()

        val gBundle = Bundle()
        gBundle.putSerializable(FRAGMENT_TYPE_KEY, IntroFragmentType.FRAGMENT_GOAL)
        val goalFragment = IntroFragment(R.color.md_pink_300)
        goalFragment.arguments = gBundle
        addSlide(goalFragment)

        val sBundle = Bundle()
        sBundle.putSerializable(FRAGMENT_TYPE_KEY, IntroFragmentType.FRAGMENT_SESSION)
        val sessionFragment = IntroFragment(R.color.md_teal_400)
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
        finish()
    }
}