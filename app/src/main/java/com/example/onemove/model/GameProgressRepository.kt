package com.example.onemove.model

import android.content.Context
import android.content.SharedPreferences

data class SessionStats(
    val levelNumber: Int,
    var attempts: Int = 0,
    var retries: Int = 0,
    var completions: Int = 0,
    var lastCompletionTimeSec: Float = 0f
)

class GameProgressRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val inMemoryStats = mutableMapOf<Int, SessionStats>()

    fun getHighestUnlockedLevel(): Int {
        return prefs.getInt(KEY_HIGHEST_UNLOCKED, 1).coerceIn(1, 12)
    }

    fun isLevelUnlocked(levelNumber: Int): Boolean {
        return levelNumber <= getHighestUnlockedLevel()
    }

    fun isLevelCompleted(levelNumber: Int): Boolean {
        val completedSet = prefs.getStringSet(KEY_COMPLETED_LEVELS, emptySet()) ?: emptySet()
        return completedSet.contains(levelNumber.toString())
    }

    fun markLevelCompleted(levelNumber: Int, completionTimeSec: Float) {
        val currentUnlocked = getHighestUnlockedLevel()
        val nextLevel = (levelNumber + 1).coerceAtMost(12)
        val newHighest = maxOf(currentUnlocked, nextLevel)

        val completedSet = (prefs.getStringSet(KEY_COMPLETED_LEVELS, emptySet()) ?: emptySet()).toMutableSet()
        completedSet.add(levelNumber.toString())

        prefs.edit()
            .putInt(KEY_HIGHEST_UNLOCKED, newHighest)
            .putStringSet(KEY_COMPLETED_LEVELS, completedSet)
            .apply()

        // Update local session stats
        val stats = inMemoryStats.getOrPut(levelNumber) { SessionStats(levelNumber) }
        stats.completions++
        stats.lastCompletionTimeSec = completionTimeSec
    }

    fun recordAttempt(levelNumber: Int) {
        val stats = inMemoryStats.getOrPut(levelNumber) { SessionStats(levelNumber) }
        stats.attempts++
    }

    fun recordRetry(levelNumber: Int) {
        val stats = inMemoryStats.getOrPut(levelNumber) { SessionStats(levelNumber) }
        stats.retries++
    }

    fun getSessionStats(levelNumber: Int): SessionStats {
        return inMemoryStats.getOrPut(levelNumber) { SessionStats(levelNumber) }
    }

    companion object {
        private const val PREFS_NAME = "one_move_game_progress"
        private const val KEY_HIGHEST_UNLOCKED = "highest_unlocked_level"
        private const val KEY_COMPLETED_LEVELS = "completed_levels_set"
    }
}
