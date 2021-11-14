package de.practicetime.practicetime

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import de.practicetime.practicetime.entities.SessionWithSectionsWithCategories
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


private var dao: PTDao? = null

class MetronomeFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        openDatabase()

        var monthList = ArrayList<ArrayList<SessionWithSectionsWithCategories>>()
        val sessionMonthListAdapter = SessionMonthListAdapter(monthList)
        val sessionListMonths = view.findViewById<ExpandableListView>(R.id.sessionListMonths)
        sessionListMonths.setAdapter(sessionMonthListAdapter)

        lifecycleScope.launch {
            var currentMonthList = ArrayList<SessionWithSectionsWithCategories>()

            dao?.getSessionsWithSectionsWithCategories()!!.also { sessions ->
                var currentMonth: Int
                Calendar.getInstance().also { newDate ->
                    newDate.timeInMillis =
                        sessions.first().sections.first().section.timestamp * 1000L
                    currentMonth = newDate.get(Calendar.MONTH)
                }

                currentMonthList.add(sessions.first())
                sessions.drop(1)

                sessions.forEach { session ->
                    var sessionMonth: Int

                    Calendar.getInstance().also { newDate ->
                        newDate.timeInMillis =
                            session.sections.first().section.timestamp * 1000L
                        sessionMonth = newDate.get(Calendar.MONTH)
                    }

                    if (sessionMonth == currentMonth) {
                        currentMonthList.add(session)
                    } else {
                        monthList.add(currentMonthList)
                        currentMonthList = ArrayList()
                    }
                }
                if(currentMonthList.isNotEmpty()) {
                    monthList.add(currentMonthList)
                }
            }

            // notifyDataSetChanged necessary here since all items might have changed
            sessionMonthListAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_metronome, container, false)
    }

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            requireContext(),
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }

    private class SessionMonthListAdapter(
        private val months: ArrayList<ArrayList<SessionWithSectionsWithCategories>>,
    ) : BaseExpandableListAdapter() {
        private val monthFormat: SimpleDateFormat = SimpleDateFormat("M")

        override fun getGroupCount(): Int {
            return months.size
        }

        override fun getChildrenCount(groupPosition: Int): Int {
            return months[groupPosition].size
        }

        override fun getGroup(groupPosition: Int): ArrayList<SessionWithSectionsWithCategories> {
            return months[groupPosition]
        }

        override fun getChild(groupPosition: Int, childPosition: Int): SessionWithSectionsWithCategories {
            return months[groupPosition][childPosition]
        }

        override fun getGroupId(groupPosition: Int): Long {
            return groupPosition.toLong()
        }

        override fun getChildId(groupPosition: Int, childPosition: Int): Long {
            return childPosition.toLong()
        }

        override fun hasStableIds(): Boolean {
            return false
        }

        override fun getGroupView(
            groupPosition: Int,
            isExpanded: Boolean,
            convertView: View?,
            parent: ViewGroup?
        ): View {
            if(convertView != null) {
                return convertView
            }

            val view = LayoutInflater.from(parent?.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)

            val title = view.findViewById<TextView>(android.R.id.text1)

            val date = Date(
                months[groupPosition].first().sections.first().section.timestamp * 1000L
            )
            title.text = monthFormat.format(date)
            return view
        }

        override fun getChildView(
            groupPosition: Int,
            childPosition: Int,
            isLastChild: Boolean,
            convertView: View?,
            parent: ViewGroup?
        ): View {
            val view = LayoutInflater.from(parent?.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)

            val title = view.findViewById<TextView>(android.R.id.text1)
            title.text = "lolol"
            return view
        }

        override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
            return true
        }
    }
}