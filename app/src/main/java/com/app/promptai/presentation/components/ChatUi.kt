@file:OptIn(ExperimentalMaterial3Api::class)

package com.app.promptai.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.app.promptai.R

@Composable
fun BaseChatScreen(
    content: @Composable ((PaddingValues) -> Unit)
){
    Scaffold(
        topBar = {
        },
        bottomBar = {
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
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ){
                Text(
                    cnt[0],
                    style = MaterialTheme.typography.titleLarge
                )
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.new_chat),
                    contentDescription = "new chat",
                    modifier = Modifier.size(36.dp).clickable{onNew()}
                )
            }
        },
        navigationIcon = {
            Icon(
                imageVector = Icons.Rounded.Menu,
                contentDescription = "Menu",
                modifier = Modifier
                    .size(36.dp)
                    .padding(16.dp)
                    .clickable {onMenu()}
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.surfaceContainer)
    )
}

@Composable
fun TypingChatBar(
    text: String
){
    var prompt by rememberSaveable { mutableStateOf(text) }
    val cnt = stringArrayResource(R.array.chatUi_cnt)
    val focusManager = LocalFocusManager.current
    val visible by keyboardAsState()
    LaunchedEffect(!visible) {
        if(!visible){
            focusManager.clearFocus()
        }
    }
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
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
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(

                ){

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