package com.nutrikin.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nutrikin.app.data.MemberDto
import com.nutrikin.core.Allergen
import com.nutrikin.core.Sex

/**
 * Add or edit a family member - mirrors MemberEditView.swift's "Profile & care" and "Body measurements"
 * sections, plus the diabetes/hypertension/high-cholesterol toggles and built-in allergy checkboxes.
 * See MemberEditViewModel's doc comment for what's deliberately out of scope for this first port.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberEditScreen(existing: MemberDto?, householdId: String, onDone: () -> Unit) {
    val viewModel = remember { MemberEditViewModel(existing, householdId) }
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()
    val saved by viewModel.saved.collectAsState()

    LaunchedEffect(saved) { if (saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Add member" else "Edit member") },
                navigationIcon = { TextButton(onClick = onDone) { Text("Cancel") } },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = !isSaving && viewModel.name.isNotBlank()) {
                        Text(if (isSaving) "Saving…" else "Save")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = viewModel.name, onValueChange = { viewModel.name = it },
                label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = viewModel.age, onValueChange = { viewModel.age = it.filter(Char::isDigit) },
                    label = { Text("Age") }, singleLine = true, modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = viewModel.heightCm, onValueChange = { viewModel.heightCm = it },
                    label = { Text("Height (cm)") }, singleLine = true, modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = viewModel.weightKg, onValueChange = { viewModel.weightKg = it },
                    label = { Text("Weight (kg)") }, singleLine = true, modifier = Modifier.weight(1f),
                )
            }

            Text("Sex", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = viewModel.sex == null, onClick = { viewModel.sex = null }, label = { Text("Not set") })
                Sex.entries.forEach { s ->
                    FilterChip(
                        selected = viewModel.sex == s, onClick = { viewModel.sex = s },
                        label = { Text(s.name.lowercase().replaceFirstChar(Char::uppercase)) },
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = viewModel.isManagedByParent, onCheckedChange = { viewModel.isManagedByParent = it })
                Text("Managed by a parent or carer")
            }

            HorizontalDivider()
            Text("Conditions", style = MaterialTheme.typography.titleMedium)
            ConditionRow("Diabetes", viewModel.hasDiabetes) { viewModel.hasDiabetes = it }
            ConditionRow("High blood pressure", viewModel.hasHypertension) { viewModel.hasHypertension = it }
            ConditionRow("High cholesterol", viewModel.hasHighCholesterol) { viewModel.hasHighCholesterol = it }

            HorizontalDivider()
            Text("Allergies", style = MaterialTheme.typography.titleMedium)
            Allergen.entries.forEach { a ->
                ConditionRow(a.displayName, a in viewModel.allergies) { viewModel.toggleAllergy(a) }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ConditionRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}
