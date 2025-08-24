package com.example.flagquiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.time.LocalDate

data class ScheduledTime(
    val hours: String = "0",
    val minutes: String = "0",
    val seconds: String = "0"
)

data class FlagsChallengeState(
    val selectedTab: Int = 1, // 0 for Challenge, 1 for Schedule
    val scheduledTime: ScheduledTime = ScheduledTime(),
    val currentTime: String = "00:00",
    val isScheduled: Boolean = false,
    val scheduledTimeString: String = "",
    val countdownSeconds: Int = 20,
    val isCountdownActive: Boolean = false,
    val scheduledDateTime: LocalDateTime? = null,
    val timeUntilChallenge: Long = 0L, // seconds until challenge starts
    val showPreCountdown: Boolean = false,
    val preCountdownSeconds: Int = 20
)

class FlagsChallengeViewModel : ViewModel() {
    
    private val _state = MutableStateFlow(FlagsChallengeState())
    val state: StateFlow<FlagsChallengeState> = _state.asStateFlow()
    
    fun updateHours(hours: String) {
        if (hours.length <= 2 && hours.all { it.isDigit() }) {
            val newScheduledTime = _state.value.scheduledTime.copy(hours = hours)
            _state.value = _state.value.copy(scheduledTime = newScheduledTime)
        }
    }
    
    fun updateMinutes(minutes: String) {
        if (minutes.length <= 2 && minutes.all { it.isDigit() }) {
            val newScheduledTime = _state.value.scheduledTime.copy(minutes = minutes)
            _state.value = _state.value.copy(scheduledTime = newScheduledTime)
        }
    }
    
    fun updateSeconds(seconds: String) {
        if (seconds.length <= 2 && seconds.all { it.isDigit() }) {
            val newScheduledTime = _state.value.scheduledTime.copy(seconds = seconds)
            _state.value = _state.value.copy(scheduledTime = newScheduledTime)
        }
    }
    
    fun updateCurrentTime() {
        val time = LocalTime.now()
        val currentTimeString = time.format(DateTimeFormatter.ofPattern("mm:ss"))
        _state.value = _state.value.copy(currentTime = currentTimeString)
        
        // Check if we need to start pre-countdown
        checkScheduledTime()
    }
    
    fun saveScheduledTime() {
        val scheduledTime = _state.value.scheduledTime
        val timeString = "${scheduledTime.hours.padStart(2, '0')}:${scheduledTime.minutes.padStart(2, '0')}:${scheduledTime.seconds.padStart(2, '0')}"
        
        // Calculate the scheduled date time for today
        val now = LocalDateTime.now()
        val scheduledTimeOfDay = LocalTime.of(
            scheduledTime.hours.toIntOrNull() ?: 0,
            scheduledTime.minutes.toIntOrNull() ?: 0,
            scheduledTime.seconds.toIntOrNull() ?: 0
        )
        
        var scheduledDateTime = LocalDateTime.of(now.toLocalDate(), scheduledTimeOfDay)
        
        // If the scheduled time has already passed today, schedule for tomorrow
        if (scheduledDateTime.isBefore(now)) {
            scheduledDateTime = scheduledDateTime.plusDays(1)
        }
        
        _state.value = _state.value.copy(
            isScheduled = true,
            scheduledTimeString = timeString,
            scheduledDateTime = scheduledDateTime
        )
        
        // Start monitoring the scheduled time
        startScheduledTimeMonitoring()
    }
    
    private fun startScheduledTimeMonitoring() {
        viewModelScope.launch {
            while (true) {
                checkScheduledTime()
                delay(1000) // Check every second
            }
        }
    }
    
    private fun checkScheduledTime() {
        val scheduledDateTime = _state.value.scheduledDateTime ?: return
        val now = LocalDateTime.now()
        val timeUntilChallenge = java.time.Duration.between(now, scheduledDateTime).seconds
        
        if (timeUntilChallenge <= 0) {
            // Challenge time has arrived
            _state.value = _state.value.copy(
                showPreCountdown = false,
                isCountdownActive = true
            )
        } else if (timeUntilChallenge <= 20) {
            // Show pre-countdown (20 seconds before)
            _state.value = _state.value.copy(
                showPreCountdown = true,
                preCountdownSeconds = timeUntilChallenge.toInt()
            )
        } else {
            // Normal waiting state
            _state.value = _state.value.copy(
                showPreCountdown = false,
                timeUntilChallenge = timeUntilChallenge
            )
        }
    }
    
    fun startCountdown() {
        _state.value = _state.value.copy(isCountdownActive = true)
    }
    
    fun updateCountdown(seconds: Int) {
        _state.value = _state.value.copy(countdownSeconds = seconds)
    }
    
    fun validateTimeInput(): Boolean {
        val scheduledTime = _state.value.scheduledTime
        val hours = scheduledTime.hours.toIntOrNull() ?: 0
        val minutes = scheduledTime.minutes.toIntOrNull() ?: 0
        val seconds = scheduledTime.seconds.toIntOrNull() ?: 0
        
        return hours in 0..23 && minutes in 0..59 && seconds in 0..59
    }
}
