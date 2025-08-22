package com.meshchat.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meshchat.app.core.crypto.CryptoManager
import com.meshchat.app.core.crypto.FakeKeyStore
import com.meshchat.app.core.crypto.FakeSessionCrypto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScreen(
    onNavigateBack: () -> Unit,
    viewModel: QRViewModel = viewModel {
        // Create a temporary CryptoManager for now
        val keyStore = FakeKeyStore()
        val sessionCrypto = FakeSessionCrypto()
        val cryptoManager = CryptoManager(keyStore, sessionCrypto)
        QRViewModel(cryptoManager)
    }
) {
    val identityInfo by viewModel.identityInfo.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Identity & QR Code") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshIdentityInfo() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Safety Number Card
            SafetyNumberCard(safetyNumber = identityInfo.safetyNumber)
            
            // Public Keys Card
            PublicKeysCard(
                signingPublicKey = identityInfo.signingPublicKey,
                dhPublicKey = identityInfo.dhPublicKey
            )
            
            // QR Code Card
            QRCodeCard(qrData = identityInfo.qrData)
            
            // Instructions Card
            InstructionsCard()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyNumberCard(safetyNumber: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Safety Number",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "This is your unique safety number. Compare it with your peer to verify the connection is secure.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = safetyNumber.ifEmpty { "Generating..." },
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicKeysCard(signingPublicKey: String, dhPublicKey: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Public Keys",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "These are your public cryptographic keys. Share them with peers to establish secure connections.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Signing Public Key
            Text(
                text = "Signing Public Key:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = signingPublicKey.ifEmpty { "Generating..." },
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            
            // DH Public Key
            Text(
                text = "Diffie-Hellman Public Key:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = dhPublicKey.ifEmpty { "Generating..." },
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRCodeCard(qrData: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "QR Code",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "Scan this QR code to share your identity with another device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (qrData.isNotEmpty()) {
                // TODO: Implement actual QR code generation
                // For now, show a placeholder
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "QR Code\nPlaceholder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = "QR Data:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                
                Text(
                    text = qrData,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "How to Use",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            val instructions = listOf(
                "1. Share your safety number with your peer",
                "2. Compare safety numbers to verify identity",
                "3. Scan each other's QR codes to exchange keys",
                "4. Messages will be encrypted automatically"
            )
            
            instructions.forEach { instruction ->
                Text(
                    text = instruction,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}
