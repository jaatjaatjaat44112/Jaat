package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.screens.ChatRoomScreen
import com.example.ui.screens.JoinScreen
import com.example.ui.screens.MainAppScreen
import com.example.ui.screens.StoryViewerScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(dynamicColor = false) { // Disable dynamic color to enforce WhatsApp Green Branding
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModelFactory(application)
                )

                val me by viewModel.me.collectAsState()
                val activeStoryView by viewModel.activeStoryView.collectAsState()
                var currentScreen by remember { mutableStateOf("home") } // "home" or "chat_room"

                Box(modifier = Modifier.fillMaxSize()) {
                    if (me == null) {
                        JoinScreen(
                            onJoin = { name, gender ->
                                viewModel.joinGang(name, gender)
                            },
                            isJoining = false
                        )
                    } else {
                        // Main Application screen routing
                        if (currentScreen == "home") {
                            MainAppScreen(
                                viewModel = viewModel,
                                onNavigateToChat = { currentScreen = "chat_room" }
                            )
                        } else {
                            ChatRoomScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = "home" }
                            )
                        }

                        // IMMERSIVE STORY UPDATES VIEWER OVERLAY
                        AnimatedVisibility(
                            visible = activeStoryView != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            activeStoryView?.let { stories ->
                                StoryViewerScreen(
                                    stories = stories,
                                    onDismiss = { viewModel.dismissStory() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

