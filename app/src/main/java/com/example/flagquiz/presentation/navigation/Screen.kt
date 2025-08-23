package com.example.flagquiz.presentation.navigation

sealed class Screen(val route: String) {
    object FlagsChallenge : Screen("flags_challenge")
    object Countdown : Screen("countdown")
    object Quiz : Screen("quiz")
    object GameOver : Screen("game_over")
}
