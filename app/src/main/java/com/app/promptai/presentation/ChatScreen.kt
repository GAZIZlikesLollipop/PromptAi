package com.app.promptai.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.promptai.R
import com.app.promptai.data.UiState
import com.app.promptai.data.database.SenderType
import com.app.promptai.presentation.components.BaseChatScreen
import dev.jeziellago.compose.markdowntext.AutoSizeConfig
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.widget.Toast // Это android.widget.Toast - правильно
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.platform.LocalClipboardManager // <-- И LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel
) {
    val cnt = stringArrayResource(R.array.chat_cnt)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

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
                    items(messages) { msg ->
                        if(msg.senderType == SenderType.AI) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Surface(
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.onBackground,
                                    shape = RoundedCornerShape(
                                        bottomEnd = 24.dp,
                                        bottomStart = 20.dp,
                                        topEnd = 24.dp,
                                        topStart = 4.dp
                                    )
                                ) {
                                    ChatText(msg.content)
                                }
                            }
                        }else{
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(
                                        bottomEnd = 20.dp,
                                        bottomStart = 24.dp,
                                        topEnd = 4.dp,
                                        topStart = 20.dp,
                                    ),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(16.dp),
                                    contentColor = MaterialTheme.colorScheme.onBackground
                                ) {
                                    ChatText(msg.content)
                                }
                                Row(
                                    modifier = Modifier,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
//                                    repeat(3) {
//                                        val text = when(it){
//                                            0 -> cnt[6]
//                                            1 -> cnt[7]
//                                            else -> cnt[8]
//                                        }
//                                        val icon= when(it){
//                                            0 -> Icons.Outlined.ContentCopy
//                                            1 -> Icons.Outlined.Edit
//                                            else -> ImageVector.vectorResource(R.drawable.text_select)
//                                        }
//                                        val callback= when(it){
//                                            0 -> {
//                                                clipboardManager.setText(AnnotatedString(msg.content))
//                                                Toast.makeText(context, "Текст скопирован в буфер!", Toast.LENGTH_SHORT).show()
//                                            }
//                                            1 -> cnt[7]
//                                            else -> cnt[8]
//                                        }
//                                    }
                                }
                            }
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

@Composable
private fun ChatText(
    text: String
){
    MarkdownText(
        markdown = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(10.dp),

        linkColor = MaterialTheme.colorScheme.onBackground,
        syntaxHighlightColor = MaterialTheme.colorScheme.background,
        syntaxHighlightTextColor = MaterialTheme.colorScheme.primary.copy(0.75f),
        headingBreakColor = MaterialTheme.colorScheme.onBackground.copy(0.75f),

        truncateOnTextOverflow = true,
        isTextSelectable = true,
//      disableLinkMovementMethod = true,

        autoSizeConfig = AutoSizeConfig(
            autoSizeMinTextSize = 16,
            autoSizeMaxTextSize = 24,
            autoSizeStepGranularity = 2,
        ),

        onClick = {},
        onLinkClicked = {},

//      imageLoader =
    )
}