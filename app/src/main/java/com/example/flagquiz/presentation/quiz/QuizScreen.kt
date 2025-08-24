package com.example.flagquiz.presentation.quiz

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flagquiz.data.model.Country
import kotlinx.coroutines.delay

import com.example.flagquiz.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    onQuizComplete: (correctAnswers: Int, totalQuestions: Int) -> Unit = { _, _ -> },
    viewModel: QuizViewModel = viewModel(factory = QuizViewModelFactory())
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val quizPreferences = remember { com.example.flagquiz.data.local.QuizPreferences(context) }

    // Initialize quiz with context
    LaunchedEffect(Unit) {
        viewModel.initializeQuiz(context)
        viewModel.startQuiz(context)
    }

    // Observe lifecycle events to refresh state when app comes back to foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.refreshState(context)
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handle quiz completion
    LaunchedEffect(state.isQuizComplete) {
        if (state.isQuizComplete) {
            delay(2000) // Show final results for 2 seconds
            // Clear quiz state when quiz is completed
            quizPreferences.clearQuizState()
            onQuizComplete(state.correctAnswers, state.totalQuestions)
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TopAppBarDefaults.LargeAppBarCollapsedHeight)
                .background(Color(0xFFFF6B35))
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .offset(y = 100.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header with timer and title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Timer in black box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = String.format("00:%02d", state.questionTimer),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    Text(
                        text = "FLAGS CHALLENGE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B35)
                    )

                    // Empty space for balance
                    Spacer(modifier = Modifier.width(80.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))


                state.currentQuestion?.let { question ->
                    // Question number and text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Question number in orange circle
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = Color(0xFFFF6B35),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${state.currentQuestionIndex + 1}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                        // Show interval screen or question content
                        Text(
                            text = "GUESS THE COUNTRY FROM THE FLAG ?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                    }

                    if (state.isShowingInterval) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "NEXT QUESTION IN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = String.format("00:%02d", state.intervalTimer),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF6B35),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Flag and answers row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        val flagResourceId =
                            when (question.countryCode.toLowerCase(Locale.current)) {
                                "jp" -> R.drawable.ic_jp
                                "ae" -> R.drawable.ic_ae
                                "cz" -> R.drawable.ic_cz
                                "ec" -> R.drawable.ic_ec
                                "ga" -> R.drawable.ic_ga
                                "mq" -> R.drawable.ic_mq
                                "pm" -> R.drawable.ic_pm
                                "py" -> R.drawable.ic_py
                                "tm" -> R.drawable.ic_tm
                                "nz" -> R.drawable.ic_nz
                                "aw" -> R.drawable.ic_aw
                                "kg" -> R.drawable.ic_kg
                                "bz" -> R.drawable.ic_bz
                                "je" -> R.drawable.ic_je
                                "ls" -> R.drawable.ic_ls
                                else -> 0 // Default to 0 if no resource found
                            }
                        if (flagResourceId != 0) {
                            Image(
                                painter = painterResource(id = flagResourceId),
                                contentDescription = "Flag of ${question.countryCode}",
                                modifier = Modifier
                                    .weight(0.3f)
                                    .padding(8.dp)
                            )
                        }


                        // Answer options on the right in vertical grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.weight(0.7f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(question.countries.size) { index ->
                                AnswerOption(
                                    option = question.countries[index],
                                    state = state,
                                    onOptionSelected = {
                                        viewModel.selectAnswer(it, context)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Quiz complete screen
            if (state.isQuizComplete) {
                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Quiz Complete!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Score: ${state.correctAnswers}/${state.totalQuestions}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }

    }
}

@Composable
fun AnswerOption(
    option: Country,
    state: QuizState,
    onOptionSelected: (Int) -> Unit = {},
) {
    val isSelected = state.selectedAnswerId == option.id
    val isCorrect = state.correctAnswerId == option.id
    val isWrong = isSelected && !isCorrect && state.isAnswered
    val showCorrectAnswer = state.isAnswered && isCorrect

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.animateContentSize()
    ) {
        OutlinedCard(
            modifier = Modifier.padding(4.dp)
                .fillMaxWidth()
                .clickable(
                    enabled = !state.isAnswered
                ) {
                    onOptionSelected(option.id)
                },
            border = BorderStroke(width = 1.dp, color = when {
                showCorrectAnswer -> Color.Green
                isWrong -> Color.Red
                isSelected -> Color(0xFFFF6B35)
                else -> Color.Black
            }),
            colors = CardDefaults.outlinedCardColors(
                contentColor = when {
                    showCorrectAnswer -> Color.Green
                    isWrong -> Color.Red
                    isSelected -> Color(0xFFFF6B35)
                    else -> Color.Black
                }
            ),
        ) {

            Text(
                text = option.countryName,
                fontSize = 14.sp,
                maxLines = 1,
                color = when {
                    showCorrectAnswer -> Color.Green
                    isWrong -> Color.Red
                    else -> Color.Black
                },
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(8.dp)
                    .basicMarquee()
            )

        }

        if (state.isAnswered) {
            Text(
                text = when {
                    showCorrectAnswer -> "CORRECT"
                    isWrong -> "WRONG"
                    else -> ""
                },
                fontSize = 10.sp,
                modifier = Modifier.padding(4.dp),
                fontWeight = FontWeight.Bold,
                color = if (showCorrectAnswer) Color.Green else Color.Red
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QuizScreenPreview() {
    QuizScreen()
}
