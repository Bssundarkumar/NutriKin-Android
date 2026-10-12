package com.nutrikin.app.ui.screens

import androidx.compose.foundation.clickable
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
import com.nutrikin.app.data.MemberDto
import com.nutrikin.core.Condition

/**
 * The proof-of-concept for "same backend, both platforms": whatever members exist in this household's
 * Supabase row - whether they were added from the iPhone app or here - show up here, live. Mirrors the
 * iOS onboarding (Create family / Join with invite code) for a brand-new account, then the full
 * MemberEditScreen (not just a name) once a household exists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(viewModel: FamilyViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val isWorking by viewModel.isWorking.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    // null = not editing; MemberDto? inside the pair-less Optional-of-Optional problem is avoided by a
    // tiny wrapper: "editing" holds Some(null) for "adding new" and Some(member) for "editing existing".
    var editing by remember { mutableStateOf<EditTarget?>(null) }

    val loaded = state as? FamilyUiState.Loaded

    Scaffold(
        topBar = { TopAppBar(title = { Text("Family") }) },
        floatingActionButton = {
            if (loaded != null) {
                FloatingActionButton(onClick = { editing = EditTarget(null) }) {
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
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (s.inviteCode.isNotBlank()) {
                            Text(
                                "Invite code: ${s.inviteCode}",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (s.members.isEmpty()) {
                            Text("No family members yet.", modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 32.dp))
                        } else {
                            MembersList(s.members, Modifier.weight(1f)) { editing = EditTarget(it) }
                        }
                    }
                }
            }
        }
    }

    editing?.let { target ->
        MemberEditScreen(
            existing = target.member,
            householdId = loaded?.householdId ?: return@let,
            onDone = { editing = null; viewModel.load() },
        )
    }
}

private data class EditTarget(val member: MemberDto?)

@Composable
private fun MembersList(members: List<MemberDto>, modifier: Modifier = Modifier, onSelect: (MemberDto) -> Unit) {
    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        items(members) { member ->
            ListItem(
                modifier = Modifier.clickable { onSelect(member) },
                headlineContent = { Text(member.name) },
                supportingContent = {
                    val details = listOfNotNull(member.sex, member.age?.let { "$it yrs" }).joinToString(" · ")
                    val conditions = member.decodedConditions.joinToString(", ") { conditionLabel(it) }
                    Text(listOf(details, conditions).filter { it.isNotBlank() }.joinToString(" — "))
                },
            )
            HorizontalDivider()
        }
    }
}

private fun conditionLabel(c: Condition): String = when (c) {
    Condition.Diabetes -> "Diabetes"
    Condition.Hypertension -> "High blood pressure"
    Condition.HighCholesterol -> "High cholesterol"
    is Condition.Allergy -> "${c.allergen.displayName} allergy"
    is Condition.CustomAllergy -> "${c.name} allergy"
    is Condition.Custom -> c.name
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
