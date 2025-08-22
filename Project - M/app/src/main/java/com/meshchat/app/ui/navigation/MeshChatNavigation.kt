package com.meshchat.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.meshchat.app.core.data.PeerDevice
import com.meshchat.app.ui.*

sealed class Screen(val route: String, val title: String, val icon: @Composable () -> Unit) {
    object Home : Screen("home", "Home", { Icon(Icons.Default.Home, contentDescription = null) })
    object Peers : Screen("peers", "Peers", { Icon(Icons.Default.People, contentDescription = null) })
    object Chat : Screen("chat/{peerAddress}", "Chat", { Icon(Icons.Default.Chat, contentDescription = null) })
    object Diagnostics : Screen("diagnostics", "Diagnostics", { Icon(Icons.Default.Analytics, contentDescription = null) })
    object Settings : Screen("settings", "Settings", { Icon(Icons.Default.Settings, contentDescription = null) })
    object QR : Screen("qr", "Identity", { Icon(Icons.Default.QrCode, contentDescription = null) })
    
    fun createRoute(vararg args: String): String {
        return when (this) {
            is Chat -> route.replace("{peerAddress}", args.firstOrNull() ?: "")
            else -> route
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshChatNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(
                    Screen.Home,
                    Screen.Peers,
                    Screen.Diagnostics,
                    Screen.Settings,
                    Screen.QR
                ).forEach { screen ->
                    NavigationBarItem(
                        icon = screen.icon,
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route || 
                                 (screen is Screen.Chat && currentRoute?.startsWith("chat/") == true),
                        onClick = {
                            if (screen !is Screen.Chat) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToPeers = { navController.navigate(Screen.Peers.route) }
                )
            }
            
            composable(Screen.Peers.route) {
                PeersScreen(
                    onOpenTestChat = { peerDevice ->
                        navController.navigate(Screen.Chat.createRoute(peerDevice.address))
                    }
                )
            }
            
            composable(Screen.Chat.route) { backStackEntry ->
                val peerAddress = backStackEntry.arguments?.getString("peerAddress")
                if (peerAddress != null) {
                    // Create a mock PeerDevice for now - in a real app, you'd get this from a repository
                    val mockPeer = PeerDevice(
                        address = peerAddress,
                        addressHash = peerAddress.hashCode().toString(),
                        ephemeralId = ByteArray(8),
                        lastSeen = System.currentTimeMillis(),
                        rssi = -50,
                        isConnected = false
                    )
                    ChatScreen(
                        peer = mockPeer,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
            
            composable(Screen.Diagnostics.route) {
                DiagnosticsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.QR.route) {
                QRScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
