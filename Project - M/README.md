# MeshChat

A decentralized mesh networking chat application for Android that enables peer-to-peer communication without internet connectivity using Bluetooth Low Energy (BLE).

## Features

- **BLE Mesh Networking**: Discover and connect to nearby peers using Bluetooth Low Energy
- **Ephemeral ID Rotation**: Automatically rotates device identifiers every 15 minutes for privacy
- **BLE Data Channel**: GATT service with INBOX characteristic for data transfer
- **Framing Layer**: Chunking, reassembly, and routing with TTL-based flooding
- **Chat Interface**: Simple peer-to-peer messaging with real-time updates
- **Message Persistence**: Room database for storing chat messages with lazy loading
- **Delivery States**: SENT → RELAYED → DELIVERED with ACK frame support
- **Diagnostics Screen**: Real-time mesh statistics, logs viewer, and export functionality
- **Settings Screen**: Configurable rotation intervals, scan modes, and relay settings
- **Log Management**: Ring buffer logging with export capabilities
- **Cryptographic Security**: Identity verification, key exchange, and message encryption
- **QR Code Identity**: Share identity information via QR codes for secure peer verification

## Architecture

### Core Components

- **EphemeralIdRotator**: Generates and rotates 8-byte ephemeral IDs every 15 minutes
- **MeshBleManager**: Manages BLE advertising, scanning, and peer discovery
- **GattServerManager**: Handles incoming connections and data reception
- **GattClientManager**: Manages outgoing connections and data transmission
- **MeshLink**: Interface for peer-to-peer data communication
- **Chunker**: Splits large messages into BLE-compatible chunks
- **Reassembler**: Reconstructs messages from chunks with deduplication
- **MeshRouter**: Routes messages through the mesh network with TTL-based flooding

### Cryptographic Components

- **KeyStore**: Manages cryptographic keys (signing and DH keys)
- **SessionCrypto**: Handles session-based encryption/decryption
- **CryptoManager**: Coordinates cryptographic operations
- **FakeKeyStore/FakeSessionCrypto**: Development implementations
- **RealSessionCrypto**: Stub for future libsodium integration

### Data Flow

1. **Message Creation**: User types message in ChatScreen
2. **Encryption**: Message payload encrypted using SessionCrypto
3. **Envelope Creation**: Encrypted message wrapped in Envelope with metadata (TTL, flags, etc.)
4. **Chunking**: Large messages split into chunks for BLE transmission
5. **Transmission**: Chunks sent via MeshLink to peer
6. **Reassembly**: Peer reassembles chunks into complete Envelope
7. **Decryption**: Message payload decrypted using SessionCrypto
8. **Routing**: MeshRouter processes envelope and delivers to local inbox or floods to other peers
9. **ACK Handling**: Receiving peer sends ACK frame back to sender
10. **Status Updates**: Message status progresses through SENT → RELAYED → DELIVERED

### Delivery States

- **SENDING**: Message being prepared for transmission
- **SENT**: Message sent to local router
- **RELAYED**: Message acknowledged by local router
- **DELIVERED**: Message acknowledged by peer
- **READ**: Message read by peer (future enhancement)
- **FAILED**: Message failed to send
- **RECEIVED**: Message received from peer

## Technical Details

### BLE Configuration

- **Service UUID**: `0000a0a0-0000-1000-8000-00805f9b34fb`
- **INBOX Characteristic**: `0000a1a1-0000-1000-8000-00805f9b34fb`
- **MTU Size**: Up to 247 bytes (BLE maximum)
- **Scan Mode**: Configurable (Low Latency, Balanced, Low Power)

### Message Framing

- **Chunk Header**: 8 bytes `[u32 msg_seq, u16 idx, u16 total]`
- **Envelope Format**: Length-prefixed with metadata and payload
- **ACK Frames**: Special frame type for delivery confirmation
- **TTL Support**: Time-to-live for message flooding (default: 8 hops)
- **Encryption Flags**: Support for encrypted and signed messages

### Cryptographic Security

- **Identity Keys**: Ed25519 signing keys for identity verification
- **Key Exchange**: X25519 Diffie-Hellman for session establishment
- **Message Encryption**: XChaCha20-Poly1305 for authenticated encryption
- **Safety Numbers**: Hash-based verification for peer identity
- **QR Code Exchange**: Secure sharing of public keys

### Database Schema

- **ChatMessage**: Stores all chat messages with delivery status
- **Room Integration**: Full CRUD operations with lazy loading
- **Type Converters**: UUID support for message identification

### Settings

- **Ephemeral ID Rotation**: 5-60 minute intervals
- **Scan Mode**: Low Latency, Balanced, Low Power
- **Relay Mode**: Enable/disable message relaying
- **Log Level**: Verbose to Error
- **Max Log Entries**: 100-10,000 entries
- **Notifications**: Sound, vibration, and notification preferences

## Permissions

- `BLUETOOTH`: Basic Bluetooth functionality
- `BLUETOOTH_ADMIN`: Bluetooth administration
- `BLUETOOTH_SCAN`: Scan for BLE devices (Android 12+)
- `BLUETOOTH_ADVERTISE`: Advertise BLE services (Android 12+)
- `BLUETOOTH_CONNECT`: Connect to BLE devices (Android 12+)
- `ACCESS_FINE_LOCATION`: Required for BLE scanning on older devices
- `FOREGROUND_SERVICE`: Keep mesh network running in background

## Usage

### Starting the Mesh Network

1. Navigate to the Home screen
2. Toggle "Mesh Network" to start
3. The app will begin advertising and scanning for peers
4. Current ephemeral ID is displayed for verification

### Connecting to Peers

1. Navigate to the Peers screen
2. View discovered peers with RSSI and ephemeral ID information
3. Tap "Connect" to establish a connection
4. Monitor connection status and MTU size

### Chatting

1. Navigate to Peers screen
2. Tap "Test Chat" on a connected peer
3. Type and send messages
4. Monitor delivery status through status icons

### Identity Verification

1. Navigate to Identity & QR Code screen
2. View your safety number and public keys
3. Share QR code with peer for key exchange
4. Compare safety numbers to verify identity

### Diagnostics

1. Navigate to Diagnostics screen
2. View real-time mesh statistics
3. Monitor logs with color-coded levels
4. Export logs to text file for analysis

### Settings

1. Navigate to Settings screen
2. Configure mesh network parameters
3. Adjust logging and notification preferences
4. Save or reset to defaults

## Future Enhancements

- [ ] End-to-end encryption using libsodium
- [ ] Advanced routing algorithms (AODV, OLSR)
- [ ] File sharing and media support
- [ ] Group chat functionality
- [ ] Offline message queuing
- [ ] Battery optimization strategies
- [ ] Mesh network visualization
- [ ] Cross-platform compatibility
- [ ] Real QR code generation
- [ ] Advanced identity management
- [ ] Certificate-based authentication

## Requirements

- Android 8.0+ (API 26+)
- Bluetooth Low Energy support
- Location permissions (required for BLE scanning)

## Dependencies

- **AndroidX Core KTX**: Core Android functionality
- **Jetpack Compose**: Modern UI toolkit
- **Room**: Database persistence
- **WorkManager**: Background task management
- **Lifecycle**: ViewModel and LiveData
- **Navigation Compose**: Screen navigation
- **Bluetooth**: BLE functionality
- **libsodium**: Cryptographic operations (future)

## Building

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Build and run on device with BLE support

## Contributing

Contributions are welcome! Please ensure all code follows the project's coding standards and includes appropriate tests.


