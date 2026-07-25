package com.jcv.mocktests

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

enum class QuestionStatus {
    NOT_VISITED, NOT_ANSWERED, ANSWERED, MARKED_FOR_REVIEW, ANSWERED_AND_MARKED
}

data class Question(
    val globalId: Int,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    var status: QuestionStatus = QuestionStatus.NOT_VISITED,
    var selectedOption: Int? = null
)

data class TestSection(val name: String, val questions: List<Question>)
data class ExamTest(val title: String, val sections: List<TestSection>, val totalQuestions: Int)

class ExamViewModel : ViewModel() {
    val availableTests = mutableStateListOf<ExamTest>()
    val selectedTest = mutableStateOf<ExamTest?>(null)
    
    val appState = mutableStateOf("LOADING") // LOADING, MENU, INSTRUCTIONS, EXAM, RESULTS
    val errorMessage = mutableStateOf<String?>(null)

    val currentSectionIndex = mutableStateOf(0)
    val currentQuestionIndex = mutableStateOf(0)
    val timeLeft = mutableStateOf(0)
    
    val score = mutableStateOf(0)

    private val sheetId = "15OOuXOGxXb5YFcCxaovRE-voZN98Kr__IpYlV7h-3oA"
    private val sheetName = "Sheet3"
    private var timerJob: Job? = null

    init {
        fetchExamData()
    }

    private fun fetchExamData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val urlString = "https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:json&headers=0&sheet=$sheetName"
                val connection = URL(urlString).openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonString = response.substringAfter("setResponse(").substringBeforeLast(");")
                val jsonObject = JSONObject(jsonString)
                val rows = jsonObject.getJSONObject("table").getJSONArray("rows")

                // Group by Test Name -> Section Name -> Questions
                val parsedData = mutableMapOf<String, MutableMap<String, MutableList<Question>>>()
                var globalIdCounter = 1

                for (i in 1 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val cArray = row.getJSONArray("c")
                    
                    fun getCell(index: Int): String {
                        if (cArray.isNull(index)) return ""
                        val cell = cArray.getJSONObject(index)
                        return if (cell.has("v")) cell.getString("v") else ""
                    }

                    val testName = getCell(0).ifBlank { "General Test" }
                    val secName = getCell(1).ifBlank { "General" }
                    val qText = getCell(2)
                    if (qText.isBlank()) continue

                    val correctRaw = getCell(7).toDoubleOrNull()?.toInt() ?: 1
                    
                    val q = Question(
                        globalId = globalIdCounter++,
                        text = qText.replace("\n", "\n\n"),
                        options = listOf(getCell(3), getCell(4), getCell(5), getCell(6)).filter { it.isNotBlank() },
                        correctIndex = (correctRaw - 1).coerceAtLeast(0)
                    )

                    parsedData.getOrPut(testName) { mutableMapOf() }
                        .getOrPut(secName) { mutableListOf() }
                        .add(q)
                }

                val finalTests = parsedData.map { (tName, sMap) ->
                    var tQuestions = 0
                    val sections = sMap.map { (sName, qList) ->
                        tQuestions += qList.size
                        TestSection(sName, qList)
                    }
                    ExamTest(tName, sections, tQuestions)
                }

                viewModelScope.launch(Dispatchers.Main) {
                    if (finalTests.isEmpty()) {
                        errorMessage.value = "No tests found in sheet."
                    } else {
                        availableTests.addAll(finalTests)
                        appState.value = "MENU"
                    }
                }
            } catch (e: Exception) {
                Log.e("ExamViewModel", "Fetch Error", e)
                viewModelScope.launch(Dispatchers.Main) {
                    errorMessage.value = "Failed to load data. Please check connection."
                    appState.value = "MENU"
                }
            }
        }
    }

    fun selectTest(test: ExamTest) {
        selectedTest.value = test
        currentSectionIndex.value = 0
        currentQuestionIndex.value = 0
        
        // Reset all statuses for a fresh attempt
        test.sections.forEach { sec ->
            sec.questions.forEach { q ->
                q.status = QuestionStatus.NOT_VISITED
                q.selectedOption = null
            }
        }
        
        appState.value = "INSTRUCTIONS"
    }

    fun startExam() {
        val test = selectedTest.value ?: return
        timeLeft.value = test.totalQuestions * 60 // 1 minute per question
        appState.value = "EXAM"
        
        // Mark first question as not answered upon entering
        updateCurrentQuestionStatus(QuestionStatus.NOT_ANSWERED, onlyIfNotVisited = true)
        
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (timeLeft.value > 0) {
                delay(1000)
                timeLeft.value--
            }
            submitExam()
        }
    }

    private fun getCurrentQuestion(): Question? {
        val test = selectedTest.value ?: return null
        return test.sections.getOrNull(currentSectionIndex.value)?.questions?.getOrNull(currentQuestionIndex.value)
    }

    private fun updateCurrentQuestionStatus(newStatus: QuestionStatus, onlyIfNotVisited: Boolean = false) {
        val q = getCurrentQuestion() ?: return
        if (onlyIfNotVisited && q.status != QuestionStatus.NOT_VISITED) return
        
        // Adjust status based on if an option is actually selected
        val finalStatus = when (newStatus) {
            QuestionStatus.MARKED_FOR_REVIEW -> if (q.selectedOption != null) QuestionStatus.ANSWERED_AND_MARKED else QuestionStatus.MARKED_FOR_REVIEW
            QuestionStatus.ANSWERED -> if (q.selectedOption != null) QuestionStatus.ANSWERED else QuestionStatus.NOT_ANSWERED
            else -> newStatus
        }
        q.status = finalStatus
    }

    fun selectOption(index: Int) {
        val q = getCurrentQuestion() ?: return
        q.selectedOption = index
    }

    fun clearResponse() {
        val q = getCurrentQuestion() ?: return
        q.selectedOption = null
        q.status = QuestionStatus.NOT_ANSWERED
    }

    fun saveAndNext() {
        updateCurrentQuestionStatus(QuestionStatus.ANSWERED)
        moveToNext()
    }

    fun markAndNext() {
        updateCurrentQuestionStatus(QuestionStatus.MARKED_FOR_REVIEW)
        moveToNext()
    }

    fun moveToPrevious() {
        if (currentQuestionIndex.value > 0) {
            currentQuestionIndex.value--
        } else if (currentSectionIndex.value > 0) {
            currentSectionIndex.value--
            currentQuestionIndex.value = (selectedTest.value?.sections?.get(currentSectionIndex.value)?.questions?.size ?: 1) - 1
        }
        updateCurrentQuestionStatus(QuestionStatus.NOT_ANSWERED, onlyIfNotVisited = true)
    }

    private fun moveToNext() {
        val test = selectedTest.value ?: return
        val currentSec = test.sections[currentSectionIndex.value]
        
        if (currentQuestionIndex.value < currentSec.questions.size - 1) {
            currentQuestionIndex.value++
        } else if (currentSectionIndex.value < test.sections.size - 1) {
            currentSectionIndex.value++
            currentQuestionIndex.value = 0
        }
        updateCurrentQuestionStatus(QuestionStatus.NOT_ANSWERED, onlyIfNotVisited = true)
    }

    fun jumpToQuestion(sectionIdx: Int, questionIdx: Int) {
        currentSectionIndex.value = sectionIdx
        currentQuestionIndex.value = questionIdx
        updateCurrentQuestionStatus(QuestionStatus.NOT_ANSWERED, onlyIfNotVisited = true)
    }

    fun submitExam() {
        timerJob?.cancel()
        var calculatedScore = 0
        selectedTest.value?.sections?.forEach { sec ->
            sec.questions.forEach { q ->
                if (q.selectedOption == q.correctIndex) {
                    calculatedScore++
                }
            }
        }
        score.value = calculatedScore
        appState.value = "RESULTS"
    }

    fun getStats(): Map<QuestionStatus, Int> {
        val stats = mutableMapOf(
            QuestionStatus.NOT_VISITED to 0,
            QuestionStatus.NOT_ANSWERED to 0,
            QuestionStatus.ANSWERED to 0,
            QuestionStatus.MARKED_FOR_REVIEW to 0,
            QuestionStatus.ANSWERED_AND_MARKED to 0
        )
        selectedTest.value?.sections?.forEach { sec ->
            sec.questions.forEach { q ->
                stats[q.status] = (stats[q.status] ?: 0) + 1
            }
        }
        return stats
    }
}
