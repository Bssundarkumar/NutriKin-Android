package com.nutrikin.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nutrikin.core.MemberScore
import com.nutrikin.core.Verdict

/**
 * Mirrors ScanView.swift's barcode path: type/paste a barcode, look it up, see every family member's
 * verdict. Camera scanning (CameraX + ML Kit) and plate-photo estimation aren't ported yet - see the
 * README; this is the core "scan a product, get family scores" loop, the thing the whole app is for.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(viewModel: ScanViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var barcode by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Scan") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = barcode, onValueChange = { barcode = it.filter(Char::isDigit) },
                    label = { Text("Barcode") }, singleLine = true, modifier = Modifier.weight(1f),
                )
                Button(onClick = { viewModel.lookUp(barcode) }, enabled = barcode.isNotBlank() && state !is ScanUiState.Loading) {
                    Text("Look up")
                }
            }
            Spacer(Modifier.height(16.dp))

            when (val s = state) {
                is ScanUiState.Idle -> Text(
                    "Type a barcode to see how it scores for your family.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is ScanUiState.Loading -> Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is ScanUiState.Failed -> Text(s.message, color = MaterialTheme.colorScheme.error)
                is ScanUiState.Result -> ScanResult(s)
            }
        }
    }
}

@Composable
private fun ScanResult(result: ScanUiState.Result) {
    Column {
        Text(result.product.name, style = MaterialTheme.typography.headlineSmall)
        result.product.brand?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.height(16.dp))

        if (!result.product.hasAnyNutrition) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    "Some information is missing for this product - scores can't be fully trusted without it.",
                    modifier = Modifier.padding(12.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        Text("Who can eat this", style = MaterialTheme.typography.titleMedium)
        LazyColumn {
            items(result.scores) { score -> MemberScoreRow(score) }
        }
    }
}

@Composable
private fun MemberScoreRow(score: MemberScore) {
    val color = when (score.verdict) {
        Verdict.OKAY -> Color(0xFF2E7D32)
        Verdict.CAUTION -> Color(0xFFEF6C00)
        Verdict.AVOID -> Color(0xFFC62828)
    }
    ListItem(
        headlineContent = { Text(score.member.name) },
        supportingContent = { Text(score.reasons.firstOrNull() ?: "") },
        trailingContent = {
            Text("${score.score}", color = color, style = MaterialTheme.typography.titleLarge)
        },
    )
    HorizontalDivider()
}
