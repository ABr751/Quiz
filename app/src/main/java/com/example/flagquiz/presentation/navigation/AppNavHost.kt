package com.example.flagquiz.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.flagquiz.presentation.flags.FlagsChallengeScreen
import com.example.flagquiz.presentation.countdown.CountdownScreen
import com.example.flagquiz.presentation.quiz.QuizScreen
import com.example.flagquiz.presentation.quiz.GameOverScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Screen.FlagsChallenge.route
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
                androidx.navigation.navArgument("correctAnswers") { type = androidx.navigation.NavType.IntType },
                androidx.navigation.navArgument("totalQuestions") { type = androidx.navigation.NavType.IntType }
            )
        ) { backStackEntry ->
            val correctAnswers = backStackEntry.arguments?.getInt("correctAnswers") ?: 0
            val totalQuestions = backStackEntry.arguments?.getInt("totalQuestions") ?: 0
            
            GameOverScreen(
                correctAnswers = correctAnswers,
                totalQuestions = totalQuestions,
                onPlayAgain = {
                    navController.navigate(Screen.Quiz.route) {
                        popUpTo(Screen.Quiz.route) { inclusive = true }
                    }
                },
                onBackToMenu = {
                    navController.navigate(Screen.FlagsChallenge.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
