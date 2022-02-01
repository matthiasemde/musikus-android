package de.practicetime.practicetime.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.github.appintro.AppIntroPageTransformerType
import de.practicetime.practicetime.R
import de.practicetime.practicetime.ui.goals.GoalsFragment

class AppIntroActivity : AppIntro() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Call addSlide passing your Fragments.
        // You can use AppIntroFragment to use a pre-built fragment
        addSlide(AppIntroFragment.createInstance(
            title = "Welcome...",
            description = "This is the first slide of the example",
            backgroundColorRes = R.color.md_red_200
        ))
        addSlide(AppIntroFragment.createInstance(
            title = "...Let's get started!",
            description = "This is the last slide, I won't annoy you more :)",
            backgroundColorRes = R.color.md_red_200
        ))

        setTransformer(AppIntroPageTransformerType.Flow)
        addSlide(AppIntroFragment.createInstance(
            title = "...Let's get started!",
            description = "This is the last slide, I won't annoy you more :)",
            backgroundColorRes = R.color.md_red_200
        ))
        addSlide(AppIntroFragment.createInstance(
            title = "...Let's get started!",
            description = "This is the last slide, I won't annoy you more :)",
            backgroundColorRes = R.color.md_red_200
        ))

        addSlide(GoalsFragment())
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