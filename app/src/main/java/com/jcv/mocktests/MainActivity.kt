package com.jcv.mocktests

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExamScreen()
                }
            }
        }
    }
}

@Composable
fun ExamScreen(viewModel: ExamViewModel = viewModel()) {
    if (viewModel.isLoading.value) {
        // THIS IS THE UPDATED LOADING SCREEN
        Box(
            contentAlignment = Alignment.Center, 
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            CircularProgressIndicator(
                color = Color(0xFF673AB7), // Deep purple matching your screenshot
                strokeWidth = 4.dp,
                modifier = Modifier.size(60.dp)
            )
        }
    } else if (viewModel.isExamFinished.value) {
        ResultScreen(viewModel.score.value, viewModel.questions.size)
    } else {
        QuestionUI(viewModel)
    }
}

@Composable
fun QuestionUI(viewModel: ExamViewModel) {
    val currentIndex = viewModel.currentQuestionIndex.value
    val question = viewModel.questions[currentIndex]

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        // Header
        Text(
            text = "Question ${currentIndex + 1} of ${viewModel.questions.size}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF104E8B)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // Question Text
        Text(text = question.text, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(24.dp))

        // Options
        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(question.options) { index, optionText ->
                val isSelected = question.selectedOption == index
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { viewModel.selectOption(index) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFE0F2FE) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.selectOption(index) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = optionText)
                    }
                }
            }
        }

        // Footer Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { viewModel.previousQuestion() },
                enabled = currentIndex > 0
            ) {
                Text("Previous")
            }

            if (currentIndex == viewModel.questions.size - 1) {
                Button(
                    onClick = { viewModel.submitExam() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                ) {
                    Text("Submit")
                }
            } else {
                Button(onClick = { viewModel.nextQuestion() }) {
                    Text("Save & Next")
                }
            }
        }
    }
}

@Composable
fun ResultScreen(score: Int, total: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Exam Submitted!", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Your Score: $score / $total", fontSize = 24.sp, color = Color(0xFF1E90FF))
    }
}
