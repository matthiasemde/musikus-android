package de.practicetime.practicetime

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.practicetime.practicetime.entities.SectionWithCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FullscreenSessionActivity : AppCompatActivity() {

    private lateinit var dao: PTDao

    private lateinit var dateView: TextView
    private lateinit var ratingBarView: RatingBar
    private lateinit var practiceDurationView: TextView
    private lateinit var breakDurationView: TextView
    private lateinit var sectionListView: RecyclerView
    private lateinit var commentFieldView: TextView

    private val timeFormat: SimpleDateFormat = SimpleDateFormat("H:mm", Locale.getDefault())
    private val dateFormat: SimpleDateFormat = SimpleDateFormat("E dd.MM.yyyy", Locale.getDefault())

    private lateinit var sectionAdapter: SectionAdapter
    private val sectionAdapterData = ArrayList<SectionWithCategory>()

    private lateinit var commentDialog: BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_session)

        openDatabase()

        val sessionId = intent.extras?.getInt("KEY_SESSION")

        if (sessionId != null) {
            showFullscreenSession(sessionId)
        } else {
            exitActivity()
        }
    }

    private fun showFullscreenSession(id: Int) {
//        dateView = findViewById(R.id.fullscreen_session_date)
        ratingBarView = findViewById(R.id.fullscreen_session_rating_bar)
//        practiceDurationView = findViewById(R.id.fullscreen_session_practice_duration)
//        breakDurationView = findViewById(R.id.fullscreen_session_break_duration)
        sectionListView = findViewById(R.id.fullscreen_session_section_list)
        commentFieldView = findViewById(R.id.fullscreen_session_comment_field)

        // define the layout and adapter for the section list
        val sectionAdapter = SectionAdapter(sectionAdapterData, this)
        val lm = object : LinearLayoutManager(this) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }
        sectionListView.apply {
            layoutManager = lm
            adapter = sectionAdapter
        }

        lifecycleScope.launch {
            Log.d("doa", "$id, ${dao.getSessionWithSectionsWithCategories(id)}")
            val (session, sectionsWithCategories) = dao.getSessionWithSectionsWithCategories(id)

            val startTimestamp = Date(sectionsWithCategories.first().section.timestamp * 1000L)

            // compute the total practice time
            var practiceDuration = 0
            sectionsWithCategories.forEach { (section, _) ->
                practiceDuration += section.duration ?: 0
            }

            val breakDuration = session.break_duration

//            dateView.text = getString(R.string.fullscreen_session_date).format(
//                dateFormat.format(startTimestamp),
//                timeFormat.format(startTimestamp),
//            )
            ratingBarView.progress = session.rating
//            practiceDurationView.text = getTimeString(practiceDuration)
//            breakDurationView.text = getTimeString(breakDuration)
            commentFieldView.text = session.comment
            commentFieldView.setOnClickListener { showDialog() }

            sectionAdapterData.addAll(sectionsWithCategories)
            sectionAdapter.notifyItemRangeInserted(0, sectionsWithCategories.size)
        }
    }

    private fun getTimeString(duration: Int) : String {
        val hoursDur =  duration % 3600 / 60     // TODO change back eventually
        val minutesDur = duration % 60           // TODO change back eventually

        return if (hoursDur > 0) {
            "%dh %dmin".format(hoursDur, minutesDur)
        } else if (minutesDur == 0 && duration > 0){
            "<1min"
        } else {
            "%dmin".format(minutesDur)
        }
    }


    /*************************************************************************
     * Section Adapter
     *************************************************************************/

    private inner class SectionAdapter(
        private val sectionsWithCategories: List<SectionWithCategory>,
        private val context: Context
    ) : RecyclerView.Adapter<SectionAdapter.ViewHolder>() {

        private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val sectionColor: ImageView = view.findViewById(R.id.sectionColor)
            val sectionName: TextView = view.findViewById(R.id.sectionName)
            val sectionDuration: TextView = view.findViewById(R.id.sectionDuration)
        }

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            // Create a new view, which defines the UI of the list item
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.view_session_summary_section, viewGroup, false)

            return ViewHolder(view)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            // Get element from your dataset at this position
            val (section, category) = sectionsWithCategories[position]

            // set the color to the category color
            val categoryColors =  context.resources.getIntArray(R.array.category_colors)
            viewHolder.sectionColor.backgroundTintList = ColorStateList.valueOf(
                categoryColors[category.colorIndex]
            )

            val sectionDuration = section.duration ?: 0

            // contents of the view with that element
            viewHolder.sectionName.text = category.name
            viewHolder.sectionDuration.text = getTimeString(sectionDuration)
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = sectionsWithCategories.size
    }

    /*************************************************************************
     * Utility functions
     *************************************************************************/

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            this,
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }

    private fun exitActivity() {
        // go back to MainActivity, make new intent so MainActivity gets reloaded and shows new session
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    /*************************************************************************
     * Comment Fragment
     *************************************************************************/

    private fun showDialog() {
        val fragmentManager = supportFragmentManager
        val newFragment = CommentDialogFragment(::editCommentHandler)
        val args = Bundle()
        args.putCharSequence("comment", commentFieldView.text)
        newFragment.arguments = args
        newFragment.show(fragmentManager, "dialog")
    }

    private fun editCommentHandler(newComment: CharSequence) {
        commentFieldView.text = newComment
    }

    class CommentDialogFragment(
        private val onConfirmHandler: (CharSequence) -> Unit
    ) : BottomSheetDialogFragment() {

        private lateinit var commentText: CharSequence

        private lateinit var commentFieldView: EditText

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            return inflater.inflate(R.layout.bottom_sheet_dialog_comment, container, false)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setStyle(STYLE_NO_TITLE, R.style.CommentDialog)
            commentText = arguments?.getCharSequence("comment") ?: ""
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            commentFieldView = view.findViewById<EditText>(R.id.comment_dialog_text_field).also {
                it.setText(commentText)
                if(it.requestFocus()) {
                    lifecycleScope.launch {
                        delay(150L) // TODO: WTF
                        (requireActivity()
                            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                            .showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                    }
                }
            }
            view.findViewById<ImageButton>(R.id.comment_dialog_confirm).setOnClickListener {
                onConfirmHandler(commentFieldView.text)
                dismiss()
            }
        }
    }

}