package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun JoinScreen(
    onJoin: (name: String, gender: String) -> Unit,
    isJoining: Boolean
) {
    var name by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("Male") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF128C7E), // WhatsApp Green
                        Color(0xFF075E54)  // WhatsApp Dark Green
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Diversity3,
                    contentDescription = "गैंग",
                    tint = Color(0xFF128C7E),
                    modifier = Modifier
                        .size(80.dp)
                        .padding(bottom = 12.dp)
                )

                Text(
                    text = "फचे गैंग",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF075E54),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "भाईचारे की नई ऑनलाइन चौपाल 🚜🔥",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Input Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("अपना नाम दर्ज करें (Hindi / English)") },
                    placeholder = { Text("जैसे: रामनिवास ठेकेदार") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("name_input"),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF128C7E),
                        focusedLabelColor = Color(0xFF128C7E)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "अवतार चुनिए (Avatar Style)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )

                // Gender Avatar Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Male choice
                    Button(
                        onClick = { selectedGender = "Male" },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("gender_male"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedGender == "Male") Color(0xFF128C7E) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedGender == "Male") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Male, contentDescription = "पुरुष", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("पुरुष (Male)")
                    }

                    // Female choice
                    Button(
                        onClick = { selectedGender = "Female" },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("gender_female"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedGender == "Female") Color(0xFF128C7E) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedGender == "Female") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Female, contentDescription = "महिला", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("महिला (Female)")
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                if (isJoining) {
                    CircularProgressIndicator(color = Color(0xFF128C7E))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "ऑनलाइन सदस्यों को जोड़ रहे हैं...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Button(
                        onClick = {
                            if (name.trim().isNotEmpty()) {
                                onJoin(name.trim(), selectedGender)
                            }
                        },
                        enabled = name.trim().isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("submit_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF128C7E),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "गैंग में शामिल हों (Join Gang)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Helper to use compose with state objects in clean approach
@Composable
fun <T> rememberStateOf(initial: T): MutableState<T> {
    return remember { mutableStateOf(initial) }
}

fun <T> mutableStateOf(value: T): MutableState<T> {
    return androidx.compose.runtime.mutableStateOf(value)
}
