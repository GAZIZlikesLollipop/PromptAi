package com.app.promptai

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.app.promptai.MyApp.Companion.db
import com.app.promptai.data.repository.ChatRepository
import com.app.promptai.presentation.ChatViewModel
import com.app.promptai.presentation.theme.PromptAiTheme
import com.app.promptai.utils.ChatViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var chatViewModel: ChatViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val chatRepository = ChatRepository(db.chatDao())
        chatViewModel = viewModels<ChatViewModel> { ChatViewModelFactory(chatRepository) }.value

        setContent {
            PromptAiTheme {
                AppNavigation(chatViewModel)
            }
        }
    }
}