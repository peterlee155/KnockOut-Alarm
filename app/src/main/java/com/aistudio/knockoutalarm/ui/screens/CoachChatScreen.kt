package com.aistudio.knockoutalarm.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.knockoutalarm.ai.GeminiCoachService
import com.example.ui.theme.ArenaBackground
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.ArenaSurfaceVariant
import com.example.ui.theme.BoxingGreen
import com.example.ui.theme.BoxingMuted
import com.example.ui.theme.BoxingRed
import com.example.ui.theme.BoxingWhite
import com.example.ui.theme.ChampionshipGold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachChatScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("🥊 Coach Chat", "🔍 Grounding", "🧬 Science", "🎵 Walkout Music", "🎙️ Live & Voice")

    // Chat thread state
    val chatMessages = remember {
        mutableStateListOf(
            GeminiCoachService.ChatMessage(
                role = "model",
                text = "Welcome to the corner, champ! I'm Coach Mick. I'll analyze your morning routine, fine-tune your jab & cross form, and explain the sports science of waking up with punches. What's on your mind?"
            )
        )
    }
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Tab 1 (Google Search Grounding)
    var searchQuery by remember { mutableStateOf("Latest boxing championship fight results") }
    var searchResult by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    // Tab 2 (Exercise Science)
    var scienceResult by remember { mutableStateOf<String?>(null) }
    var isScienceLoading by remember { mutableStateOf(false) }

    // Tab 3 (Lyria Music generation)
    var musicPrompt by remember { mutableStateOf("Intense heavyweight walkout rap beat with heavy brass horns") }
    var musicStatus by remember { mutableStateOf<String?>(null) }
    var isMusicGenerating by remember { mutableStateOf(false) }

    // Tab 4 (Live Voice & Audio)
    var voiceStatus by remember { mutableStateOf("gemini-3.8-live conversational session ready.") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ArenaBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AI CORNER COACH",
                            color = BoxingWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ChampionshipGold)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("GEMINI", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BoxingWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ArenaSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Scrollable tabs for the skills
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = ArenaSurface,
                contentColor = ChampionshipGold,
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) ChampionshipGold else BoxingMuted
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // --- MULTI-TURN CHAT INTERFACE ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(chatMessages) { msg ->
                                val isUser = msg.role == "user"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isUser) BoxingRed else ArenaSurfaceVariant)
                                            .border(
                                                1.dp,
                                                if (isUser) BoxingRed else Color.White.copy(alpha = 0.1f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(14.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = if (isUser) "YOU" else "COACH MICK (gemini-3.1-pro)",
                                                color = if (isUser) ChampionshipGold else BoxingGreen,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = msg.text,
                                                color = BoxingWhite,
                                                fontSize = 14.sp,
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                }
                            }

                            if (isThinking) {
                                item {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            color = ChampionshipGold,
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Coach Mick is analyzing your bout...", color = BoxingMuted, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Message Input
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text("Ask Coach about boxing form, routine...", color = BoxingMuted, fontSize = 13.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("coach_input_field"),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = BoxingWhite,
                                    unfocusedTextColor = BoxingWhite,
                                    focusedContainerColor = ArenaSurface,
                                    unfocusedContainerColor = ArenaSurface,
                                    focusedBorderColor = BoxingRed,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        val prompt = inputText.trim()
                                        if (prompt.isNotBlank() && !isThinking) {
                                            inputText = ""
                                            chatMessages.add(GeminiCoachService.ChatMessage("user", prompt))
                                            isThinking = true
                                            coroutineScope.launch {
                                                val reply = GeminiCoachService.sendChatMessage(chatMessages.toList(), prompt)
                                                chatMessages.add(GeminiCoachService.ChatMessage("model", reply))
                                                isThinking = false
                                                listState.animateScrollToItem(chatMessages.size - 1)
                                            }
                                        }
                                    }
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    val prompt = inputText.trim()
                                    if (prompt.isNotBlank() && !isThinking) {
                                        inputText = ""
                                        chatMessages.add(GeminiCoachService.ChatMessage("user", prompt))
                                        isThinking = true
                                        coroutineScope.launch {
                                            val reply = GeminiCoachService.sendChatMessage(chatMessages.toList(), prompt)
                                            chatMessages.add(GeminiCoachService.ChatMessage("model", reply))
                                            isThinking = false
                                            listState.animateScrollToItem(chatMessages.size - 1)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(BoxingRed)
                                    .testTag("coach_send_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                1 -> {
                    // --- GOOGLE SEARCH GROUNDING TAB ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "GOOGLE SEARCH GROUNDED INTELLIGENCE",
                            color = ChampionshipGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Uses gemini-3.5-flash with live googleSearch tool for verified real-time sports, fight news, and training weather.",
                            color = BoxingMuted,
                            fontSize = 12.sp
                        )

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search Grounding Query") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                isSearching = true
                                coroutineScope.launch {
                                    searchResult = GeminiCoachService.getSearchGroundedInfo(searchQuery)
                                    isSearching = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BoxingRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RUN GROUNDED SEARCH", fontWeight = FontWeight.Bold)
                        }

                        if (isSearching) {
                            CircularProgressIndicator(color = ChampionshipGold, modifier = Modifier.align(Alignment.CenterHorizontally))
                        }

                        searchResult?.let { result ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = ArenaSurface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("GROUNDED RESPONSE:", color = BoxingGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(result, color = BoxingWhite, fontSize = 13.sp, lineHeight = 19.sp)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // --- EXERCISE SCIENCE TAB ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "EXERCISE SCIENCE & PHYSIOLOGY SKILLS",
                            color = ChampionshipGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Scientific biomechanics breakdown of rapid sleep-inertia elimination via morning straight punches.",
                            color = BoxingMuted,
                            fontSize = 12.sp
                        )

                        val scienceTopics = listOf(
                            "Rapid sleep-inertia clearance via punch kinematics",
                            "Dopamine & norepinephrine spikes from 30 punches",
                            "Shoulder stabilizer recruitment & elbow protection"
                        )

                        scienceTopics.forEach { topic ->
                            Button(
                                onClick = {
                                    isScienceLoading = true
                                    coroutineScope.launch {
                                        scienceResult = GeminiCoachService.getScienceInsights(topic)
                                        isScienceLoading = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ArenaSurfaceVariant),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Science, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(topic, fontSize = 12.sp, color = BoxingWhite, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (isScienceLoading) {
                            CircularProgressIndicator(color = ChampionshipGold, modifier = Modifier.align(Alignment.CenterHorizontally))
                        }

                        scienceResult?.let { result ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = ArenaSurface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("SPORTS SCIENCE REPORT:", color = ChampionshipGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(result, color = BoxingWhite, fontSize = 13.sp, lineHeight = 19.sp)
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // --- MUSIC GENERATION (LYRIA) TAB ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "AI WALKOUT SOUNDTRACK GENERATOR",
                            color = ChampionshipGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Uses lyria-3-clip-preview to generate energetic 30-second alarm ring walkout tracks.",
                            color = BoxingMuted,
                            fontSize = 12.sp
                        )

                        OutlinedTextField(
                            value = musicPrompt,
                            onValueChange = { musicPrompt = it },
                            label = { Text("Soundtrack Theme Description") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                isMusicGenerating = true
                                coroutineScope.launch {
                                    musicStatus = GeminiCoachService.generateFightSoundtrack(musicPrompt)
                                    isMusicGenerating = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BoxingRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GENERATE WALKOUT TRACK (lyria-3)", fontWeight = FontWeight.Bold)
                        }

                        if (isMusicGenerating) {
                            CircularProgressIndicator(color = ChampionshipGold, modifier = Modifier.align(Alignment.CenterHorizontally))
                        }

                        musicStatus?.let { status ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BoxingGreen.copy(alpha = 0.2f))
                                    .border(1.dp, BoxingGreen, RoundedCornerShape(8.dp))
                                    .padding(14.dp)
                            ) {
                                Text(status, color = BoxingWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                4 -> {
                    // --- LIVE CONVERSATIONS & TRANSCRIPTION TAB ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "VOICE CONVERSATION & AUDIO TRANSCRIPTION",
                            color = ChampionshipGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Uses gemini-3.8-live (Live API) for real-time corner coach dialogue and gemini-3.5-transcribe for audio note transcription.",
                            color = BoxingMuted,
                            fontSize = 12.sp
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = ArenaSurface)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = BoxingRed, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Live Coach Session",
                                    color = BoxingWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = voiceStatus,
                                    color = BoxingMuted,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        voiceStatus = "Live session active! Coach Mick is listening for fight commands."
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BoxingRed)
                                ) {
                                    Text("START LIVE VOICE SESSION (gemini-3.8-live)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
