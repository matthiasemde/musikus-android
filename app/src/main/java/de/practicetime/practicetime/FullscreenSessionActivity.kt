package de.practicetime.practicetime

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import de.practicetime.practicetime.entities.PracticeSection
import de.practicetime.practicetime.entities.PracticeSession
import de.practicetime.practicetime.entities.SectionWithCategory
import de.practicetime.practicetime.entities.SessionWithSectionsWithCategories
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import android.app.Activity
import android.util.Log
import androidx.fragment.app.FragmentManager


class FullscreenSessionActivity : AppCompatActivity() {

    private lateinit var dao: PTDao

    private lateinit var ratingBarView: RatingBar
    private lateinit var sectionListView: RecyclerView
    private lateinit var commentFieldView: TextView

    private lateinit var sectionAdapter: SectionAdapter
    private val sectionAdapterData = ArrayList<SectionWithCategory>()

    private lateinit var editSectionTimeDialog: EditTimeDialog
    private lateinit var confirmationDialog: AlertDialog

    private var sessionWithSectionsWithCategories: SessionWithSectionsWithCategories? = null
    private var selectedSection: PracticeSection? = null

    private var showCommentPlaceholder = true

    private var sessionEdited = false

    override fun onBackPressed() {
        if(!sessionEdited) return super.onBackPressed()
        confirmationDialog.apply {
            setMessage(getString(R.string.discard_changes_dialog_message))
            show()
            getButton(AlertDialog.BUTTON_NEGATIVE).setText(R.string.discard_changes_dialog_cancel)
            getButton(AlertDialog.BUTTON_POSITIVE).apply {
                setText(R.string.discard_dialog_ok)
                setOnClickListener {
                    dismiss()
                    super.onBackPressed()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_session)

        openDatabase()

        initConfirmationDialog()

        sessionEdited = false

        editSectionTimeDialog = EditTimeDialog(
            this,
            title = getString(R.string.edit_section_time_dialog_title)
        ) {
            editSectionDurationHandler(selectedSection, it)
        }

        val sessionId = intent.extras?.getInt("KEY_SESSION")

        if (sessionId != null) {
            showFullscreenSession(sessionId)
            findViewById<MaterialButton>(R.id.fullscreen_session_cancel).setOnClickListener {
                if(!sessionEdited) return@setOnClickListener exitActivity()
                confirmationDialog.apply {
                    setMessage(getString(R.string.discard_changes_dialog_message))
                    show()
                    getButton(AlertDialog.BUTTON_NEGATIVE).setText(R.string.discard_changes_dialog_cancel)
                    getButton(AlertDialog.BUTTON_POSITIVE).apply {
                        setText(R.string.discard_dialog_ok)
                        setOnClickListener {
                            dismiss()
                            exitActivity()
                        }
                    }
                }
            }

            findViewById<MaterialButton>(R.id.fullscreen_session_save).setOnClickListener {
                confirmationDialog.apply {
                    setMessage(getString(R.string.confirm_changes_dialog_message))
                    show()
                    getButton(AlertDialog.BUTTON_NEGATIVE).setText(R.string.dialogCancel)
                    getButton(AlertDialog.BUTTON_POSITIVE).apply {
                        setText(R.string.confirm_changes_dialog_ok)
                        setOnClickListener {
                            lifecycleScope.launch {
                                dao.updateSession(
                                    sessionId,
                                    newRating = ratingBarView.rating.toInt(),
                                    newSections = sectionAdapterData,
                                    newComment = if(showCommentPlaceholder) ""
                                        else commentFieldView.text.toString(),
                                )
                                dismiss()
                                exitActivity()
                            }
                        }
                    }
                }
            }
        } else {
            exitActivity()
        }

    }

    private fun showFullscreenSession(id: Int) {
        ratingBarView = findViewById(R.id.fullscreen_session_rating_bar)
        sectionListView = findViewById(R.id.fullscreen_session_section_list)
        commentFieldView = findViewById(R.id.fullscreen_session_comment_field)

        // define the layout and adapter for the section list
        sectionAdapter = SectionAdapter(sectionAdapterData, this)
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
            sessionWithSectionsWithCategories = dao.getSessionWithSectionsWithCategories(id)
            val (session, sectionsWithCategories) = sessionWithSectionsWithCategories!!

            ratingBarView.progress = session.rating
            ratingBarView.setOnRatingBarChangeListener { _, _, _ ->
                sessionEdited = true
            }

            showCommentPlaceholder = session.comment?.isBlank() == true

            if(showCommentPlaceholder)
                commentFieldView.setText(R.string.endSessionDialogCommentField)
            else
                commentFieldView.text = session.comment

            commentFieldView.setOnClickListener { showCommentDialog() }

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

    private fun editSectionDurationHandler(section: PracticeSection?, newSectionDuration: Int) {
        section?.duration = newSectionDuration
        sectionAdapterData.indexOfFirst {
            it.section.id == section?.id
        }.also {
            sectionAdapter.notifyItemChanged(it)
        }
        sessionEdited = true
    }

    private fun initConfirmationDialog() {
        confirmationDialog = AlertDialog.Builder(this).apply {
            setPositiveButton(R.string.discard_dialog_ok) { dialog, _ ->
                dialog.dismiss()
            }
            setNegativeButton(R.string.dialogCancel) { dialog, _ ->
                dialog.cancel()
            }
        }.create()
        confirmationDialog.setCanceledOnTouchOutside(false)
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
                .inflate(R.layout.view_fullscreen_session_section, viewGroup, false)

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

            viewHolder.itemView.setOnClickListener {
                selectedSection = section
                editSectionTimeDialog.show(sectionDuration)
            }
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

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view = currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(this)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
    /*************************************************************************
     * Comment Fragment
     *************************************************************************/

    private fun showCommentDialog(altText: String? = null) {
        val fragmentManager = supportFragmentManager
        val newFragment = CommentDialogFragment(::editCommentHandler) { comment ->
            // if the comment has not changed, don't show the confirmation dialog
            if(comment == if(showCommentPlaceholder) "" else commentFieldView.text)
                return@CommentDialogFragment

            hideKeyboard()
            lifecycleScope.launch {
                delay(25L) // We need this delay so the keyboard has time to close
                confirmationDialog.apply {
                    setMessage(context.getString(R.string.discard_comment_dialog_message))
                    show()
                    getButton(AlertDialog.BUTTON_NEGATIVE).apply{
                        setText(R.string.discard_comment_dialog_cancel)
                        setOnClickListener {
                            cancel()
                            showCommentDialog(comment)
                        }
                    }
                    getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.discard_dialog_ok)
                }
            }
        }
        val args = Bundle()
        args.putCharSequence("comment", altText ?: if(showCommentPlaceholder) "" else commentFieldView.text)
        newFragment.arguments = args
        newFragment.show(fragmentManager, "dialog")
    }

    private fun editCommentHandler(newComment: String) {
        showCommentPlaceholder = newComment.isBlank()
        commentFieldView.text = if (showCommentPlaceholder)
                getString(R.string.endSessionDialogCommentField)
            else
                newComment
        sessionEdited = true
    }

    class CommentDialogFragment(
        private val onConfirmHandler: (String) -> Unit,
        private val onCancelHandler: (String) -> Unit,
    ) : BottomSheetDialogFragment() {

        private lateinit var commentText: String

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
            arguments?.getCharSequence("comment").let {
                if(it != null) commentText = it.toString()
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            commentFieldView = view.findViewById<EditText>(R.id.comment_dialog_text_field)
            commentFieldView.also {
                it.setText(commentText)
                if(it.requestFocus()) {
                    (requireActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .showSoftInput(it, 0)
                }
            }

            view.findViewById<ImageButton>(R.id.comment_dialog_confirm).setOnClickListener {
                onConfirmHandler(commentFieldView.text.toString())
                dismiss()
            }
        }

        override fun onCancel(dialog: DialogInterface) {
            onCancelHandler(commentFieldView.text.toString())
            super.onCancel(dialog)
        }
    }

}