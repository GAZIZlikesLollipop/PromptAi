package com.app.promptai.presentation

import android.content.ClipData
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.promptai.R
import com.app.promptai.data.ApiState
import com.app.promptai.data.UiState
import com.app.promptai.data.database.SenderType
import com.app.promptai.presentation.components.BaseChatScreen
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

    val apiState by chatViewModel.apiState.collectAsState()
    val uiState by chatViewModel.uiState.collectAsState()

    val chats by chatViewModel.chatMap.collectAsState()
    val messages by chatViewModel.messages.collectAsState()

    LaunchedEffect(messages.size) {
        if(messages.isNotEmpty()) {
            chatViewModel.msgId = messages.lastIndex + 1
        }
    }

    LaunchedEffect(drawerState.isOpen) {
        chatViewModel.isOpen = drawerState.isOpen
    }

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
                                            Text(it.chat.name)
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
            when {
                uiState is UiState.Success && messages.isNotEmpty() -> {
                    LaunchedEffect(apiState is ApiState.Success || apiState is ApiState.Error || apiState is ApiState.Loading) {
                        delay(100)
                        listState.animateScrollToItem(messages.lastIndex)
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(it),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(36.dp)
                    ) {
                        itemsIndexed(messages) {ind, msg ->
                            if(apiState is ApiState.Error || msg.content.isBlank() && apiState !is ApiState.Loading){
                                if(msg.content.isNotBlank()){
                                    if(msg.senderType == SenderType.USER) {
                                        ChatContent(
                                            message = msg.content,
                                            senderType = SenderType.USER,
                                            onEdit = {
                                                chatViewModel.isEdit = true
                                                chatViewModel.editingMessageId = ind
                                            }
                                        )
                                    }else{
                                        ChatContent(
                                            message = msg.content,
                                            senderType = SenderType.AI,
                                            onRegenerate = {
                                                chatViewModel.regenerateResponse(message = messages[ind-1])
                                            }
                                        )
                                    }
                                } else {
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
                                                topStart = 3.dp
                                            ),
                                            tonalElevation = 12.dp,
                                            shadowElevation = 12.dp,
                                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.onError)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Text(
                                                    cnt[9],
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = MaterialTheme.colorScheme.onError
                                                )
                                                Button(
                                                    onClick = {
                                                        chatViewModel.regenerateResponse(message = messages[ind-1])
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                                        contentColor = MaterialTheme.colorScheme.error
                                                    ),
                                                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onError)
                                                ) {
                                                    Text(
                                                        cnt[10]
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }else{
                                if(msg.senderType == SenderType.USER) {
                                    ChatContent(
                                        message = msg.content,
                                        senderType = SenderType.USER,
                                        onEdit = {
                                            chatViewModel.isEdit = true
                                            chatViewModel.editingMessageId = ind
                                        }
                                    )
                                }else {
                                    ChatContent(
                                        message = msg.content,
                                        senderType = SenderType.AI,
                                        onRegenerate = {
                                            chatViewModel.regenerateResponse(message = messages[ind - 1])
                                        }
                                    )
                                }
                            }
                        }
                        item{}
                    }
                }
                uiState is UiState.Initial || messages.isEmpty() -> {
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

}

@Composable
private fun ChatText(text: String){
    MarkdownText(
        markdown = text,
        modifier = Modifier.padding(12.dp),

        linkColor = MaterialTheme.colorScheme.onBackground,
        syntaxHighlightColor = MaterialTheme.colorScheme.background,
        syntaxHighlightTextColor = MaterialTheme.colorScheme.primary.copy(0.75f),
        headingBreakColor = MaterialTheme.colorScheme.onBackground.copy(0.75f),

        isTextSelectable = true,

        onClick = {},
        onLinkClicked = {},

//      imageLoader =
    )
}

@Composable
private fun ChatContent(
    message: String,
    senderType: SenderType,
    onRegenerate: () -> Unit = {},
    onEdit: () -> Unit = {}
){
    val coroutine = rememberCoroutineScope()
    val clipboardManager = LocalClipboard.current
    val cnt = stringArrayResource(R.array.chat_cnt)

    if(message.isNotEmpty() || senderType == SenderType.USER) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (senderType == SenderType.USER) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    bottomEnd = 24.dp,
                    bottomStart = 24.dp,
                    topEnd = if (senderType == SenderType.USER) 4.dp else 24.dp,
                    topStart = if (senderType == SenderType.USER) 24.dp else 4.dp,
                ),
                color = if (senderType == SenderType.USER) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.padding(16.dp),
                contentColor = MaterialTheme.colorScheme.onBackground,
                tonalElevation = 12.dp,
                shadowElevation = 12.dp
            ) {
                ChatText(message)
            }
            Row(
                modifier = Modifier.offset(x = if (senderType == SenderType.USER) (-12).dp else 12.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(2) {

                    val text = if(it == 0) cnt[6] else if (senderType == SenderType.USER) cnt[7] else cnt[8]
                    val icon = if(it == 0)  Icons.Outlined.ContentCopy else if (senderType == SenderType.USER) Icons.Outlined.Edit else Icons.Outlined.Cached

                    Surface(
                        onClick = {
                            if(it == 0){
                                coroutine.launch { clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("", message))) }
                            }else{
                                if(senderType == SenderType.AI) onRegenerate() else onEdit()
                            }
                        },
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = text,
                                tint = MaterialTheme.colorScheme.onBackground.copy(0.35f),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(0.35f)
                            )
                        }
                    }
                }
            }
        }
    }else{
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier,
                trackColor = MaterialTheme.colorScheme.onBackground,
                strokeCap = StrokeCap.Round,
            )
        }
    }
}