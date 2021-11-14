package de.practicetime.practicetime
//
//import android.os.Bundle
//import android.view.View
//import android.widget.Button
//import androidx.activity.OnBackPressedCallback
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.lifecycleScope
//import androidx.navigation.Navigation
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import androidx.room.Room
//import de.practicetime.practicetime.entities.SessionWithSectionsWithCategories
//import kotlinx.coroutines.launch
//
//private var dao: PTDao? = null
//
//
//class SessionSummaryFragment : Fragment(R.layout.fragment_session_summary) {
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//
//        // prevent going back to active Session while in Summary Screen
//        requireActivity().onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                // ain't doin' nothin'
//            }
//        })
//
//
//        // load the database
//        openDatabase()
//
//        // initialize adapter and recyclerView for showing category buttons from database
//        val sessionWithSectionsWithCategories = ArrayList<SessionWithSectionsWithCategories>()
//        val sessionSummaryAdapter = SessionSummaryAdapter(requireContext(), sessionWithSectionsWithCategories)
//
//        val sessionSummary = view.findViewById<RecyclerView>(R.id.sessionSummary)
//        sessionSummary.layoutManager = LinearLayoutManager(requireContext())
//        sessionSummary.adapter = sessionSummaryAdapter
//
//        lifecycleScope.launch {
//            sessionWithSectionsWithCategories.add(dao?.getSessionsWithSectionsWithCategories()?.last()!!)
//
//            // notifyDataSetChanged necessary here since all items might have changed
//            sessionSummaryAdapter.notifyDataSetChanged()
//        }
//
//        val saveSession = view.findViewById<Button>(R.id.save)
//        saveSession.setOnClickListener{goToSessionList()}
//    }
//
//    private fun goToSessionList() {
//        Navigation.findNavController(requireView())
//            .navigate(R.id.action_sessionSummaryFragment_to_sessionListFragment)
//    }
//
//    private fun openDatabase() {
//        val db = Room.databaseBuilder(
//            requireContext(),
//            PTDatabase::class.java, "pt-database"
//        ).build()
//        dao = db.ptDao
//    }
//}