package com.reshma.vidyarthibus

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun BottomNavBar() {
    NavigationBar {

        NavigationBarItem(
            selected = true,
            onClick = { },
            icon = {
                Icon(Icons.Default.Home, contentDescription = null)
            },
            label = { Text("Home") }
        )

        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = {
                Icon(Icons.Default.LocationOn, contentDescription = null)
            },
            label = { Text("Map") }
        )

        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = {
                Icon(Icons.Default.Person, contentDescription = null)
            },
            label = { Text("Profile") }
        )

        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = {
                Icon(Icons.Default.Settings, contentDescription = null)
            },
            label = { Text("Settings") }
        )
    }
}