package com.jcv.mocktests

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class Question(
    val id: Int,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    var selectedOption: Int? = null
)

class ExamViewModel : ViewModel() {
    val questions = mutableStateListOf<Question>()
    val currentQuestionIndex = mutableStateOf(0)
    val isLoading = mutableStateOf(true)
    val isExamFinished = mutableStateOf(false)
    val score = mutableStateOf(0)
    
    // New state to show errors on screen instead of infinite loading
    val errorMessage = mutableStateOf<String?>(null) 

    private val sheetId = "15OOuXOGxXb5YFcCxaovRE-voZN98Kr__IpYlV7h-3oA"
    private val sheetName = "Sheet3" // Explicitly requesting Sheet3

    init {
        fetchQuestions()
    }

    private fun fetchQuestions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Using the exact same Google Sheets JSON API as your original HTML code
                val urlString = "https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:json&headers=0&sheet=$sheetName"
                
                val connection = URL(urlString).openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000 // 15 seconds timeout
                connection.readTimeout = 15000

                // Read the response
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                
                // Clean the Google visualization wrapper to get pure JSON
                val jsonString = response.substringAfter("setResponse(").substringBeforeLast(");")
                
                val jsonObject = JSONObject(jsonString)
                val rows = jsonObject.getJSONObject("table").getJSONArray("rows")
                
                val qList = mutableListOf<Question>()
                
                // Loop through rows (starting at 1 to skip the header)
                for (i in 1 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val cArray = row.getJSONArray("c")
                    
                    // Safe extractor for cell values
                    fun getCellString(index: Int): String {
                        if (cArray.isNull(index)) return ""
                        val cell = cArray.getJSONObject(index)
                        return if (cell.has("v")) cell.getString("v") else ""
                    }

                    val qText = getCellString(2)
                    if (qText.isBlank()) continue

                    val opt1 = getCellString(3)
                    val opt2 = getCellString(4)
                    val opt3 = getCellString(5)
                    val opt4 = getCellString(6)
                    
                    // Parse correct answer (subtract 1 to match 0-based index)
                    val correctRaw = getCellString(7).toDoubleOrNull()?.toInt() ?: 1
                    val correctIndex = (correctRaw - 1).coerceAtLeast(0)

                    qList.add(
                        Question(
                            id = i,
                            text = qText,
                            options = listOf(opt1, opt2, opt3, opt4).filter { it.isNotBlank() },
                            correctIndex = correctIndex
                        )
                    )
                }

                // Switch back to the Main UI thread to update the screen
                viewModelScope.launch(Dispatchers.Main) {
                    if (qList.isEmpty()) {
                        errorMessage.value = "No questions found in Sheet3."
                    } else {
                        questions.clear()
                        questions.addAll(qList)
                    }
                    isLoading.value = false
                }

            } catch (e: Exception) {
                Log.e("ExamViewModel", "Error fetching data", e)
                viewModelScope.launch(Dispatchers.Main) {
                    errorMessage.value = "Failed to load data. Please check your internet connection."
                    isLoading.value = false // Stop the spinner even if it fails
                }
            }
        }
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
