@file:OptIn(ExperimentalMaterial3Api::class)

package com.app.promptai.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.promptai.R
import com.app.promptai.data.ApiState
import com.app.promptai.data.UiState
import com.app.promptai.presentation.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun BaseChatScreen(
    viewModel: ChatViewModel,
    drawState: DrawerState,
    content: @Composable ((PaddingValues) -> Unit),
){
    val cnt = stringArrayResource(R.array.chatUi_cnt)
    val prompt by viewModel.userPrompt.collectAsState()
    val apiState by viewModel.apiState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val chats by viewModel.chats.collectAsState()
    val messages by viewModel.messages.collectAsState()
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
                uiState = uiState,
                apiState = apiState
            )
        },
        bottomBar = {
            TypingChatBar(
                text = prompt,
                sendPrompt = { pr, boo ->
                    viewModel.sendPrompt(null,pr)
                    viewModel.isPrompt = boo
                },
                apiState = apiState,
                isMore = viewModel.isMore,
                uiState = uiState,
                onMore = viewModel::switchIsMore
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
    uiState: UiState,
    apiState: ApiState
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
                    modifier = Modifier.size(30.dp).clickable { if(uiState !is UiState.Initial && apiState !is ApiState.Error) coroutineScope.launch { state.open() } },
                    tint = if(uiState is UiState.Initial && apiState !is ApiState.Error) MaterialTheme.colorScheme.onBackground.copy(0.5f) else MaterialTheme.colorScheme.onBackground
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

@Composable
fun TypingChatBar(
    text: String,
    sendPrompt: (String,Boolean) -> Unit,
    apiState: ApiState,
    isMore: Boolean,
    uiState: UiState,
    onMore: () -> Unit
){
    var prompt by rememberSaveable { mutableStateOf(text) }
    val cnt = stringArrayResource(R.array.chatUi_cnt)
    val focusManager = LocalFocusManager.current
    val rotateAnim by animateFloatAsState(
        targetValue = if(isMore) 45f else 0f,
        animationSpec = tween(300,50),
    )
    val colorAnim by animateColorAsState(
        targetValue = if(isMore) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary,
        animationSpec = tween(300,50)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
            .imePadding()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = prompt,
                onValueChange = {prompt = it},
                modifier = Modifier.fillMaxWidth(),
                maxLines = 6,
                placeholder = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            cnt[1],
                            color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(20.dp),
//                isError = uiState is UiState.Error,
                enabled = uiState is UiState.Success && apiState !is ApiState.Error
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center){
                    Button(
                        onClick = onMore,
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, colorAnim),
                        content = {},
                        enabled = uiState is UiState.Success && apiState !is ApiState.Error
                    )
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "",
                        modifier = Modifier.size(24.dp).rotate(rotateAnim),
                        tint = if(uiState is UiState.Success && apiState !is ApiState.Error) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(0.5f)
                    )
                }

                Box(contentAlignment = Alignment.Center){
                    Button(
                        onClick = {
                            sendPrompt(prompt,true)
                            prompt = ""
                            focusManager.clearFocus()
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp),
                        enabled = apiState is ApiState.Success || apiState is ApiState.Initial && prompt.isNotBlank() && uiState is UiState.Success,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.5f)
                        ),
                        content = {}
                    )
                    if(apiState is ApiState.Loading){
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onBackground.copy(0.5f),
                            strokeWidth = 2.dp
                        )
                    }else {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = "",
                            modifier = Modifier.size(24.dp),
                            tint = if (apiState is ApiState.Success || apiState is ApiState.Initial && prompt.isNotBlank() && uiState is UiState.Success) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(0.5f)
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = isMore,
                enter = slideInVertically(tween(300),{it}),
                exit = slideOutVertically(tween(300),{it})
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) {
                        val text = when(it){
                            0 -> cnt[2]
                            1 -> cnt[3]
                            else -> cnt[4]
                        }
                        val icon= when(it){
                            0 -> Icons.Outlined.CameraAlt
                            1 -> Icons.Outlined.Image
                            else -> Icons.Outlined.FileOpen
                        }
                        Card(
                            onClick = {
                                when(it){
                                    0 -> {}
                                    1 -> {}
                                    else -> {}
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ){
                                Icon(
                                    imageVector = icon,
                                    contentDescription = text,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text,
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}