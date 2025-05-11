package com.app.promptai

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.promptai.presentation.theme.PromptAiTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalDrawerSample() {
    // 1. Создаём состояние выдвижного меню (drawer)
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 2. Обёртка ModalNavigationDrawer
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // 3. Содержимое меню
            ModalDrawerSheet(
                Modifier
                    .width(240.dp)
                    .fillMaxHeight()
            ) {
                Spacer(Modifier.height(16.dp))
                Text("Меню", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Чаты") },
                    selected = false,
                    onClick = { /* перейти в раздел "Чаты" */ },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Настройки") },
                    selected = false,
                    onClick = { /* перейти в раздел "Настройки" */ },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        // 4. Основной контент с TopAppBar
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Главный экран") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Открыть меню")
                        }
                    }
                )
            }
        ) { innerPadding ->
            // 5. Остальная разметка экрана
            Column(
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Здесь основной контент экрана")
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    scope.launch {
                        if (drawerState.isClosed) drawerState.open()
                        else drawerState.close()
                    }
                }) {
                    Text("Переключить меню")
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun Djfhlkak(){
    PromptAiTheme {
        ModalDrawerSample()
    }
}