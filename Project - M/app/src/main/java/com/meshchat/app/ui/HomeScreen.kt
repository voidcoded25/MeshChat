package com.meshchat.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meshchat.app.R

@Composable
fun HomeScreen(
    onNavigateToPeers: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val isMeshRunning by viewModel.isMeshRunning.collectAsState()
    val currentEphemeralId by viewModel.currentEphemeralId.collectAsState()
    
    LaunchedEffect(Unit) {
        // Refresh ephemeral ID when component is first launched
        viewModel.refreshEphemeralId(context)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isMeshRunning) {
                        "Mesh Network Active"
                    } else {
                        "Mesh Network"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (isMeshRunning && currentEphemeralId.isNotEmpty()) {
                    Text(
                        text = "Ephemeral ID:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    Text(
                        text = currentEphemeralId,
                        fontSize = 18.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                
                Button(
                    onClick = {
                        if (isMeshRunning) {
                            viewModel.stopMesh(context)
                        } else {
                            viewModel.startMesh(context)
                            // Refresh ephemeral ID after starting
                            viewModel.refreshEphemeralId(context)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = if (isMeshRunning) {
                            "Stop Mesh"
                        } else {
                            "Start Mesh"
                        },
                        fontSize = 16.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onNavigateToPeers,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isMeshRunning
        ) {
            Text(
                text = stringResource(R.string.peers),
                fontSize = 16.sp
            )
        }
    }
}
