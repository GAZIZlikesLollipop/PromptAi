package com.app.promptai.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.promptai.R
import com.app.promptai.data.UiState
import com.app.promptai.presentation.components.BaseChatScreen
import dev.jeziellago.compose.markdowntext.AutoSizeConfig
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel
) {
    val cnt = stringArrayResource(R.array.chat_cnt)

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val uiState by chatViewModel.uiState.collectAsState()

    val chats by chatViewModel.chatMap.collectAsState()
    val messages by chatViewModel.messages.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = MaterialTheme.colorScheme.background.copy(0.5f),
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxHeight().width(275.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer
            ){
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    items(chats.toList()){
                        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
                        Column {

                            Text(
                                text = it.first,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(0.5f)
                            )

                            it.second.forEach {
                                val time = Instant.ofEpochMilli(it.chat.creationTimestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
                                NavigationDrawerItem(
                                    label = {
                                        Column {
                                            Text(
                                                it.chat.name
                                            )
                                            Text(
                                                formatter.format(time),
                                                color = MaterialTheme.colorScheme.onBackground.copy(0.5f),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    },
                                    selected = false,
                                    onClick = {
                                        chatViewModel.updateChatId(it.chat.chatId)
                                        scope.launch { drawerState.close() }
                                    },
                                    shape = RoundedCornerShape(0.dp)
                                )
                            }

                        }
                    }
                }
            }
        },
    ) {
        val listState = rememberLazyListState()
        BaseChatScreen(
            viewModel = chatViewModel,
            drawState = drawerState
        ){
            if(messages.isNotEmpty()) {
                LaunchedEffect(uiState is UiState.Success || uiState is UiState.Error) {
                    delay(100)
                    listState.animateScrollToItem(messages.lastIndex)
                }
                LaunchedEffect(chatViewModel.isPrompt) {
                    listState.animateScrollToItem(messages.lastIndex)
                    chatViewModel.isPrompt = false
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(it),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(36.dp)
                ) {
                    items(messages) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            MarkdownText(
                                markdown = it.content,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(16.dp),

                                linkColor = MaterialTheme.colorScheme.onBackground,
                                syntaxHighlightColor = MaterialTheme.colorScheme.background,
                                syntaxHighlightTextColor = MaterialTheme.colorScheme.primary.copy(0.75f),
                                headingBreakColor = MaterialTheme.colorScheme.onBackground.copy(0.75f),

                                truncateOnTextOverflow = true,
                                isTextSelectable = true,
//                                disableLinkMovementMethod = true,

                                autoSizeConfig = AutoSizeConfig(
                                    autoSizeMinTextSize = 16,
                                    autoSizeMaxTextSize = 24,
                                    autoSizeStepGranularity = 2,
                                ),

                                onClick = {},
                                onLinkClicked = {},

//                                imageLoader =
                            )
                        }
                    }
                    item{}
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        cnt[0],
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

}