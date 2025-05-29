@file:OptIn(ExperimentalMaterial3Api::class)

package com.app.promptai.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.promptai.R
import com.app.promptai.presentation.ChatViewModel
import com.app.promptai.utils.ApiState
import com.app.promptai.utils.UiState
import kotlinx.coroutines.launch

@Composable
fun BaseChatScreen(
    viewModel: ChatViewModel,
    drawState: DrawerState,
    content: @Composable ((PaddingValues) -> Unit),
){
    val prompt by viewModel.userPrompt.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val chats by viewModel.chats.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val cnt = stringArrayResource(R.array.chatUi_cnt)

    val chatId by viewModel.currentChatId.collectAsState()

    val chatName = if(messages.isNotEmpty() && chats.isNotEmpty()){
        chats[viewModel.currentChatId.collectAsState().value.toInt()].chat.name
    }else{
        cnt[0]
    }

    Scaffold(
        topBar = {
            TopChatBar(
                state = drawState,
                onNew = viewModel::newChat,
                chatName = chatName,
                uiState = uiState
            )
        },
        bottomBar = {
            TypingChatBar(
                text = prompt,
                sendPrompt = viewModel::getResponse,
                apiState = if(chats.isNotEmpty() && chatId.toInt() <= chats.lastIndex)chats[chatId.toInt()].chat.chatState else ApiState.Initial,
                isMore = viewModel.isMore,
                uiState = uiState,
                onMore = viewModel::switchIsMore,
                isEdit = viewModel.isEdit,
                editMessage = viewModel::editMessage,
                previousMsg = if(messages.isNotEmpty())viewModel.messages.collectAsState().value[viewModel.editingMessageId].content else "",
                isOpen = viewModel.isOpen,
                switchIsEdit = viewModel::switchIsEdit,
                picList = viewModel.picList,
                fileList = viewModel.fileList,
                aiMsg = viewModel.aiMsg
            )
        },
        modifier = Modifier.fillMaxSize()
    ){
        content(it)
    }
}

@Composable
fun TopChatBar(
    state: DrawerState,
    onNew: () -> Unit,
    chatName: String,
    uiState: UiState
){
    val coroutineScope = rememberCoroutineScope()
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "Menu",
                    modifier = Modifier.size(30.dp).clickable { if(uiState !is UiState.Initial) coroutineScope.launch { state.open() } },
                    tint = if(uiState is UiState.Initial) MaterialTheme.colorScheme.onBackground.copy(0.5f) else MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = chatName,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.new_chat),
                    contentDescription = "new chat",
                    modifier = Modifier.size(30.dp).clickable { if(uiState !is UiState.Initial) onNew() },
                    tint = if(uiState is UiState.Initial) MaterialTheme.colorScheme.onBackground.copy(0.5f) else MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.width(20.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.surfaceContainer)
    )
}