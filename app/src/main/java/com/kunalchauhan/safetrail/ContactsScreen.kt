package com.kunalchauhan.safetrail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            Text("Trusted contacts", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "${contacts.size} of $MAX_CONTACTS added",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { contacts.size / MAX_CONTACTS.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            if (contacts.isEmpty()) {
                EmptyContacts()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(contacts, key = { it.id }) { c ->
                        ContactCard(c, onDelete = { scope.launch { dao.delete(c) } })
                    }
                }
            }
        }
        if (contacts.size < MAX_CONTACTS) {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
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
private fun ContactCard(contact: Contact, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            ) {
                Text(
                    contact.name.trim().firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    contact.phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete ${contact.name}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmptyContacts() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("No contacts yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Add the people who should be alerted if you don't check in. Tap the + button to start.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
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
