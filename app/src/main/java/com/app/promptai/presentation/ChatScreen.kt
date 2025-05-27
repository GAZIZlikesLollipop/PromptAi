package com.app.promptai.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.promptai.R
import com.app.promptai.data.database.ChatEntity
import com.app.promptai.presentation.components.BaseChatScreen
import com.app.promptai.presentation.components.ChatCard
import com.app.promptai.presentation.components.ChatContent
import com.app.promptai.utils.ApiState
import com.app.promptai.utils.UiState
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel
) {
    val cnt = stringArrayResource(R.array.chat_cnt)
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val alrt = stringArrayResource(R.array.alert_dialog)
    val focusManager = LocalFocusManager.current

    val uiState by chatViewModel.uiState.collectAsState()

    val chatsMap by chatViewModel.chatMap.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val chats by chatViewModel.chats.collectAsState()

    val chatId by chatViewModel.currentChatId.collectAsState()
    val apiState by remember { mutableStateOf(if(chats.isNotEmpty()) chats[chatId.toInt()].chat.chatState else ApiState.Initial) }

    var pressOffset by remember { mutableStateOf(Offset.Zero) }
    var isEditName by rememberSaveable { mutableStateOf(false) }
    var isDelete by rememberSaveable { mutableStateOf(false) }
    var editedText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(chatViewModel.isEdit) {
        if(messages.isNotEmpty()) {
            if (messages[chatViewModel.editingMessageId].pictures.isNotEmpty()) {
                if (chatViewModel.isEdit == true) chatViewModel.picList.addAll(messages[chatViewModel.editingMessageId].pictures) else chatViewModel.picList.clear()
            }
            if(messages[chatViewModel.editingMessageId].files.isNotEmpty()){
                if (chatViewModel.isEdit == true) chatViewModel.fileList.addAll(messages[chatViewModel.editingMessageId].files) else chatViewModel.fileList.clear()
            }
        }
    }

    LaunchedEffect(messages.size) {
        if(messages.isNotEmpty()) {
            chatViewModel.msgId = messages.lastIndex + 1
        }
    }

    LaunchedEffect(drawerState.isOpen) {
        chatViewModel.isOpen = drawerState.isOpen
    }

    Box(Modifier.fillMaxSize()){
        ModalNavigationDrawer(
            drawerState = drawerState,
            scrimColor = MaterialTheme.colorScheme.background.copy(0.5f),
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.fillMaxHeight().width(275.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        items(chatsMap.toList()) { chit ->
                            Column {

                                Text(
                                    text = chit.first,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground.copy(0.5f)
                                )

                                chit.second.forEachIndexed { ind, cht ->
                                    var isMenu by rememberSaveable { mutableStateOf(false) }
                                    ChatCard(
                                        isMenu = isMenu,
                                        chat = cht.chat,
                                        drawerState = drawerState,
                                        onLongPress = { offset ->
                                            pressOffset = offset
                                            chatViewModel.manipulChatId = ind
                                            isMenu = true
                                        },
                                        onTap = {
                                            chatViewModel.updateChatId(cht.chat.chatId)
                                            scope.launch { drawerState.close() }
                                        },
                                        onRename = {
                                            chatViewModel.manipulChatId = ind
                                            isEditName = true
                                        },
                                        switchIsFavorite = {
                                            chatViewModel.manipulChatId = ind
                                            chatViewModel.editChat(
                                                ChatEntity(
                                                    chatId = chats[chatViewModel.manipulChatId].chat.chatId,
                                                    name = chats[chatViewModel.manipulChatId].chat.name,
                                                    isFavorite = !chats[chatViewModel.manipulChatId].chat.isFavorite
                                                )
                                            )
                                        },
                                        onDelete = {
                                            chatViewModel.manipulChatId = ind
                                            isDelete = true
                                        },
                                        menuFalse = { isMenu = false }
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
            ) {
                when {
                    uiState is UiState.Success && messages.isNotEmpty() -> {
                        LaunchedEffect(apiState is ApiState.Success || apiState is ApiState.Error || apiState is ApiState.Loading) {
//                            delay(200)
                            listState.animateScrollToItem(messages.lastIndex)
                        }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(it).offset(y = 50.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(36.dp)
                        ) {
                            itemsIndexed(messages) { ind, msg ->
                                if (apiState is ApiState.Error || msg.content.isBlank() && apiState !is ApiState.Loading && apiState !is ApiState.Success) {
                                    if (msg.content.isNotBlank()) {
                                        ChatContent(
                                            msg = msg,
                                            chatViewModel = chatViewModel,
                                            messages = messages,
                                            ind = ind,
                                            apiState = apiState
                                        )
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
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Text(
                                                        cnt[9],
                                                        color = MaterialTheme.colorScheme.onError
                                                    )
                                                    Spacer(Modifier.height(12.dp))
                                                    Button(
                                                        onClick = {
                                                            chatViewModel.regenerateResponse(
                                                                message = messages[ind - 1],
                                                                pics = messages[ind - 1].pictures,
                                                                files = messages[ind - 1].files
                                                            )
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                                            contentColor = MaterialTheme.colorScheme.error
                                                        ),
                                                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onError)
                                                    ) {
                                                        Text(cnt[10])
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    ChatContent(
                                        msg = msg,
                                        chatViewModel = chatViewModel,
                                        messages = messages,
                                        ind = ind,
                                        apiState = apiState
                                    )
                                }
                            }
                            item {}
                            item {}
                            item {}
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
        AnimatedVisibility(
            visible = isEditName || isDelete,
            enter = fadeIn(tween(300,50)),
            exit = fadeOut(tween(300,50))
        ) { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(0.35f))) }

        AnimatedVisibility(
            visible = isDelete,
            modifier = Modifier.fillMaxSize()
        ) {
            AlertDialog(
                onDismissRequest = { },
                text = {
                    Text(
                        text = alrt[2],
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            chatViewModel.deleteChat(chats[chatViewModel.manipulChatId].chat)
                            isDelete = false
                        }
                    ) {
                        Text(
                            text = alrt[0],
                            color = MaterialTheme.colorScheme.errorContainer
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isDelete = false }) {
                        Text(alrt[1])
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = isEditName,
            enter = fadeIn(tween(300,50)),
            exit = fadeOut(tween(300,50))
        ) {
            LaunchedEffect(Unit) {
                editedText = chats[chatViewModel.manipulChatId].chat.name
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ){
                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHighest)
                ){
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        OutlinedTextField(
                            value = editedText,
                            onValueChange = {editedText = it},
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            maxLines = 1,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ){
                            repeat(2) {
                                TextButton(
                                    onClick = {
                                        isEditName = false
                                        if(it == 1) {
                                            chatViewModel.editChat(
                                                ChatEntity(
                                                    chatId = chats[chatViewModel.manipulChatId].chat.chatId,
                                                    name = editedText
                                                )
                                            )
                                        }
                                        focusManager.clearFocus()
                                    }
                                ) {
                                    Text(
                                        text = if(it == 0) alrt[1] else alrt[0],
                                        color = if(it == 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}