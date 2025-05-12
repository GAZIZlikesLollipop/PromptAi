package com.app.promptai.presentation

import androidx.compose.foundation.layout.Arrangement
import com.app.promptai.R
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import com.app.promptai.data.UiState
import com.app.promptai.presentation.components.BaseChatScreen
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val chats by chatViewModel.chats.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val currentChatId by chatViewModel.currentChatId.collectAsState()
    val cnt = stringArrayResource(R.array.chat_cnt)
    val uiState by chatViewModel.uiState.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = MaterialTheme.colorScheme.background.copy(0.5f),
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxHeight().width(275.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer
            ){
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chats){
                        NavigationDrawerItem(
                            label = { Text(it.chat.name) },
                            selected = false,
                            onClick = {
                                chatViewModel.updateChatId(it.chat.chatId)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        },
    ) {
        val listState = rememberLazyListState()
        BaseChatScreen(
            viewModel = chatViewModel,
            onMenu = { scope.launch { drawerState.open() } },
            onNew = { chatViewModel.newChat() },
            chatId = currentChatId
        ){
            LaunchedEffect(uiState is UiState.Success || uiState is UiState.Error) {
                listState.animateScrollToItem(messages.lastIndex)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(it),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ){
                if(messages.isNotEmpty()) {
                    items(messages) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Text(
                                it.content,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }else{
                    item {
                        Text(
                            cnt[0],
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }

}