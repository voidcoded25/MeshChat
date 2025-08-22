package com.meshchat.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meshchat.app.R
import com.meshchat.app.core.data.ChatMessage
import com.meshchat.app.core.data.PeerDevice
import com.meshchat.app.core.data.MessageStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    peer: PeerDevice,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    
    val listState = rememberLazyListState()
    val focusRequester = rememberFocusRequester()
    val focusManager = LocalFocusManager.current
    
    var messageText by remember { mutableStateOf("") }
    
    // Initialize chat when peer changes
    LaunchedEffect(peer) {
        viewModel.initializeChat(peer)
    }
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Chat with ${peer.addressHash}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isConnected) "Connected" else "Disconnected",
                            fontSize = 12.sp,
                            color = if (isConnected) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.error
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { message ->
                    ChatMessageItem(message = message)
                }
            }
            
            // Input area
            ChatInputArea(
                messageText = messageText,
                onMessageTextChange = { messageText = it },
                onSendMessage = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                        focusManager.clearFocus()
                    }
                },
                isConnected = isConnected,
                isSending = isSending,
                focusRequester = focusRequester
            )
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isFromMe = message.isFromMe
    val backgroundColor = if (isFromMe) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    
    val alignment = if (isFromMe) Alignment.End else Alignment.Start
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isFromMe) 16.dp else 4.dp,
                bottomEnd = if (isFromMe) 4.dp else 16.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.getFormattedTime(),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (isFromMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusIcon(status = message.status)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageStatusIcon(status: MessageStatus) {
    val iconColor = when (status) {
        MessageStatus.SENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        MessageStatus.SENT -> MaterialTheme.colorScheme.onSurfaceVariant
        MessageStatus.DELIVERED -> MaterialTheme.colorScheme.primary
        MessageStatus.READ -> MaterialTheme.colorScheme.primary
        MessageStatus.FAILED -> MaterialTheme.colorScheme.error
        MessageStatus.RECEIVED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val iconSize = 12.dp
    
    when (status) {
        MessageStatus.SENDING -> {
            CircularProgressIndicator(
                modifier = Modifier.size(iconSize),
                strokeWidth = 1.dp,
                color = iconColor
            )
        }
        MessageStatus.SENT -> {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Sent",
                modifier = Modifier.size(iconSize),
                tint = iconColor
            )
        }
        MessageStatus.DELIVERED -> {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Delivered",
                modifier = Modifier.size(iconSize),
                tint = iconColor
            )
        }
        MessageStatus.READ -> {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Read",
                modifier = Modifier.size(iconSize),
                tint = iconColor
            )
        }
        MessageStatus.FAILED -> {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Failed",
                modifier = Modifier.size(iconSize),
                tint = iconColor
            )
        }
        MessageStatus.RECEIVED -> {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Received",
                modifier = Modifier.size(iconSize),
                tint = iconColor
            )
        }
    }
}

@Composable
fun ChatInputArea(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    isConnected: Boolean,
    isSending: Boolean,
    focusRequester: FocusRequester
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = messageText,
            onValueChange = onMessageTextChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            placeholder = {
                Text(
                    text = if (isConnected) "Type a message..." else "Not connected",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            enabled = isConnected && !isSending,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = { onSendMessage() }
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        FloatingActionButton(
            onClick = onSendMessage,
            enabled = isConnected && !isSending && messageText.isNotBlank(),
            modifier = Modifier.size(56.dp)
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
