package com.mydreamlinks.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mydreamlinks.app.data.Api
import com.mydreamlinks.app.data.ApiException
import com.mydreamlinks.app.data.BlockedPerson
import com.mydreamlinks.app.ui.components.Load
import com.mydreamlinks.app.ui.components.rememberLoader
import com.mydreamlinks.app.ui.theme.Ink
import kotlinx.coroutines.launch

/** The same reasons the website offers (ReportModel::REASONS). */
private val REPORT_REASONS = listOf(
    "harassment" to "Harassment or hate",
    "sexual" to "Sexual content",
    "violence" to "Violence or threats",
    "self_harm" to "Self-harm",
    "spam" to "Spam",
    "other" to "Something else",
)

/** Report a dream ([dreamId]) or a dreamer ([userId]). [onSent] runs once the report is filed. */
@Composable
fun ReportDialog(api: Api, what: String, dreamId: Int? = null, userId: Int? = null, onDismiss: () -> Unit, onSent: () -> Unit) {
    var reason by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Report $what") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Reports are private. They won't know it was you.", style = MaterialTheme.typography.bodySmall)
                REPORT_REASONS.forEach { (key, label) ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = reason == key, onClick = { reason = key }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(reason == key, onClick = { reason = key }, colors = RadioButtonDefaults.colors(selectedColor = Ink.pink))
                        Text(label)
                    }
                }
                OutlinedTextField(note, { note = it.take(500) }, label = { Text("Anything else? (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = Ink.danger, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = {
            TextButton(enabled = reason != null && !busy, onClick = {
                busy = true; error = null
                scope.launch {
                    try { api.report(reason!!, note.trim(), dreamId, userId); onSent() } catch (e: ApiException) { error = e.message } finally { busy = false }
                }
            }) { Text(if (busy) "Sending…" else "Send report", color = Ink.danger) }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel", color = Ink.ink) } },
        containerColor = Ink.card,
    )
}

/** Asks before blocking [name]. [onBlocked] runs once it's done. */
@Composable
fun BlockDialog(api: Api, userId: Int, name: String, onDismiss: () -> Unit, onBlocked: () -> Unit) {
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Block $name?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("You won't see each other's dreams or profiles, and any link between you ends. You can unblock them from Links.")
                error?.let { Text(it, color = Ink.danger) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch { try { api.block(userId); onBlocked() } catch (e: ApiException) { error = e.message } }
            }) { Text("Block", color = Ink.danger) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Ink.ink) } },
        containerColor = Ink.card,
    )
}

/** The Blocked section on the Links page. Shows nothing when nobody is blocked. */
@Composable
fun BlockedList(api: Api) {
    val loader = rememberLoader { api.blocks() }
    val scope = rememberCoroutineScope()
    val blocked: List<BlockedPerson> = (loader.state as? Load.Ready)?.value?.blocked.orEmpty()
    if (blocked.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Blocked", style = MaterialTheme.typography.headlineSmall)
        blocked.forEach { b ->
            PersonRow(b.username, b.avatar, onOpen = {}) {
                TextButton(onClick = {
                    scope.launch { runCatching { api.unblock(b.blockId) }.onSuccess { r -> loader.update { r } } }
                }) { Text("Unblock", color = Ink.muted) }
            }
        }
    }
}
