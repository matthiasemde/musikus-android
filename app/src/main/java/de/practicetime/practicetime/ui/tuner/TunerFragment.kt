package de.practicetime.practicetime.ui.tuner

import android.animation.ValueAnimator
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchDetectionResult
import be.tarsos.dsp.pitch.PitchProcessor
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.round

private const val SAMPLE_RATE = 22050
private const val SAMPLES = SAMPLE_RATE / 5
private const val MAX_ROTATION_ANGLE: Float = 22f / 180 * PI.toFloat()
private const val MAX_ANIM_DURATION = 10  // duration of the animation if needle needs to travel from -MAX to +MAX
private const val IN_TUNE_CENTS = 4f

class TunerFragment : Fragment(R.layout.fragment_tuner) {

    data class Note(
        val name: String,
        val cents: Float
    )

    private var a4f = 440.0f
    private lateinit var needle: View
    private lateinit var tvNote: TextView
    private lateinit var tvHz: TextView
    private var animating = false
    private lateinit var anim: ValueAnimator
    private lateinit var note: Note
    private lateinit var audioDispatcher: AudioDispatcher

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupPitchHandler()
        needle = requireView().findViewById(R.id.fragment_tuner_needle)
        tvNote = requireView().findViewById(R.id.fragment_tuner_tv_note)
        tvHz = requireView().findViewById(R.id.fragment_tuner_tv_hz)
    }

    private fun radToDeg(deg: Float) = (deg * 180 / PI).toFloat()

    // TODO check for mic permission!
    private fun setupPitchHandler() {
        val pdh = PitchDetectionHandler { result: PitchDetectionResult, audioEvent: AudioEvent ->

            val hz = result.pitch
            note = getNote(hz, a4f)

            requireActivity().runOnUiThread {
                tvNote.text = note.name
                tvHz.text =
                    if (hz > 0)
                        "%.2f Hz".format(hz)
                    else
                        "- Hz"

                rotateNeedle(radToDeg((note.cents / 50) * MAX_ROTATION_ANGLE))

                if (abs(note.cents) < IN_TUNE_CENTS && hz > 0) {
                    needle.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.md_green_A400))
                    tvNote.apply {
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.md_green_A400))
                        setTypeface(null, Typeface.BOLD)
                    }
                } else {
                    needle.setBackgroundColor(PracticeTime.getThemeColor(R.attr.colorOnBackground, requireContext()))
                    tvNote.apply {
                        setTextColor(PracticeTime.getThemeColor(R.attr.colorOnBackground, requireContext()))
                        setTypeface(null, Typeface.NORMAL)
                    }
                }
            }

        }

        val bufferSize = (SAMPLES * 1.0f).toInt()     // multiplier possible up to 2.0f
        val processor = PitchProcessor(
            PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
            SAMPLE_RATE.toFloat(),
            bufferSize,
            pdh
        )

        audioDispatcher = AudioDispatcherFactory.fromDefaultMicrophone(
            SAMPLE_RATE,
            bufferSize,
            0
        )

        audioDispatcher.addAudioProcessor(processor)

        Thread(audioDispatcher).start()
    }

    private fun getNote(hz: Float, a4f_freq: Float): Note {

        if (hz < 0) return Note("-", 0f)

        val notes = arrayOf("A", "A♯ / B♭", "B", "C", "C♯ / D♭", "D", "D♯ / E♭", "E", "F", "F♯ / G♭", "G", "G♯ / A♭")

        val semi = log2(hz / a4f_freq) * 12.0f
        val note = (round(semi) % 12 + 12) % 12
        val cents = (semi - round(semi)) * 100

        return Note(notes[note.toInt()], cents)
    }


    private fun rotateNeedle(desiredAngle: Float) {
        if (animating) {
            animating = false
            anim.cancel()       // stop the animation
        }

        anim = ValueAnimator.ofFloat(
            needle.rotation,            // start at current angle
            desiredAngle                         // end at desired angle
        ).apply {

            val fractionOfMaxRotation = (needle.rotation - desiredAngle) / MAX_ROTATION_ANGLE
            duration = abs((MAX_ANIM_DURATION * fractionOfMaxRotation).toLong())

            addUpdateListener {
                needle.rotation = it.animatedValue as Float    // change rotation property
            }
            doOnEnd { animating = false }

            animating = true
            start()
        }
    }

    override fun onStop() {
        super.onStop()
        audioDispatcher.stop()
    }
}