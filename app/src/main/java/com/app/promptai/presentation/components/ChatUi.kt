@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)

package com.app.promptai.presentation.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.app.promptai.R
import com.app.promptai.presentation.ChatViewModel
import com.app.promptai.utils.ApiState
import com.app.promptai.utils.UiState
import com.app.promptai.utils.createFileProviderTempUri
import com.app.promptai.utils.deleteTempFile
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
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
                uiState = uiState
            )
        },
        bottomBar = {
            TypingChatBar(
                text = prompt,
                sendPrompt = viewModel::getResponse,
                apiState = apiState,
                isMore = viewModel.isMore,
                uiState = uiState,
                onMore = viewModel::switchIsMore,
                isEdit = viewModel.isEdit,
                editMessage = viewModel::editMessage,
                previousMsg = if(messages.isNotEmpty())viewModel.messages.collectAsState().value[viewModel.editingMessageId].content else "",
                isOpen = viewModel.isOpen,
                switchIsEdit = viewModel::switchIsEdit,
                isWebSearch = viewModel.isWebSearch,
                switchIsWebSearch = viewModel::switchIsWebSearch,
                switchIsMore = viewModel::switchIsMore,
                picList = viewModel.picList,
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

@Composable
fun TypingChatBar(
    text: String,
    sendPrompt: (String, List<Uri>, Context) -> Unit,
    apiState: ApiState,
    isMore: Boolean,
    uiState: UiState,
    onMore: () -> Unit,
    isEdit: Boolean,
    editMessage: (String, List<Uri>, Context) -> Unit,
    previousMsg: String,
    isOpen: Boolean,
    switchIsEdit: () -> Unit,
    isWebSearch: Boolean,
    switchIsWebSearch: () -> Unit,
    switchIsMore: () -> Unit,
    picList: MutableList<Uri>,
){
    var message by remember { mutableStateOf(TextFieldValue(text)) }
    val context = LocalContext.current
    val cnt = stringArrayResource(R.array.chatUi_cnt)
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val rotateAnim by animateFloatAsState(
        targetValue = if(isMore) 45f else 0f,
        animationSpec = tween(300,50),
    )
    val colorAnim by animateColorAsState(
        targetValue = if(isMore) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary,
        animationSpec = tween(300,50)
    )

    var pictureUri: Uri? by remember { mutableStateOf(null) }
    val cameraPermissionState = rememberPermissionState(permission = android.Manifest.permission.CAMERA)
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
        if(it){ pictureUri?.let { picList.add(it) } }
    }

    LaunchedEffect(isEdit) {
        if(isEdit == true) {
            focusRequester.requestFocus()
            message = message.copy(text = previousMsg,selection = TextRange(previousMsg.length))
        }else{
            message = TextFieldValue("")
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(isOpen) {
        focusManager.clearFocus()
    }

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
            AnimatedVisibility(
                visible = isEdit,
                enter = fadeIn(tween(300))+slideInVertically(tween(400)) { it },
                exit = fadeOut(tween(300))+slideOutVertically(tween(400)) { it }
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Draw,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(24.dp))
                            Text(
                                text = cnt[5],
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        IconButton(
                            onClick = switchIsEdit,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(picList.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ){
                    itemsIndexed(picList) { ind, it ->
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ){
                            AsyncImage(
                                model = it,
                                contentDescription = null,
                                modifier = Modifier.clip(RoundedCornerShape(12.dp)).size(100.dp),
                                contentScale = ContentScale.Crop
                            )
                            Button(
                                onClick = {
                                    deleteTempFile(context,picList[ind])
                                    picList.removeAt(ind)
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.background.copy(0.75f),
                                    contentColor = MaterialTheme.colorScheme.onBackground
                                ),
                                modifier = Modifier.padding(2.dp).align(Alignment.TopEnd).size(26.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
            TextField(
                value = message,
                onValueChange = {message = it},
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                maxLines = 6,
                placeholder = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = if(isEdit) cnt[5] else cnt[1],
                            color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(20.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    Button(
                        onClick = onMore,
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                            disabledContentColor = MaterialTheme.colorScheme.onBackground.copy(0.5f),
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(0.5f)
                        ),
                        border = BorderStroke(1.dp, if(!isWebSearch) colorAnim else MaterialTheme.colorScheme.primary.copy(0.5f)),
                        contentPadding = PaddingValues(0.dp),
                        enabled = !isWebSearch
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "",
                            modifier = Modifier.size(20.dp).rotate(rotateAnim)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if(isMore){switchIsMore()}
                            switchIsWebSearch()
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp),
                        border = BorderStroke(1.dp, if(isWebSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if(isWebSearch) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = if(isWebSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        ),
                        enabled = picList.isEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = "",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        if(isEdit){
                            editMessage(message.text,picList.toList(),context)
                        }else {
                            sendPrompt(message.text,picList.toList(),context)
                        }
                        picList.clear()
                        message = TextFieldValue("")
                        focusManager.clearFocus()
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp),
                    enabled = apiState !is ApiState.Error && message.text.isNotBlank() && uiState is UiState.Success,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.5f),
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        disabledContentColor = MaterialTheme.colorScheme.onBackground.copy(0.5f)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
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
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

            }
            AnimatedVisibility(
                visible = isMore,
                enter = slideInVertically(tween(300)) { it },
                exit = slideOutVertically(tween(300)) { it }
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
                                    0 -> {
                                        if(cameraPermissionState.status.isGranted){
                                            pictureUri = createFileProviderTempUri(context)
                                            if(pictureUri != null) {
                                                pictureUri?.let { takePicture.launch(it) }
                                            }
                                        }else{
                                            cameraPermissionState.launchPermissionRequest()
                                        }
                                    }
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