package com.nutrikin.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * The proof-of-concept for "same backend, both platforms": whatever members exist in this household's
 * Supabase row - whether they were added from the iPhone app or here - show up here, live. Mirrors the
 * iOS onboarding (Create family / Join with invite code) for a brand-new account, then a plain add-member
 * flow once a household exists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(viewModel: FamilyViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val isWorking by viewModel.isWorking.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    var showAddMember by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Family") }) },
        floatingActionButton = {
            if (state is FamilyUiState.Loaded) {
                FloatingActionButton(onClick = { showAddMember = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add family member")
                }
            }
        },
    ) { padding ->
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

                is FamilyUiState.NeedsHousehold -> NeedsHouseholdContent(
                    isWorking = isWorking,
                    error = actionError,
                    onCreate = viewModel::createHousehold,
                    onJoin = viewModel::joinHousehold,
                )

                is FamilyUiState.Loaded -> {
                    if (s.inviteCode.isNotBlank()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                "Invite code: ${s.inviteCode}",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            MembersList(s.members, Modifier.weight(1f))
                        }
                    } else if (s.members.isEmpty()) {
                        Text("No family members yet.", modifier = Modifier.align(Alignment.Center))
                    } else {
                        MembersList(s.members, Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    if (showAddMember) {
        AddMemberDialog(
            isWorking = isWorking,
            error = actionError,
            onDismiss = { showAddMember = false },
            onAdd = { name ->
                viewModel.addMember(name)
                showAddMember = false
            },
        )
    }
}

@Composable
private fun MembersList(members: List<com.nutrikin.app.data.MemberDto>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        items(members) { member ->
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

@Composable
private fun NeedsHouseholdContent(
    isWorking: Boolean,
    error: String?,
    onCreate: (String) -> Unit,
    onJoin: (String) -> Unit,
) {
    var familyName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Start or join a family", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = familyName, onValueChange = { familyName = it },
            label = { Text("Family name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onCreate(familyName) },
            enabled = !isWorking && familyName.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Create family") }

        Spacer(Modifier.height(24.dp))
        Text("— or —", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = inviteCode, onValueChange = { inviteCode = it },
            label = { Text("Invite code") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onJoin(inviteCode) },
            enabled = !isWorking && inviteCode.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Join with code") }

        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun AddMemberDialog(isWorking: Boolean, error: String?, onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add family member") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") }, singleLine = true,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name) }, enabled = !isWorking && name.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
