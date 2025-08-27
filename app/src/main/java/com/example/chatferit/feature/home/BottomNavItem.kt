package com.example.chatferit.feature.home // Or feature.home.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Chats : BottomNavItem("chats", Icons.Filled.Message, "Chats")
    object Groups : BottomNavItem("groups", Icons.Filled.Group, "Groups")
    object Friends : BottomNavItem("friends", Icons.Filled.People, "Friends")
    object Settings : BottomNavItem("settings", Icons.Filled.Settings, "Settings")
}
val bottomNavItemsList = listOf(
    BottomNavItem.Chats,
    BottomNavItem.Groups,
    BottomNavItem.Friends,
    BottomNavItem.Settings
)