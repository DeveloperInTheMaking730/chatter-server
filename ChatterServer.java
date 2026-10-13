import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standard standalone Java Server Engine for the Chatter App.
 * Handles live anonymous structural handshakes and session packet relay.
 */
public class ChatterServer {
    // Dynamic fallback port to comply with cloud environment variable routing constraints
    private static final int DEFAULT_PORT = 8080;
    
    // Tracks active connections indexed strictly by their volatile 7-digit friend code mapping
    private static final Map<String, ClientSession> connectedNodes = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
        // Automated binding configuration check for standard container deployment frameworks (e.g., Render, Railway)
        String environmentPort = System.getenv("PORT");
        if (environmentPort != null && !environmentPort.trim().isEmpty()) {
            try {
                port = Integer.parseInt(environmentPort.trim());
            } catch (NumberFormatException nfe) {
                System.out.println("[Configuration Warning] Invalid system port variable fallback used.");
            }
        }

        System.out.println("[Chatter Cloud Engine] Initializing application core layout on port: " + port);

        try (ServerSocket systemSocket = new ServerSocket(port)) {
            while (true) {
                try {
                    Socket incomingConnection = systemSocket.accept();
                    // Spawn asynchronous worker instance execution cycle for isolated stability
                    new Thread(new ClientSession(incomingConnection)).start();
                } catch (IOException connectionException) {
                    System.err.println("[Socket Interface Exception] Dropped inbound synchronization loop: " + connectionException.getMessage());
                }
            }
        } catch (IOException serverException) {
            System.err.println("[Critical Core Failure] Application terminated layout setup: " + serverException.getMessage());
        }
    }

    /**
     * Isolated thread runtime controlling atomic socket streams and protocol structural delivery handlers.
     */
    private static class ClientSession implements Runnable {
        private final Socket networkSocket;
        private DataInputStream readerPipeline;
        private DataOutputStream writerPipeline;
        private String registeredUserCode = "";
        private String allocatedUsername = "";

        public ClientSession(Socket activeSocket) {
            this.networkSocket = activeSocket;
        }

        @Override
        public void run() {
            try {
                networkSocket.setTcpNoDelay(true); // Suppresses micro-latency queue buffering
                readerPipeline = new DataInputStream(networkSocket.getInputStream());
                writerPipeline = new DataOutputStream(networkSocket.getOutputStream());

                // 1. Core Handshake Block: Capture transient metadata layout parameters
                allocatedUsername = readerPipeline.readUTF();
                registeredUserCode = readerPipeline.readUTF();

                if (registeredUserCode.trim().isEmpty() || connectedNodes.containsKey(registeredUserCode)) {
                    writerPipeline.writeByte(99); // Code rejection error flag
                    writerPipeline.writeUTF("Identifier sequence collision or empty configuration.");
                    writerPipeline.flush();
                    return;
                }

                // Node validated - place on the temporary look-up index registry matrix
                connectedNodes.put(registeredUserCode, this);
                writerPipeline.writeByte(100); // Access confirmation tracking handshake signal
                writerPipeline.flush();
                System.out.println("[Session Registered] Node handle '" + allocatedUsername + "' bound cleanly to code sequence: " + registeredUserCode);

                // 2. Active Networking Pipeline Management Loop
                while (true) {
                    int transmissionHeaderCode = readerPipeline.readByte();

                    switch (transmissionHeaderCode) {
                        case 1: // Dynamic Invite Notification Signal Request Check
                            String targetedFriendCode = readerPipeline.readUTF();
                            ClientSession requestedPeer = connectedNodes.get(targetedFriendCode);

                            if (requestedPeer != null && !targetedFriendCode.equals(registeredUserCode)) {
                                synchronized (requestedPeer.networkSocket) {
                                    requestedPeer.writerPipeline.writeByte(10); // Outbound invite packet notification header
                                    requestedPeer.writerPipeline.writeUTF(allocatedUsername);
                                    requestedPeer.writerPipeline.writeUTF(registeredUserCode);
                                    requestedPeer.writerPipeline.flush();
                                }
                                System.out.println("[Protocol Routing] Delivery optimization: " + allocatedUsername + " targeted code target " + targetedFriendCode);
                            } else {
                                writerPipeline.writeByte(91); // System Response Target Offline alert
                                writerPipeline.flush();
                            }
                            break;

                        case 2: // Universal Encrypted Application Frame Payload Relay
                            String messageDestinationCode = readerPipeline.readUTF();
                            int structuralDataLength = readerPipeline.readInt();
                            byte[] atomicEncryptedPayload = new byte[structuralDataLength];
                            readerPipeline.readFully(atomicEncryptedPayload);

                            ClientSession downstreamReceiverNode = connectedNodes.get(messageDestinationCode);
                            if (downstreamReceiverNode != null) {
                                synchronized (downstreamReceiverNode.networkSocket) {
                                    downstreamReceiverNode.writerPipeline.writeByte(20); // Downstream incoming message block header
                                    downstreamReceiverNode.writerPipeline.writeUTF(registeredUserCode); // Sender identifying address context
                                    downstreamReceiverNode.writerPipeline.writeInt(structuralDataLength);
                                    downstreamReceiverNode.writerPipeline.write(atomicEncryptedPayload);
                                    downstreamReceiverNode.writerPipeline.flush();
                                }
                            }
                            break;

                        default:
                            System.out.println("[Protocol Anomaly] Received unrecognizable transmission code configuration flags.");
                            break;
                    }
                }
            } catch (EOFException cleanDisconnect) {
                // Connection terminated gracefully by client termination workflow
            } catch (Exception runtimeError) {
                System.err.println("[Session Drop] Connection anomaly terminated on target index tracking: " + allocatedUsername + " | Info: " + runtimeError.getMessage());
            } finally {
                terminateSessionContext();
            }
        }

        private void terminateSessionContext() {
            if (!registeredUserCode.isEmpty()) {
                connectedNodes.remove(registeredUserCode);
                System.out.println("[Session Purged] Connection safely deregistered from active runtime node maps for sequence: " + registeredUserCode);
            }
            try { if (readerPipeline != null) readerPipeline.close(); } catch (IOException ignored) {}
            try { if (writerPipeline != null) writerPipeline.close(); } catch (IOException ignored) {}
            try { if (networkSocket != null && !networkSocket.isClosed()) networkSocket.close(); } catch (IOException ignored) {}
        }
    }
}
