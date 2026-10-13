package com.kunalchauhan.safetrail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kunalchauhan.safetrail.data.AppDatabase
import com.kunalchauhan.safetrail.data.Contact
import kotlinx.coroutines.launch

const val MAX_CONTACTS = 5

@Composable
fun ContactsScreen() {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).contactDao() }
    val contacts by dao.getAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Trusted contacts", style = MaterialTheme.typography.headlineSmall)
            Text(
                "${contacts.size} of $MAX_CONTACTS added",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            if (contacts.isEmpty()) {
                Text("No contacts yet. Add the people who should be alerted if you don't check in.")
            } else {
                LazyColumn {
                    items(contacts, key = { it.id }) { c ->
                        ListItem(
                            headlineContent = { Text(c.name) },
                            supportingContent = { Text(c.phone) },
                            trailingContent = {
                                IconButton(onClick = { scope.launch { dao.delete(c) } }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete ${c.name}")
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
        if (contacts.size < MAX_CONTACTS) {
            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add contact")
            }
        }
    }

    if (showDialog) {
        AddContactDialog(
            onDismiss = { showDialog = false },
            onSave = { name, phone ->
                scope.launch { dao.insert(Contact(name = name, phone = phone)) }
                showDialog = false
            }
        )
    }
}

@Composable
fun AddContactDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val valid = name.isNotBlank() && phone.count { it.isDigit() } >= 10

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add trusted contact") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), phone.trim()) }, enabled = valid) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}