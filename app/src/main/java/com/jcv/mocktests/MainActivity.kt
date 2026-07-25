package com.jcv.mocktests

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: ExamViewModel = viewModel()) {
    when (viewModel.appState.value) {
        "LOADING" -> LoadingScreen(viewModel.errorMessage.value)
        "MENU" -> MenuScreen(viewModel)
        "INSTRUCTIONS" -> InstructionsScreen(viewModel)
        "EXAM", "REVIEW" -> CBTScreen(viewModel)
        "RESULTS" -> ResultsScreen(viewModel)
    }
}

@Composable
fun LoadingScreen(error: String?) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        if (error != null) {
            Text(text = error, color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(24.dp))
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFF1E90FF), modifier = Modifier.size(60.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading Exam Data...", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(viewModel: ExamViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("JCV MOCK TESTS", color = Color.White, fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF104E8B))
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF8FAFC)).padding(16.dp)) {
            items(viewModel.availableTests) { test ->
                val score = viewModel.testScores[test.title]
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable { viewModel.selectTest(test) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(test.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${test.sections.size} Sections • ${test.totalQuestions} Questions", color = Color.Gray, fontSize = 14.sp)
                        
                        // Show Score Badge
                        if (score != null) {
                            Text(
                                text = "Score: $score / ${test.totalQuestions}", 
                                color = Color(0xFF16A34A), 
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 12.dp).background(Color(0xFFDCFCE7), RoundedCornerShape(4.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        } else {
                            Text(
                                text = "Not Attempted", 
                                color = Color.DarkGray, 
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 12.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionsScreen(viewModel: ExamViewModel) {
    val test = viewModel.selectedTest.value ?: return
    var agreed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Instructions", color = Color.White) },
                navigationIcon = {
                    TextButton(onClick = { viewModel.appState.value = "MENU" }) {
                        Text("Back", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF104E8B))
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                Text(test.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Total Questions: ${test.totalQuestions} | Time: ${test.totalQuestions} Mins", modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F5F9)).padding(8.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                
                StatusLegendItem(QuestionStatus.NOT_VISITED, "Not Visited")
                StatusLegendItem(QuestionStatus.NOT_ANSWERED, "Not Answered")
                StatusLegendItem(QuestionStatus.ANSWERED, "Answered")
                StatusLegendItem(QuestionStatus.MARKED_FOR_REVIEW, "Marked for Review")
            }
            
            Column(modifier = Modifier.background(Color(0xFFF8FAFC)).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { agreed = !agreed }) {
                    Checkbox(checked = agreed, onCheckedChange = { agreed = it })
                    Text("I have read and understood the instructions.", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.startExam() },
                    enabled = agreed,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E90FF))
                ) {
                    Text("Start Exam", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CBTScreen(viewModel: ExamViewModel) {
    val test = viewModel.selectedTest.value ?: return
    val isReview = viewModel.appState.value == "REVIEW"
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(320.dp).background(Color.White)) {
                PaletteDrawer(viewModel) { scope.launch { drawerState.close() } }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isReview) {
                                Text("REVIEW MODE", fontSize = 12.sp, modifier = Modifier.background(Color(0x33FFFFFF), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(test.title, color = Color.White, fontSize = 16.sp, maxLines = 1)
                        }
                    },
                    actions = {
                        if (isReview) {
                            TextButton(onClick = { viewModel.appState.value = "MENU" }) {
                                Text("Exit Review", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            val minutes = viewModel.timeLeft.value / 60
                            val seconds = viewModel.timeLeft.value % 60
                            Text(String.format("%02d:%02d", minutes, seconds), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 16.dp))
                        }
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Palette", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF104E8B))
                )
            },
            bottomBar = { ExamBottomBar(viewModel) }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF8FAFC))) {
                ScrollableTabRow(
                    selectedTabIndex = viewModel.currentSectionIndex.value,
                    containerColor = Color(0xFFE2E8F0),
                    edgePadding = 0.dp
                ) {
                    test.sections.forEachIndexed { index, section ->
                        Tab(
                            selected = viewModel.currentSectionIndex.value == index,
                            onClick = { 
                                viewModel.currentSectionIndex.value = index
                                viewModel.currentQuestionIndex.value = 0
                            },
                            text = { Text(section.name, fontWeight = FontWeight.Bold, color = if (viewModel.currentSectionIndex.value == index) Color(0xFF1E90FF) else Color.Gray) }
                        )
                    }
                }
                
                val currentQ = test.sections[viewModel.currentSectionIndex.value].questions[viewModel.currentQuestionIndex.value]
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    item {
                        Text("Q ${currentQ.globalId}.", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(currentQ.text, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    
                    itemsIndexed(currentQ.options) { index, option ->
                        val isSelected = currentQ.selectedOption == index
                        val isCorrect = currentQ.correctIndex == index
                        
                        // STYLING LOGIC FOR REVIEW VS EXAM
                        val cardBg = if (isReview) {
                            if (isCorrect) Color(0xFFDCFCE7) // Green for correct
                            else if (isSelected) Color(0xFFFEE2E2) // Red for wrong selection
                            else Color.White
                        } else {
                            if (isSelected) Color(0xFFE0F2FE) else Color.White
                        }
                        
                        val borderCol = if (isReview) {
                            if (isCorrect) Color(0xFF22C55E)
                            else if (isSelected) Color(0xFFEF4444)
                            else Color(0xFFE2E8F0)
                        } else {
                            if (isSelected) Color(0xFF60A5FA) else Color(0xFFE2E8F0)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { viewModel.selectOption(index) },
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, borderCol)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                                Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(if (isReview && isCorrect) Color(0xFF22C55E) else if (isReview && isSelected) Color(0xFFEF4444) else if (isSelected) Color(0xFF1E90FF) else Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                                    Text(('A' + index).toString(), color = if (isSelected || (isReview && isCorrect)) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(option, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExamBottomBar(viewModel: ExamViewModel) {
    val isReview = viewModel.appState.value == "REVIEW"
    
    Column(modifier = Modifier.fillMaxWidth().background(Color.White).border(1.dp, Color(0xFFE2E8F0)).padding(8.dp)) {
        if (isReview) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { viewModel.moveToPrevious() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black), border = BorderStroke(1.dp, Color.Gray), modifier = Modifier.weight(1f)) {
                    Text("Previous", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { viewModel.saveAndNext() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E90FF)), modifier = Modifier.weight(1f)) {
                    Text("Next", fontSize = 14.sp)
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { viewModel.moveToPrevious() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black), border = BorderStroke(1.dp, Color.Gray), modifier = Modifier.weight(1f)) {
                    Text("Prev", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(onClick = { viewModel.markAndNext() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF7E22CE)), border = BorderStroke(1.dp, Color(0xFF9333EA)), modifier = Modifier.weight(1f)) {
                    Text("Mark", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(onClick = { viewModel.clearResponse() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black), border = BorderStroke(1.dp, Color.Gray), modifier = Modifier.weight(1f)) {
                    Text("Clear", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.saveAndNext() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E90FF)), modifier = Modifier.fillMaxWidth()) {
                Text("Save & Next", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PaletteDrawer(viewModel: ExamViewModel, closeDrawer: () -> Unit) {
    val isReview = viewModel.appState.value == "REVIEW"
    val test = viewModel.selectedTest.value ?: return
    val currentSec = test.sections[viewModel.currentSectionIndex.value]

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        Column(modifier = Modifier.background(Color.White).padding(16.dp).fillMaxWidth()) {
            Text("Palette Overview", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))
            
            if (isReview) {
                val reviewStats = viewModel.getReviewStats()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Text("Correct: ${reviewStats.first}", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                    Text("Incorrect: ${reviewStats.second}", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            } else {
                val stats = viewModel.getStats()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatusLegendItem(QuestionStatus.ANSWERED, "Ans (${stats[QuestionStatus.ANSWERED]})")
                    StatusLegendItem(QuestionStatus.NOT_ANSWERED, "Not Ans (${stats[QuestionStatus.NOT_ANSWERED]})")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatusLegendItem(QuestionStatus.NOT_VISITED, "Not Visited (${stats[QuestionStatus.NOT_VISITED]})")
                    StatusLegendItem(QuestionStatus.MARKED_FOR_REVIEW, "Marked (${stats[QuestionStatus.MARKED_FOR_REVIEW]})")
                }
            }
        }
        
        Divider()
        Text(currentSec.name, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
        
        LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            items(currentSec.questions.size) { qIndex ->
                val q = currentSec.questions[qIndex]
                
                // Review mode shapes vs Exam mode shapes
                val shape = if (isReview) RoundedCornerShape(4.dp) else getShapeForStatus(q.status)
                val colorTuple = if (isReview) {
                    if (q.selectedOption != null) {
                        if (q.selectedOption == q.correctIndex) Triple(Color(0xFF22C55E), Color(0xFF16A34A), Color.White) // Correct -> Green
                        else Triple(Color(0xFFEF4444), Color(0xFFDC2626), Color.White) // Wrong -> Red
                    } else Triple(Color.White, Color.Gray, Color.Black) // Unattempted -> White/Grey
                } else {
                    getColorForStatus(q.status)
                }
                
                Box(
                    modifier = Modifier.padding(4.dp).aspectRatio(1f).clip(shape).background(colorTuple.first).border(1.dp, colorTuple.second, shape).clickable {
                        viewModel.jumpToQuestion(viewModel.currentSectionIndex.value, qIndex)
                        closeDrawer()
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Text(q.globalId.toString(), color = colorTuple.third, fontWeight = FontWeight.Bold)
                    if (!isReview && q.status == QuestionStatus.ANSWERED_AND_MARKED) {
                        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp).size(8.dp).clip(CircleShape).background(Color.Green))
                    }
                }
            }
        }
        
        if (!isReview) {
            Button(
                onClick = { viewModel.submitExam(); closeDrawer() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0), contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("Submit Exam", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ResultsScreen(viewModel: ExamViewModel) {
    val test = viewModel.selectedTest.value ?: return
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC)), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFFDCFCE7)), contentAlignment = Alignment.Center) {
            Text("✓", fontSize = 40.sp, color = Color(0xFF16A34A))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Exam Submitted!", fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Score: ${viewModel.score.value} / ${test.totalQuestions}", fontSize = 24.sp, color = Color(0xFF1E90FF), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { viewModel.appState.value = "REVIEW" },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E90FF)),
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("Review Answers")
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = { viewModel.appState.value = "MENU" }) {
            Text("Return to Menu", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatusLegendItem(status: QuestionStatus, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        val shape = getShapeForStatus(status)
        val colors = getColorForStatus(status)
        Box(modifier = Modifier.size(24.dp).clip(shape).background(colors.first).border(1.dp, colors.second, shape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 12.sp)
    }
}

fun getShapeForStatus(status: QuestionStatus) = when (status) {
    QuestionStatus.NOT_ANSWERED -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    QuestionStatus.ANSWERED -> RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
    QuestionStatus.MARKED_FOR_REVIEW, QuestionStatus.ANSWERED_AND_MARKED -> CircleShape
    else -> RoundedCornerShape(4.dp)
}

fun getColorForStatus(status: QuestionStatus): Triple<Color, Color, Color> = when (status) { 
    QuestionStatus.NOT_ANSWERED -> Triple(Color(0xFFE74C3C), Color(0xFFC0392B), Color.White)
    QuestionStatus.ANSWERED -> Triple(Color(0xFF27AE60), Color(0xFF1E8449), Color.White)
    QuestionStatus.MARKED_FOR_REVIEW, QuestionStatus.ANSWERED_AND_MARKED -> Triple(Color(0xFF9B59B6), Color(0xFF7D3C98), Color.White)
    else -> Triple(Color.White, Color.LightGray, Color.Black)
}
