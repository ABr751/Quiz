package com.example.flagquiz.presentation.quiz

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flagquiz.data.local.QuizPreferences
import com.example.flagquiz.data.model.Question
import com.example.flagquiz.data.repository.QuizRepository
import com.example.flagquiz.presentation.quiz.QuizViewModel.Companion.INTERVAL_DURATION
import com.example.flagquiz.presentation.quiz.QuizViewModel.Companion.QUESTION_DURATION
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QuizState(
    val currentQuestionIndex: Int = 0,
    val currentQuestion: Question? = null,
    val questions: List<Question> = emptyList(),
    val questionTimer: Int = QUESTION_DURATION,
    val intervalTimer: Int = INTERVAL_DURATION,
    val isShowingInterval: Boolean = false,
    val isQuizComplete: Boolean = false,
    val correctAnswers: Int = 0,
    val totalQuestions: Int = 0,
    val selectedAnswerId: Int? = null,
    val isAnswered: Boolean = false,
    val correctAnswerId: Int? = null,
    val lastUpdateTime: Long = 0L,
    val answerTime: Long = 0L // Track when the answer was given
)

class QuizViewModel : ViewModel() {
    private val repository = QuizRepository()


    private val _state = MutableStateFlow(QuizState())
    val state: StateFlow<QuizState> = _state.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null
    private var quizPreferences: QuizPreferences? = null

    private fun loadSavedState(context: Context): QuizState {
        if (quizPreferences == null) {
            quizPreferences = QuizPreferences(context)
        }
        return quizPreferences?.loadQuizState() ?: QuizState()
    }

    private fun saveState(state: QuizState, context: Context) {
        if (quizPreferences == null) {
            quizPreferences = QuizPreferences(context)
        }
        quizPreferences?.saveQuizState(state)
    }

    fun initializeQuiz(context: Context) {
        viewModelScope.launch {
            val questions = repository.loadQuestions(context)

            if (questions.isNotEmpty()) {
                // Load saved state from SharedPreferences
                val savedState = loadSavedState(context)

                if (savedState.questions.isEmpty()) {
                    // First time loading
                    val initialState = savedState.copy(
                        questions = questions,
                        totalQuestions = questions.size,
                        currentQuestion = questions.firstOrNull()
                    )
                    _state.value = initialState
                    saveState(initialState, context)
                } else {
                    Log.d( "QuizViewModel initiate", "initiating quiz state: ${quizPreferences?.getQuizStartTime()}}")
                    // Quiz was already started, calculate current state based on elapsed time
                    calculateCurrentState(savedState)
                }
            }
        }
    }

    fun startQuiz(context: Context) {
        // Cancel any existing timer job
        timerJob?.cancel()

        // Set quiz start time if not already set
        if (quizPreferences?.getQuizStartTime() == 0L) {
            quizPreferences?.setQuizStartTime(System.currentTimeMillis())
            val startState = _state.value.copy(
                lastUpdateTime = System.currentTimeMillis()
            )
            _state.value = startState
            saveState(startState, context)
        }
        Log.d( "QuizViewModel start", "Starting quiz with start time: ${quizPreferences?.getQuizStartTime()}")

        // Start the question phase timer
        timerJob = viewModelScope.launch {
            startQuestionPhaseTimer(context)
        }
    }

    private fun calculateCurrentState(savedState: QuizState): QuizState {
        if (savedState.isQuizComplete) {
            _state.value = _state.value.copy(isQuizComplete = true)
            return _state.value
        } else {
            val quizStartTime = quizPreferences?.getQuizStartTime()?:0
            Log.d("QuizViewModel calculate", "Calculating state with quizStartTime: $quizStartTime")
            val currentTime = System.currentTimeMillis()
            val elapsedTime = (currentTime - quizStartTime) / 1000
            val currentIndex = elapsedTime / MAX_QUESTION_PERIOD
            Log.d( "QuizViewModel calculate", "Elapsed Time: $elapsedTime seconds, Current Index: $currentIndex")
            val phase = elapsedTime % MAX_QUESTION_PERIOD

            var remainingQuestionDuration = 0
            var remainingIntervalDuration = 0

            if (currentIndex >= savedState.questions.size) {
                _state.value =
                    _state.value.copy(isQuizComplete = true, questionTimer = 0, intervalTimer = 0)
                return _state.value
            } else {
                if (phase < QUESTION_DURATION) {
                    // Question phase
                    remainingQuestionDuration = (QUESTION_DURATION - phase).toInt()
                    if (_state.value.isAnswered){
                        quizPreferences?.setQuizStartTime(quizPreferences!!.getQuizStartTime() - (remainingQuestionDuration * 1000))
                        timerJob?.cancel()
                    }
                } else {
                    remainingIntervalDuration = (MAX_QUESTION_PERIOD - phase).toInt()
                }
            }
            _state.value = _state.value.copy(
                questions = savedState.questions,
                totalQuestions = savedState.questions.size,
                questionTimer = remainingQuestionDuration,
                intervalTimer = remainingIntervalDuration,
                currentQuestionIndex = currentIndex.toInt(),
                currentQuestion = savedState.questions[currentIndex.toInt()]
            )

            return _state.value
        }
    }

    private suspend fun startQuestionTimer(context: Context) {
        while (_state.value.currentQuestionIndex < _state.value.questions.size && !_state.value.isQuizComplete) {
            // Update state based on current time
            val updatedState = calculateCurrentState(_state.value)
            _state.value = updatedState
            Log.d( "QuizViewModel Timer", "Updated State: ${Gson().toJson(updatedState)}")
            saveState(updatedState, context)

            if (updatedState.isQuizComplete) {
                break
            }

            delay(1000)
        }

        // Quiz complete
        val completeState = _state.value.copy(isQuizComplete = true)
        _state.value = completeState
        saveState(completeState, context)
    }

    private suspend fun stopQuestionPhaseTimer(context: Context) {
        if (_state.value.isAnswered) {
            _state.value = _state.value.copy(questionTimer = 0)
            saveState(_state.value, context)
        }
        startIntervalTimer(context)
    }

    private suspend fun startQuestionPhaseTimer(context: Context) {
        var questionTime = QUESTION_DURATION
        while (questionTime > 0 && !_state.value.isAnswered) {
            _state.value = _state.value.copy(questionTimer = questionTime)
            saveState(_state.value, context)
            delay(1000)
            questionTime--
        }

        // Question time is up, start interval
        if (questionTime <= 0) {
            startIntervalTimer(context)
        }
    }

    private suspend fun startIntervalTimer(context: Context) {
        var intervalTime = INTERVAL_DURATION
        _state.value = _state.value.copy(isShowingInterval = true)
        while (intervalTime > 0) {
            _state.value = _state.value.copy(
                intervalTimer = intervalTime
            )
            saveState(_state.value, context)
            delay(1000)
            intervalTime--
        }
        moveToNextQuestion(context)
    }

    private fun moveToNextQuestion(context: Context) {
        val currentIndex = _state.value.currentQuestionIndex
        val nextIndex = currentIndex + 1

        if (nextIndex >= _state.value.questions.size) {
            // Quiz is complete
            _state.value = _state.value.copy(isQuizComplete = true)
            saveState(_state.value, context)
        } else {
            // Move to next question
            val nextQuestion = _state.value.questions[nextIndex]
            _state.value = _state.value.copy(
                currentQuestionIndex = nextIndex,
                currentQuestion = nextQuestion,
                questionTimer = QUESTION_DURATION,
                intervalTimer = INTERVAL_DURATION,
                isShowingInterval = false,
                isAnswered = false,
                selectedAnswerId = null,
                correctAnswerId = null,
                answerTime = 0L
            )
            saveState(_state.value, context)

            // Start timer for next question
            viewModelScope.launch {
                startQuestionPhaseTimer(context)
            }
        }
    }

    fun selectAnswer(answerId: Int, context: Context) {
        if (_state.value.isAnswered) return

        val currentQuestion = _state.value.currentQuestion ?: return
        val isCorrect = answerId == currentQuestion.answerId

        val updatedState = _state.value.copy(
            selectedAnswerId = answerId,
            isAnswered = true,
            correctAnswerId = currentQuestion.answerId,
            correctAnswers = if (isCorrect) _state.value.correctAnswers + 1 else _state.value.correctAnswers,
            answerTime = System.currentTimeMillis() // Record when answer was given
        )
        _state.value = updatedState
        saveState(updatedState, context)

        // Start interval timer when answer is selected
        viewModelScope.launch {
            if (_state.value.questionTimer >= 0) {
                stopQuestionPhaseTimer(context)
            } else {
                startIntervalTimer(context)
            }
        }
    }


    fun refreshState(context: Context) {
        if (_state.value.questions.isNotEmpty() && quizPreferences!!.getQuizStartTime() > 0) {
            Log.d("QuizViewModel refresh", "Refreshing state with start time: ${quizPreferences?.getQuizStartTime()}")

            val updatedState = calculateCurrentState(_state.value)
            _state.value = updatedState.copy(intervalTimer = INTERVAL_DURATION)
            saveState(updatedState, context)

            // Restart timer if quiz is not complete
            if (!updatedState.isQuizComplete) {
                timerJob?.cancel()
                timerJob = viewModelScope.launch {
                    startQuestionTimer(context)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        const val QUESTION_DURATION = 30
        const val INTERVAL_DURATION = 10
        const val MAX_QUESTION_PERIOD = QUESTION_DURATION + INTERVAL_DURATION
    }
}
