package de.practicetime.practicetime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room


private var dao: PTDao? = null

class MetronomeFragment : Fragment(R.layout.fragment_library) {

     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        openDatabase()
    }

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            requireContext(),
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }
}