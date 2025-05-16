package com.app.promptai

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.promptai.presentation.ChatScreen
import com.app.promptai.presentation.ChatViewModel

@Composable
fun AppNavigation(chatViewModel: ChatViewModel){
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Route.Chat.route
    ){
        composable(Route.Chat.route){
            ChatScreen(chatViewModel)
        }

    }
}

sealed class Route(val route: String){
    object Chat: Route("chat")
    object Settings: Route("settings")
    object SelectText: Route("select_text")
}