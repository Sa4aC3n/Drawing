package com.art.drawingkids

import java.util.Calendar

/**
 * Manages daily drawing challenges for kids.
 * Provides a featured daily lesson and congratulatory tracking.
 */
class DailyChallengeManager(private val repository: DrawingLessonRepository) {

    fun getTodayChallenge(): DrawingCategory? {
        val allLessons = repository.getLessons(CategoryGroup.ALL)
        if (allLessons.isEmpty()) return null
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val index = dayOfYear % allLessons.size
        return allLessons[index]
    }

    fun isTodayChallenge(imageArrayKey: String): Boolean {
        val today = getTodayChallenge() ?: return false
        return today.imageArrayKey == imageArrayKey
    }
}
