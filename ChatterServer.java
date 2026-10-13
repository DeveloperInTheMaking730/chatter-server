import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatterServer {
    private static final int DEFAULT_PORT = 8080;
    private static final Map<String, ClientConnection> activeClients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        }

        System.out.println("[Chatter Cloud Server] Initialized. Listening on port " + port + "...");
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                // Handle everything in a separate thread so the server never freezes
                new Thread(new ClientConnection(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("[Server Error] Engine crashed: " + e.getMessage());
        }
    }

    private static class ClientConnection implements Runnable {
        private final Socket socket;
        private DataInputStream in;
        private DataOutputStream out;
        private String username = "";
        private String friendCode = "";

        public ClientConnection(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                socket.setTcpNoDelay(true);
                
                // Read the first byte to see if it's a web browser checking the link
                BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
                bis.mark(4);
                byte[] headBytes = new byte[4];
                int read = bis.read(headBytes, 0, 4);
                
                if (read >= 3 && headBytes[0] == 'G' && headBytes[1] == 'E' && headBytes[2] == 'T') {
                    // FIX: Satisfy Render's web browser portal checking scanner instantly
                    OutputStream os = socket.getOutputStream();
                    String response = "HTTP/1.1 200 OK\r\nContent-Length: 13\r\n\r\nChatter Live!";
                    os.write(response.getBytes());
                    os.flush();
                    socket.close();
                    return;
                }
                
                // If it's not a browser scanner, reset the stream and run our regular chat data loops
                bis.reset();
                in = new DataInputStream(bis);
                out = new DataOutputStream(socket.getOutputStream());

                // Read client handshake packets on login
                this.username = in.readUTF();
                this.friendCode = in.readUTF();

                activeClients.put(this.friendCode, this);
                System.out.println("[User Online] " + username + " registered with Code: " + friendCode);

                while (true) {
                    int header = in.read();
                    if (header == -1) break; 

                    if (header == 1) { 
                        String targetCode = in.readUTF();
                        ClientConnection recipient = activeClients.get(targetCode);

                        if (recipient != null) {
                            recipient.out.writeByte(10);
                            recipient.out.writeUTF(this.username);
                            recipient.out.writeUTF(this.friendCode);
                            recipient.out.flush();
                            
                            this.out.writeByte(11);
                            this.out.writeUTF(targetCode);
                            this.out.flush();
                        } else {
                            this.out.writeByte(91);
                            this.out.flush();
                        }
                    } else if (header == 2) {
                        String recipientCode = in.readUTF();
                        int payloadLength = in.readInt();
                        byte[] cipherBytes = new byte[payloadLength];
                        in.readFully(cipherBytes);

                        ClientConnection recipient = activeClients.get(recipientCode);
                        if (recipient != null) {
                            recipient.out.writeByte(20); 
                            recipient.out.writeUTF(this.friendCode); 
                            recipient.out.writeInt(payloadLength);
                            recipient.out.write(cipherBytes);
                            recipient.out.flush();
                        }
                    }
                }
            } catch (IOException e) {
                // Connection closed safely
            } finally {
                cleanUp();
            }
        }

        private void cleanUp() {
            if (!friendCode.isEmpty()) {
                activeClients.remove(friendCode);
                System.out.println("[User Offline] Account @" + friendCode + " disconnected.");
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
