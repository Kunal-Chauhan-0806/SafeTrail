package com.kunalchauhan.safetrail

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", Icons.Filled.Home),
    Journey("journey", "Journey", Icons.Filled.Place),
    Contacts("contacts", "Contacts", Icons.Filled.Person)
}

@Composable
fun SafeTrailApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                Dest.entries.forEach { d ->
                    NavigationBarItem(
                        selected = current == d.route,
                        onClick = {
                            navController.navigate(d.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(d.icon, contentDescription = d.label) },
                        label = { Text(d.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Dest.Home.route) { HomeScreen() }
            composable(Dest.Journey.route) { PlaceholderScreen("Journey check-in") }
            composable(Dest.Contacts.route) { ContactsScreen() }
        }
    }
}

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("SafeTrail", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("Travel safe. We'll check in if you can't.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = { /* SOS logic comes in a later module */ },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.size(180.dp)
        ) {
            Text("SOS", style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}