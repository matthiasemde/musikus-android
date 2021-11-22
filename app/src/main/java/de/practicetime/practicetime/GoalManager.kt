package de.practicetime.practicetime

import de.practicetime.practicetime.entities.*

class GoalManager(
    private val goals: ArrayList<Goal>,
    private val categoriesWithGoals: Map<Int, CategoryWithGoals>,
) {

    init {

    }

    fun commitSection(newSection: PracticeSection) {
        var categoryWithGoals: CategoryWithGoals

        categoriesWithGoals[newSection.category_id].apply {
            assert(this != null)
            categoryWithGoals = this as CategoryWithGoals
        }

        for (goal in categoryWithGoals.goals) {
            goal.target += newSection.duration ?: 0
        }
    }
}