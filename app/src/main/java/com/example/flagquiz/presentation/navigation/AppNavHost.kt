package com.example.flagquiz.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.flagquiz.data.local.QuizPreferences
import com.example.flagquiz.presentation.flags.FlagsChallengeScreen
import com.example.flagquiz.presentation.countdown.CountdownScreen
import com.example.flagquiz.presentation.quiz.QuizScreen
import com.example.flagquiz.presentation.quiz.GameOverScreen
import com.example.flagquiz.presentation.quiz.ResultScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Check if there's an active quiz
    val quizPreferences = remember { QuizPreferences(context) }
    val isQuizActive = remember { quizPreferences.isQuizActive() }

    // Determine start destination based on quiz state
    val startDestination = if (isQuizActive) {
        Screen.Quiz.route
    } else {
        Screen.FlagsChallenge.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.FlagsChallenge.route) {
            FlagsChallengeScreen(
                onNavigateToChallenge = {
                    navController.navigate(Screen.Countdown.route)
                }
            )
        }

        composable(Screen.Countdown.route) {
            CountdownScreen(
                onCountdownComplete = {
                    navController.navigate(Screen.Quiz.route)
                }
            )
        }

        composable(Screen.Quiz.route) {
            QuizScreen(
                onQuizComplete = { correctAnswers, totalQuestions ->
                    navController.navigate("${Screen.GameOver.route}/$correctAnswers/$totalQuestions")
                }
            )
        }

        composable(
            route = "${Screen.GameOver.route}/{correctAnswers}/{totalQuestions}",
            arguments = listOf(
                androidx.navigation.navArgument("correctAnswers") {
                    type = androidx.navigation.NavType.IntType
                },
                androidx.navigation.navArgument("totalQuestions") {
                    type = androidx.navigation.NavType.IntType
                }
            )
        ) { backStackEntry ->
            val correctAnswers = backStackEntry.arguments?.getInt("correctAnswers") ?: 0
            val totalQuestions = backStackEntry.arguments?.getInt("totalQuestions") ?: 0

            GameOverScreen(
                correctAnswers = correctAnswers,
                totalQuestions = totalQuestions,
                navigateToResult = { correct, total ->
                    navController.navigate("${Screen.Result.route}/$correct/$total") {
                        popUpTo(
                            Screen.FlagsChallenge.route
                        ){
                            inclusive = false
                        }
                    }
                })
        }
        composable(
            route = "${Screen.Result.route}/{correctAnswers}/{totalQuestions}",
            arguments = listOf(
                androidx.navigation.navArgument("correctAnswers") {
                    type = androidx.navigation.NavType.IntType
                },
                androidx.navigation.navArgument("totalQuestions") {
                    type = androidx.navigation.NavType.IntType
                }
            )) { backStackEntry ->
            val correctAnswers = backStackEntry.arguments?.getInt("correctAnswers") ?: 0
            val totalQuestions = backStackEntry.arguments?.getInt("totalQuestions") ?: 0
            ResultScreen(correctAnswers, totalQuestions)
        }
    }
}
