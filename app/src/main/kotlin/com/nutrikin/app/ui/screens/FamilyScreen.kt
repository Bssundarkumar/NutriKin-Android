package com.nutrikin.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * The proof-of-concept for "same backend, both platforms": whatever members exist in this household's
 * Supabase row - whether they were added from the iPhone app or here - show up here, live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(viewModel: FamilyViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Family") }) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is FamilyUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is FamilyUiState.Failed -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(s.message)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = viewModel::load) { Text("Try again") }
                }
                is FamilyUiState.Loaded -> if (s.members.isEmpty()) {
                    Text("No family members yet.", modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        items(s.members) { member ->
                            ListItem(
                                headlineContent = { Text(member.name) },
                                supportingContent = {
                                    Text(listOfNotNull(member.sex, member.age?.let { "$it yrs" }).joinToString(" · "))
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
