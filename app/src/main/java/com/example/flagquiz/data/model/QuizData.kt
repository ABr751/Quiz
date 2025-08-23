package com.example.flagquiz.data.model

import com.google.gson.annotations.SerializedName

data class QuizData(
    val questions: List<Question>
)

data class Question(
    @SerializedName("answer_id")
    val answerId: Int,
    val countries: List<Country>,
    @SerializedName("country_code")
    val countryCode: String
)

data class Country(
    @SerializedName("country_name")
    val countryName: String,
    val id: Int
)

