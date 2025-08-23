package com.example.flagquiz.data.repository

import android.content.Context
import com.example.flagquiz.R
import com.example.flagquiz.data.model.Question
import com.example.flagquiz.data.model.QuizData
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader

class QuizRepository {
    
    fun loadQuestions(context: Context): List<Question> {
        return try {
            val inputStream = context.resources.openRawResource(R.raw.questions)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }
            
            val gson = Gson()
            val quizData = gson.fromJson(jsonString, QuizData::class.java)
            
            quizData.questions
        } catch (e: Exception) {
            // Return empty list if JSON loading fails
            emptyList()
        }
    }
}

