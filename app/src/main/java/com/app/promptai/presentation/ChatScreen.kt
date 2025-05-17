package com.app.promptai.presentation

import android.content.ClipData
import android.widget.Toast
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.vectorResource
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
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current
    val coroutine = rememberCoroutineScope()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val apiState by chatViewModel.apiState.collectAsState()
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
            when {
                uiState is UiState.Success && messages.isNotEmpty()-> {
                    LaunchedEffect(apiState is ApiState.Success || apiState is ApiState.Error) {
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
                            when {
                                msg.senderType == SenderType.AI -> {
                                    if(msg.content.isNotBlank()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.Start
                                        ) {
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
                                                    shadowElevation = 12.dp
                                                ) {
                                                    ChatText(msg.content)
                                                }
                                            }
                                            Row(
                                                modifier = Modifier.offset(x = 12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                repeat(3) {
                                                    val text = when (it) {
                                                        0 -> cnt[6]
                                                        1 -> cnt[10]
                                                        else -> cnt[8]
                                                    }
                                                    val icon = when (it) {
                                                        0 -> Icons.Outlined.ContentCopy
                                                        1 -> Icons.Outlined.Cached
                                                        else -> ImageVector.vectorResource(R.drawable.text_select)
                                                    }
                                                    Surface(
                                                        onClick = {
                                                            when (it) {
                                                                0 -> {
                                                                    coroutine.launch {
                                                                        clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("", msg.content)))
                                                                        Toast.makeText(
                                                                            context,
                                                                            cnt[9],
                                                                            Toast.LENGTH_SHORT
                                                                        ).show()
                                                                    }
                                                                }

                                                                1 -> {

                                                                }

                                                                else -> {

                                                                }
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
                                                                tint = MaterialTheme.colorScheme.onBackground.copy(
                                                                    0.35f
                                                                ),
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                            Text(
                                                                text = text,
                                                                style = MaterialTheme.typography.labelLarge,
                                                                color = MaterialTheme.colorScheme.onBackground.copy(
                                                                    0.35f
                                                                )
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
                                apiState is ApiState.Error -> {
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
                                            border = BorderStroke(
                                                2.dp,
                                                MaterialTheme.colorScheme.onError
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Text(
                                                    cnt[12],
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = MaterialTheme.colorScheme.onError
                                                )
                                                Button(
                                                    onClick = {},
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                                        contentColor = MaterialTheme.colorScheme.error
                                                    ),
                                                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onError)
                                                ) {
                                                    Text(
                                                        cnt[13]
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(
                                                bottomEnd = 24.dp,
                                                bottomStart = 24.dp,
                                                topEnd = 3.dp,
                                                topStart = 24.dp,
                                            ),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.padding(16.dp),
                                            contentColor = MaterialTheme.colorScheme.onBackground,
                                            tonalElevation = 12.dp,
                                            shadowElevation = 12.dp
                                        ) {
                                            ChatText(msg.content)
                                        }
                                        Row(
                                            modifier = Modifier.offset(x = (-12).dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            repeat(3) {
                                                val text = when(it){
                                                    0 -> cnt[6]
                                                    1 -> cnt[7]
                                                    else -> cnt[8]
                                                }
                                                val icon= when(it){
                                                    0 -> Icons.Outlined.ContentCopy
                                                    1 -> Icons.Outlined.Edit
                                                    else -> ImageVector.vectorResource(R.drawable.text_select)
                                                }
                                                Surface(
                                                    onClick = {
                                                        when (it) {
                                                            0 -> {
                                                                coroutine.launch {
                                                                    clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("", msg.content)))
                                                                    Toast.makeText(
                                                                        context,
                                                                        cnt[9],
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            }
                                                            1 -> {

                                                            }
                                                            else -> {

                                                            }
                                                        }
                                                    },
                                                ){
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
//        style = MaterialTheme.typography.titleLarge,
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