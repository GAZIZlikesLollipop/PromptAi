@file:OptIn(ExperimentalMaterial3Api::class)

package com.app.promptai.presentation.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.promptai.R
import com.app.promptai.data.UiState
import com.app.promptai.presentation.ChatViewModel

@Composable
fun BaseChatScreen(
    viewModel: ChatViewModel,
    onMenu: () -> Unit,
    onNew: () -> Unit,
    chatId: Long,
    content: @Composable ((PaddingValues) -> Unit),
){
    val prompt by viewModel.userPrompt.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopChatBar(
                {onMenu()},{onNew()}
            )
        },
        bottomBar = {
            TypingChatBar(
                prompt,
                {viewModel.sendPrompt(null,it,chatId)},
                uiState
            )
        },
        modifier = Modifier.fillMaxSize()
    ){
        content(it)
    }
}

@Composable
fun TopChatBar(
    onMenu: () -> Unit,
    onNew: () -> Unit
){
    val cnt = stringArrayResource(R.array.chatUi_cnt)
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ){
                Row {
                Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = "Menu",
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onMenu() }
                    )
                }
                Text(
                    cnt[0],
                    style = MaterialTheme.typography.titleLarge
                )
                Row {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.new_chat),
                        contentDescription = "new chat",
                        modifier = Modifier.size(36.dp).clickable { onNew() }
                    )
                Spacer(Modifier.width(16.dp))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.surfaceContainer)
    )
}

@Composable
fun TypingChatBar(
    text: String,
    sendPrompt: (String) -> Unit,
    uiState: UiState,
){
    var prompt by rememberSaveable { mutableStateOf(text) }
    val cnt = stringArrayResource(R.array.chatUi_cnt)
    val focusManager = LocalFocusManager.current
    val visible by keyboardAsState()
    LaunchedEffect(visible) {
        if(!visible){
            focusManager.clearFocus()
        }
    }
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).height(150.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(300.dp)
        ) {
            TextField(
                value = prompt,
                onValueChange = {prompt = it},
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        cnt[1],
                        color = MaterialTheme.colorScheme.onBackground.copy(0.75f)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ){
                    Button(
                        onClick = {
                            sendPrompt(prompt)
                            prompt = ""
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(50.dp),
                        enabled = (uiState is UiState.Initial ||uiState is UiState.Success || uiState is UiState.Error) && prompt.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.5f)
                        ),
                        content = {}
                    )
                    if(uiState is UiState.Loading){
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.onBackground.copy(0.5f)
                        )
                    }else {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = "",
                            modifier = Modifier.size(36.dp),
                            tint = if ((uiState is UiState.Initial || uiState is UiState.Success || uiState is UiState.Error) && prompt.isNotBlank()) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun keyboardAsState(): State<Boolean> {
    val keyboardState = remember { mutableStateOf(false) }
    val view = LocalView.current

    DisposableEffect(view) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            keyboardState.value = insets.isVisible(WindowInsetsCompat.Type.ime())
            insets
        }
        onDispose {
            ViewCompat.setOnApplyWindowInsetsListener(view, null)
        }
    }

    return keyboardState
}