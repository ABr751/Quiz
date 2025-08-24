package com.example.flagquiz.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.flagquiz.presentation.quiz.QuizState
import com.google.gson.Gson
import androidx.core.content.edit

class QuizPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "quiz_preferences",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    companion object {
        private const val KEY_QUIZ_STATE = "quiz_state"
        private const val KEY_QUIZ_START_TIME = "quiz_start_time"
        private const val KEY_IS_QUIZ_ACTIVE = "is_quiz_active"
    }

    fun saveQuizState(quizState: QuizState) {
        sharedPreferences.edit {
            putString(KEY_QUIZ_STATE, gson.toJson(quizState))
            putBoolean(KEY_IS_QUIZ_ACTIVE, !quizState.isQuizComplete)
            apply()
        }
    }

    fun loadQuizState(): QuizState? {
        val quizStateJson = sharedPreferences.getString(KEY_QUIZ_STATE, null)
        return if (quizStateJson != null) {
            try {
                gson.fromJson(quizStateJson, QuizState::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun getQuizStartTime(): Long {
        return sharedPreferences.getLong(KEY_QUIZ_START_TIME, 0L)
    }

    fun setQuizStartTime(lng: Long) {
        sharedPreferences.edit {
            putLong(KEY_QUIZ_START_TIME, lng)
            apply()
        }
    }


    fun isQuizActive(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_QUIZ_ACTIVE, false)
    }

    fun clearQuizState() {
        val editor = sharedPreferences.edit()
        editor.remove(KEY_QUIZ_STATE)
        editor.remove(KEY_QUIZ_START_TIME)
        editor.remove(KEY_IS_QUIZ_ACTIVE)
        editor.apply()
    }
}
