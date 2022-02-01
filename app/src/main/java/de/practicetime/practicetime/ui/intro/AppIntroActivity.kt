package de.practicetime.practicetime.ui.intro

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.github.appintro.AppIntroPageTransformerType
import de.practicetime.practicetime.R

const val FRAGMENT_TYPE_KEY = "introfragment"

enum class IntroFragmentType(val id: Int) {
    FRAGMENT_LIBRARY(0),
    FRAGMENT_GOAL(1)
}

class AppIntroActivity : AppIntro() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransformer(AppIntroPageTransformerType.Flow)

        // Call addSlide passing your Fragments.
        // You can use AppIntroFragment to use a pre-built fragment
        addSlide(AppIntroFragment.createInstance(
            title = "Welcome...",
            description = "This is the beginning of a new era",
            backgroundColorRes = R.color.md_red_200
        ))

        val lBundle = Bundle()
        lBundle.putSerializable(FRAGMENT_TYPE_KEY, IntroFragmentType.FRAGMENT_LIBRARY)
        val libFragment = IntroFragment()
        libFragment.arguments = lBundle
        addSlide(libFragment)

        val gBundle = Bundle()
        gBundle.putSerializable(FRAGMENT_TYPE_KEY, IntroFragmentType.FRAGMENT_GOAL)
        val introFragment = IntroFragment()
        introFragment.arguments = gBundle
        addSlide(introFragment)



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