package com.jcv.mocktests

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL

data class Question(
    val id: Int,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    var selectedOption: Int? = null // null means not answered
)

class ExamViewModel : ViewModel() {
    val questions = mutableStateListOf<Question>()
    val currentQuestionIndex = mutableStateOf(0)
    val isLoading = mutableStateOf(true)
    val isExamFinished = mutableStateOf(false)
    val score = mutableStateOf(0)

    // REPLACE WITH YOUR GOOGLE SHEET ID
    private val sheetId = "15OOuXOGxXb5YFcCxaovRE-voZN98Kr__IpYlV7h-3oA" 

    init {
        fetchQuestions()
    }

    private fun fetchQuestions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fetching as CSV is easier to parse natively in Kotlin than the gviz JSON
                val url = "https://docs.google.com/spreadsheets/d/$sheetId/export?format=csv"
                val csvData = URL(url).readText()
                
                val parsedQuestions = parseCsv(csvData)
                
                viewModelScope.launch(Dispatchers.Main) {
                    questions.clear()
                    questions.addAll(parsedQuestions)
                    isLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseCsv(csv: String): List<Question> {
        val lines = csv.lines().drop(1) // Drop header row
        val qList = mutableListOf<Question>()
        
        for ((index, line) in lines.withIndex()) {
            if (line.isBlank()) continue
            val parts = line.split(",") // Basic CSV split
            if (parts.size >= 8) {
                qList.add(
                    Question(
                        id = index + 1,
                        text = parts[2],
                        options = listOf(parts[3], parts[4], parts[5], parts[6]),
                        correctIndex = parts[7].toIntOrNull()?.minus(1) ?: 0
                    )
                )
            }
        }
        return qList
    }

    fun selectOption(optionIndex: Int) {
        val currentQ = questions[currentQuestionIndex.value]
        questions[currentQuestionIndex.value] = currentQ.copy(selectedOption = optionIndex)
    }

    fun nextQuestion() {
        if (currentQuestionIndex.value < questions.size - 1) {
            currentQuestionIndex.value++
        }
    }

    fun previousQuestion() {
        if (currentQuestionIndex.value > 0) {
            currentQuestionIndex.value--
        }
    }

    fun submitExam() {
        var calculatedScore = 0
        for (q in questions) {
            if (q.selectedOption == q.correctIndex) {
                calculatedScore++
            }
        }
        score.value = calculatedScore
        isExamFinished.value = true
    }
}
