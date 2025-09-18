package com.example.chatferit.feature.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Chats : BottomNavItem("chats", Icons.Filled.Message, "Chats")
    object Groups : BottomNavItem("groups", Icons.Filled.Group, "Groups")
    object Users : BottomNavItem("users", Icons.Filled.People, "Users")
    object Settings : BottomNavItem("settings", Icons.Filled.Settings, "Settings")
}
val bottomNavItemsList = listOf(
    BottomNavItem.Chats,
    BottomNavItem.Groups,
    BottomNavItem.Users,
    BottomNavItem.Settings
)