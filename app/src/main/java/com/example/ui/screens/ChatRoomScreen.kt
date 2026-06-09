package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.MessageEntity
import com.example.ui.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val members by viewModel.members.collectAsState()
    val typingMember by viewModel.typingMember.collectAsState()
    val isSending by viewModel.isSendingMessage.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto scroll to last message on load or change
    LaunchedEffect(messages.size, typingMember) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Group pile default image
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Diversity3, contentDescription = "फचे गैंग", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "फचे गैंग (ग्रुप चैट) 🚩🚜",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (typingMember != null) "$typingMember typing..." else "${members.size} सदस्य ऑनलाइन",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF075E54)
                ),
                actions = {
                    IconButton(onClick = { viewModel.syncOnlineMembers() }) {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync", tint = Color.White)
                    }
                }
            )
        }
    ) { innerPadding ->
        // WALLPAPER BACKGROUND STYLED LIKE WHATSAPP CHAT
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFECE5DD)) // Typical cream WhatsApp background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // List of Messages
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        // Secure/Information box like WhatsApp
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2D1)),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = "🔒 इस चैट में भेजे गए मैसेज असली इंटरनेट सर्वर और जेनरेटर (Gemini) से जुड़े हुए हैं। बाहरी दुनिया के लोग फचे गैंग से जुड़कर आपके संदेश पढ़ सकते हैं!",
                                fontSize = 11.sp,
                                color = Color(0xFF6B5525),
                                modifier = Modifier.padding(8.dp),
                                fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    items(messages) { message ->
                        MessageBubble(message = message)
                    }

                    if (typingMember != null) {
                        item {
                            TypingBubble(senderName = typingMember!!)
                        }
                    }
                }

                // Bottom Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chat input rounded card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Default.InsertEmoticon, contentDescription = "Emoji", tint = Color.Gray)
                            
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                placeholder = { Text("मैसेज लिखें...", fontSize = 15.sp, color = Color.Gray) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input_text"),
                                maxLines = 4,
                                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    disabledBorderColor = Color.Transparent,
                                    errorBorderColor = Color.Transparent
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (textInput.trim().isNotEmpty()) {
                                            viewModel.addManualMeMessage(textInput.trim())
                                            textInput = ""
                                        }
                                    }
                                )
                            )

                            Icon(imageVector = Icons.Default.AttachFile, contentDescription = "Attach", tint = Color.Gray, modifier = Modifier.padding(horizontal = 4.dp))
                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Camera", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Floating circular Send action
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF128C7E))
                            .clickable {
                                if (textInput.trim().isNotEmpty()) {
                                    viewModel.addManualMeMessage(textInput.trim())
                                    textInput = ""
                                }
                            }
                            .testTag("chat_send_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: MessageEntity) {
    val alignMe = message.isMe
    val dateString = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (alignMe) Arrangement.End else Arrangement.Start
    ) {
        if (!alignMe) {
            AsyncImage(
                model = message.senderImageUrl,
                contentDescription = message.senderName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .align(Alignment.Bottom)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (alignMe) 14.dp else 2.dp,
                bottomEnd = if (alignMe) 2.dp else 14.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (alignMe) Color(0xFFE2F9C3) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                if (!alignMe) {
                    // Pick a random nice color for sender name so it stands out
                    Text(
                        text = message.senderName,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF128C7E),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Text(
                    text = message.text,
                    color = Color.Black,
                    fontSize = 14.5.sp
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateString,
                        color = Color.LightGray,
                        fontSize = 9.sp
                    )
                    if (alignMe) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Delivered Ticks",
                            tint = Color(0xFF34B7F1),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TypingBubble(senderName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Diversity3, contentDescription = "typing", tint = Color.Gray, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.width(8.dp))

        Card(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = 2.dp,
                bottomEnd = 14.dp
            ),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$senderName typing",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Animated dots
                DotsTypingIndicator()
            }
        }
    }
}

@Composable
fun DotsTypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 150),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Gray.copy(alpha = alpha1)))
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Gray.copy(alpha = alpha2)))
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Gray.copy(alpha = alpha3)))
    }
}
