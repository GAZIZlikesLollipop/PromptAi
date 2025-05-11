package com.app.promptai

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.promptai.presentation.ChatScreen

@Composable
fun AppNavigation(){
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Route.Chat.route
    ){
        composable(Route.Chat.route){
            ChatScreen()
        }

    }
}

sealed class Route(val route: String){
    object Chat: Route("chat")
    object Settings: Route("settings")
}