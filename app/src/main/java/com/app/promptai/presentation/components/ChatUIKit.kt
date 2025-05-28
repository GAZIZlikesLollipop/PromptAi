package com.app.promptai.presentation.components

import android.content.ClipData
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.twotone.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.core.net.toFile
import coil3.compose.AsyncImage
import com.app.promptai.R
import com.app.promptai.data.database.ChatEntity
import com.app.promptai.data.database.MessageEntity
import com.app.promptai.data.database.SenderType
import com.app.promptai.presentation.ChatViewModel
import com.app.promptai.utils.ApiState
import com.app.promptai.utils.bytesToString
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


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
    )
}

@Composable
fun ChatCard(
    isMenu: Boolean,
    chat: ChatEntity,
    drawerState: DrawerState,
    onLongPress: (Offset) -> Unit,
    onTap: (Offset) -> Unit,
    onRename: () -> Unit,
    switchIsFavorite: () -> Unit,
    onDelete: () -> Unit,
    menuFalse: () -> Unit
){

    val time = Instant.ofEpochMilli(chat.creationTimestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
    var pressOffset by remember { mutableStateOf(Offset.Zero) }
    val poopCnt = stringArrayResource(R.array.popup_cnt)
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress(it) },
                    onTap = onTap
                )
            }
    ) {

        Column(Modifier.heightIn(min = 56.0.dp).padding(16.dp)) {
            Text(chat.name)
            Text(
                formatter.format(time),
                color = MaterialTheme.colorScheme.onBackground.copy(0.5f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Column {
            if(isMenu && drawerState.isOpen) {
                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(
                        pressOffset.x.toInt(),
                        pressOffset.y.toInt()
                    ),
                    onDismissRequest = menuFalse
                ) {
                    AnimatedVisibility(
                        visible = isMenu && drawerState.isOpen,
                        enter = fadeIn(tween(300, 50)),
                        exit = fadeOut(tween(300, 50))
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Column(modifier = Modifier.width(150.dp).padding(8.dp)) {
                                repeat(3) {
                                    val icon =
                                        if (!chat.isFavorite) {
                                            when (it) {
                                                0 -> Icons.Default.Edit
                                                1 -> Icons.TwoTone.Star
                                                else -> Icons.Default.Delete
                                            }
                                        } else {
                                            when (it) {
                                                0 -> Icons.Default.Edit
                                                1 -> Icons.Outlined.Star
                                                else -> Icons.Default.Delete
                                            }
                                        }
                                    val text =
                                        if (!chat.isFavorite) {
                                            when (it) {
                                                0 -> poopCnt[0]
                                                1 -> poopCnt[2]
                                                else -> poopCnt[1]
                                            }
                                        } else {
                                            when (it) {
                                                0 -> poopCnt[0]
                                                1 -> poopCnt[3]
                                                else -> poopCnt[1]
                                            }
                                        }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                menuFalse()
                                                when (it) {
                                                    0 -> onRename()
                                                    1 -> switchIsFavorite()
                                                    else -> onDelete()
                                                }
                                            },
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = text,
                                            modifier = Modifier.size(24.dp),
                                            tint = if (it == 2) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = text,
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            color = if (it == 2) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.onBackground,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                    if (it != 2) {
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatCnt(
    message: String,
    senderType: SenderType,
    onRegenerate: () -> Unit = {},
    onEdit: () -> Unit = {},
    apiState: ApiState
){
    val coroutine = rememberCoroutineScope()
    val clipboardManager = LocalClipboard.current
    val cnt = stringArrayResource(R.array.chat_cnt)
    AnimatedContent(
        targetState = message.isNotEmpty() || senderType == SenderType.USER,
    ) { state ->
        when {
            !state && apiState is ApiState.Loading -> {
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
            !state && apiState is ApiState.Error -> {
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
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                cnt[9],
                                color = MaterialTheme.colorScheme.onError
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {onRegenerate()},
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(
                                    2.dp,
                                    MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text(cnt[10])
                            }
                        }
                    }
                }
            }
            else -> {
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

                            val text =
                                if (it == 0) cnt[6] else if (senderType == SenderType.USER) cnt[7] else cnt[8]
                            val icon =
                                if (it == 0) Icons.Outlined.ContentCopy else if (senderType == SenderType.USER) Icons.Outlined.Edit else Icons.Outlined.Cached
                            val enabledExp =
                                apiState is ApiState.Success || apiState is ApiState.Initial || it == 0

                            Surface(
                                onClick = {
                                    if (it == 0) {
                                        coroutine.launch {
                                            clipboardManager.setClipEntry(
                                                ClipEntry(
                                                    ClipData.newPlainText("", message)
                                                )
                                            )
                                        }
                                    } else {
                                        if (senderType == SenderType.AI) onRegenerate() else onEdit()
                                    }
                                },
                                enabled = enabledExp
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceAround,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = text,
                                        tint = if (!enabledExp) MaterialTheme.colorScheme.onBackground.copy(
                                            0.25f
                                        ) else MaterialTheme.colorScheme.onBackground.copy(0.4f),
                                        modifier = Modifier.size(if (it == 0) 32.dp else 36.dp)
                                    )
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (!enabledExp) MaterialTheme.colorScheme.onBackground.copy(
                                            0.25f
                                        ) else MaterialTheme.colorScheme.onBackground.copy(0.4f)
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

@Composable
fun ChatContent(
    msg: MessageEntity,
    chatViewModel: ChatViewModel,
    messages: List<MessageEntity>,
    ind: Int,
    apiState: ApiState
){
    if (msg.senderType == SenderType.USER) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            if (msg.pictures.isNotEmpty() || msg.files.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    items(msg.files) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(Color.Transparent),
                            modifier = Modifier.width(200.dp),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        it.toFile().name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    Text(
                                        bytesToString(it.toFile().length()),
                                        color = MaterialTheme.colorScheme.onBackground.copy(0.5f)
                                    )
                                }
                            }
                        }
                    }
                    items(msg.pictures) {
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .size(100.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            ChatCnt(
                message = msg.content,
                senderType = SenderType.USER,
                onEdit = {
                    chatViewModel.isEdit = true
                    chatViewModel.editingMessageId = ind
                },
                apiState = apiState
            )
        }
    } else {
        ChatCnt(
            message = msg.content,
            senderType = SenderType.AI,
            onRegenerate = { chatViewModel.regenerateResponse(userMsg = messages[ind - 1], aiMsg = messages[ind]) },
            apiState = apiState
        )
    }
}