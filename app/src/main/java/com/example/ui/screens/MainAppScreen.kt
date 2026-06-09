package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.MemberEntity
import com.example.data.MessageEntity
import com.example.data.StoryEntity
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: MainViewModel,
    onNavigateToChat: () -> Unit
) {
    val me by viewModel.me.collectAsState()
    val members by viewModel.members.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val stories by viewModel.stories.collectAsState()
    val isSyncing by viewModel.isFetchingUsers.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    var showAddStoryDialog by remember { mutableStateOf(false) }
    var myStoryText by remember { mutableStateOf("") }

    val tabs = listOf("चैट", "स्टेटस / स्टोरी", "सक्रिय लोग")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Diversity3,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "फचे गैंग",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 22.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.syncOnlineMembers() }) {
                        if (isSyncing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "सिंक", tint = Color.White)
                        }
                    }
                    IconButton(onClick = { viewModel.clearHistory() }) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "चैट साफ करें", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF075E54) // WhatsApp Dark Green
                )
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = onNavigateToChat,
                    containerColor = Color(0xFF25D366), // WhatsApp Bright Green
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_chat"),
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Default.Chat, contentDescription = "ग्रुप चैट खोलें", modifier = Modifier.size(28.dp))
                }
            } else if (selectedTabIndex == 1) {
                FloatingActionButton(
                    onClick = { showAddStoryDialog = true },
                    containerColor = Color(0xFF128C7E),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_story"),
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = "स्टोरी जोड़े", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // WHATSAPP NAVIGATION TABS
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color(0xFF075E54),
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color(0xFF25D366)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTabIndex == index) Color.White else Color.LightGray,
                                fontSize = 15.sp
                            )
                        }
                    )
                }
            }

            // STORIES / ONLINE MEMBERS ON TOP Bar ("who join the app then her image appear on top")
            if (members.isNotEmpty()) {
                OnlineMembersRow(
                    members = members,
                    stories = stories,
                    viewModel = viewModel
                )
            }

            // TAB VIEWS
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTabIndex) {
                    0 -> ChatsTab(
                        viewModel = viewModel,
                        members = members,
                        messages = messages,
                        onNavigateToChat = onNavigateToChat
                    )
                    1 -> StatusTab(
                        viewModel = viewModel,
                        stories = stories,
                        onAddStoryClick = { showAddStoryDialog = true }
                    )
                    2 -> OnlineGangTab(
                        members = members,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Modal dialog for publishing custom story
    if (showAddStoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddStoryDialog = false },
            title = { Text("फचे गैंग में अपनी स्टोरी डालें 🚩") },
            text = {
                Column {
                    Text("आपकी अपनी स्टेटस स्टोरी गैंग के सभी सदस्यों को दिखाई देगी।", fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(
                        value = myStoryText,
                        onValueChange = { myStoryText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("क्या चल रहा है लाडले? यहाँ लिखो...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (myStoryText.trim().isNotEmpty()) {
                            viewModel.postMyStory(myStoryText.trim())
                            myStoryText = ""
                            showAddStoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128C7E))
                ) {
                    Text("स्टोरी साझा करें", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStoryDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

// HORIZONTAL ONLINE ROW - HIGHLIGHTING MEMBERS ON TOP W/ GREEN NOTIFICATION TICK
@Composable
fun OnlineMembersRow(
    members: List<MemberEntity>,
    stories: List<StoryEntity>,
    viewModel: MainViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F2F5))
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "सक्रिय सदस्य (Online Now) • " + members.size,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF075E54),
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(members) { member ->
                val memberStories = stories.filter { it.memberId == member.id }
                val hasStories = memberStories.isNotEmpty()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(68.dp)
                        .clickable {
                            if (hasStories) {
                                viewModel.showStory(memberStories)
                            }
                        }
                ) {
                    Box(modifier = Modifier.size(58.dp)) {
                        // User Avatar
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(member.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = member.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(
                                    width = if (hasStories) 2.5.dp else 1.dp,
                                    color = if (hasStories) Color(0xFF25D366) else Color.LightGray,
                                    shape = CircleShape
                                )
                        )

                        // Online green notifier
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366))
                                .border(1.5.dp, Color.White, CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (member.isMe) "आप (Me)" else member.name.split(" ")[0],
                        fontSize = 12.sp,
                        fontWeight = if (member.isMe) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// CHATS COMPONENT TAB
@Composable
fun ChatsTab(
    viewModel: MainViewModel,
    members: List<MemberEntity>,
    messages: List<MessageEntity>,
    onNavigateToChat: () -> Unit
) {
    val lastMsg = messages.lastOrNull()
    val activeCount = members.size

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            // PINNED PHACHE GANG CHAT GROUP CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToChat() }
                    .padding(vertical = 1.dp),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Overlay pile
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF128C7E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diversity3,
                            contentDescription = "फचे गैंग",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "फचे गैंग (ग्रुप चैट) 🚩🚜",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )

                            Text(
                                text = "अभी सक्रिय",
                                fontSize = 11.sp,
                                color = Color(0xFF25D366),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Preview text
                        Text(
                            text = lastMsg?.let { "${it.senderName}: ${it.text}" } ?: "गैंग चैट शुरू करने के लिए यहाँ क्लिक करें लाडले!",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Divider(color = Color(0xFFF2F2F2), thickness = 1.dp)
        }

        // List individual members as mock chat cells
        val nonMeMembers = members.filter { !it.isMe }
        items(nonMeMembers) { member ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToChat() } // Rout to central room
                    .padding(vertical = 1.dp),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = member.imageUrl,
                        contentDescription = member.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = member.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.Black
                            )

                            Text(
                                text = "ऑनलाइन",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = member.statusText,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Divider(color = Color(0xFFF2F2F2), thickness = 1.dp)
        }
    }
}

// STATUS TAB
@Composable
fun StatusTab(
    viewModel: MainViewModel,
    stories: List<StoryEntity>,
    onAddStoryClick: () -> Unit
) {
    val me by viewModel.me.collectAsState()
    val myStories = stories.filter { it.memberId == "me_user_id" }
    val otherStories = stories.filter { it.memberId != "me_user_id" }

    // Group stories by user so we show them nicely
    val storiesByUser = otherStories.groupBy { it.memberId }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            // My Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddStoryClick() },
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(54.dp)) {
                        if (me != null) {
                            AsyncImage(
                                model = me!!.imageUrl,
                                contentDescription = "Me",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.Gray)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366))
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "मेरा स्टेटस (My Status)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (myStories.isNotEmpty()) "आपने ${myStories.size} स्टोरी अपडेट पोस्ट की हैं" else "स्टोरी पोस्ट करने के लिए छुएं",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }

                    if (myStories.isNotEmpty()) {
                        IconButton(onClick = { viewModel.showStory(myStories) }) {
                            Icon(imageVector = Icons.Default.Visibility, contentDescription = "View My Story", tint = Color(0xFF128C7E))
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF0F2F5))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(text = "हाल के अपडेट (Recent Stories)", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }

        if (storiesByUser.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "अभी तक किसी सदस्य ने स्टेटस स्टोरी नहीं पोस्ट की।\nऊपर 'सिंक' बटन पर क्लिक करके स्टोरी लोड करें!",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        items(storiesByUser.entries.toList()) { entry ->
            val userStories = entry.value
            val firstStory = userStories.first()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.showStory(userStories) },
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile with ring
                    AsyncImage(
                        model = firstStory.memberImageUrl,
                        contentDescription = firstStory.memberName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFF25D366), CircleShape)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = firstStory.memberName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "स्टोरी अपडेट: \"${firstStory.text}\"", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, color = Color.Gray)
                    }

                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "View", tint = Color.LightGray)
                }
            }
            Divider(color = Color(0xFFF2F2F2), thickness = 1.dp)
        }
    }
}

// ACTIVE ONLINE MEMBERS TAB
@Composable
fun OnlineGangTab(
    members: List<MemberEntity>,
    viewModel: MainViewModel
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF0F2F5))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(text = "कुल गैंग सदस्य ऑनलाइन (Joined) • " + members.size, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }

        items(members) { member ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFECECEC))
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(50.dp)) {
                        AsyncImage(
                            model = member.imageUrl,
                            contentDescription = member.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )

                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366))
                                .border(1.dp, Color.White, CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = member.name + if (member.isMe) " (आप)" else "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = member.statusText,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
