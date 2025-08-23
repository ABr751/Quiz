package com.example.flagquiz.presentation.quiz

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flagquiz.data.model.Country
import com.example.flagquiz.data.model.Question
import com.example.flagquiz.data.repository.QuizRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

data class QuizState(
    val currentQuestionIndex: Int = 0,
    val currentQuestion: Question? = null,
    val questions: List<Question> = emptyList(),
    val currentTimer: Int = 30,
    val intervalTimer: Int = 10,
    val isShowingInterval: Boolean = false,
    val isQuizComplete: Boolean = false,
    val correctAnswers: Int = 0,
    val totalQuestions: Int = 0,
    val selectedAnswerId: Int? = null,
    val isAnswered: Boolean = false,
    val correctAnswerId: Int? = null
)

class QuizViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val repository = QuizRepository()
    private val gson = Gson()
    
    private val _state = MutableStateFlow(loadSavedState())
    val state: StateFlow<QuizState> = _state.asStateFlow()
    
    private var timerJob: kotlinx.coroutines.Job? = null
    
    private fun loadSavedState(): QuizState {
        val savedStateJson = savedStateHandle.get<String>("quiz_state")
        return if (savedStateJson != null) {
            try {
                gson.fromJson(savedStateJson, QuizState::class.java)
            } catch (e: Exception) {
                QuizState()
            }
        } else {
            QuizState()
        }
    }
    
    private fun saveState(state: QuizState) {
        savedStateHandle["quiz_state"] = gson.toJson(state)
    }
    
    fun initializeQuiz(context: Context) {
        viewModelScope.launch {
            val questions = repository.loadQuestions(context)
            val currentState = _state.value
            
            if (questions.isNotEmpty() && currentState.questions.isEmpty()) {
                // First time loading
                val initialState = currentState.copy(
                    questions = questions,
                    totalQuestions = questions.size,
                    currentQuestion = questions.firstOrNull()
                )
                _state.value = initialState
                saveState(initialState)
            }
        }
    }
    
    fun startQuiz() {
        // Cancel any existing timer job
        timerJob?.cancel()
        
        timerJob = viewModelScope.launch {
            startQuestionTimer()
        }
    }
    
    private suspend fun startQuestionTimer() {
        while (_state.value.currentQuestionIndex < _state.value.questions.size) {
            val currentQuestion = _state.value.questions[_state.value.currentQuestionIndex]
            _state.value = _state.value.copy(
                currentQuestion = currentQuestion,
                currentTimer = 30,
                isShowingInterval = false,
                selectedAnswerId = null,
                isAnswered = false,
                correctAnswerId = null
            )
            saveState(_state.value)
            
            // Question timer
            while (_state.value.currentTimer > 0 && !_state.value.isAnswered) {
                delay(1000)
                _state.value = _state.value.copy(currentTimer = _state.value.currentTimer - 1)
                saveState(_state.value)
            }
            
            // If not answered, mark as incorrect
            if (!_state.value.isAnswered) {
                _state.value = _state.value.copy(
                    correctAnswerId = currentQuestion.answerId
                )
                saveState(_state.value)
            }
            
            // Interval between questions
            if (_state.value.currentQuestionIndex < _state.value.questions.size - 1) {
                _state.value = _state.value.copy(
                    isShowingInterval = true,
                    intervalTimer = 10
                )
                saveState(_state.value)
                
                while (_state.value.intervalTimer > 0) {
                    delay(1000)
                    _state.value = _state.value.copy(intervalTimer = _state.value.intervalTimer - 1)
                    saveState(_state.value)
                }
            }
            
            _state.value = _state.value.copy(
                currentQuestionIndex = _state.value.currentQuestionIndex + 1
            )
            saveState(_state.value)
        }
        
        // Quiz complete
        _state.value = _state.value.copy(isQuizComplete = true)
        saveState(_state.value)
    }
    
    fun selectAnswer(answerId: Int) {
        if (_state.value.isAnswered) return
        
        val currentQuestion = _state.value.currentQuestion ?: return
        val isCorrect = answerId == currentQuestion.answerId
        
        val updatedState = _state.value.copy(
            selectedAnswerId = answerId,
            isAnswered = true,
            correctAnswerId = currentQuestion.answerId,
            correctAnswers = if (isCorrect) _state.value.correctAnswers + 1 else _state.value.correctAnswers
        )
        _state.value = updatedState
        saveState(updatedState)
    }
    
    fun resetQuiz() {
        timerJob?.cancel()
        val resetState = QuizState()
        _state.value = resetState
        saveState(resetState)
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
