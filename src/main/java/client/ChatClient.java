package client;

import shared.AccountProtocol;
import shared.Protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class ChatClient {
    private volatile Consumer<String> statusConsumer;
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private Thread listenerThread;
    private volatile boolean connected;
    // [type, sender, content, timestamp_ms] — buffered until ChatFrame reads them
    private final List<String[]> pendingHistory = new ArrayList<>();

    public ChatClient(Consumer<String> statusConsumer) {
        this.statusConsumer = statusConsumer;
    }

    public void setStatusHandler(Consumer<String> handler) {
        this.statusConsumer = handler;
    }

    public boolean isConnected() {
        return connected;
    }

    public synchronized void connect(String host, int port) {
        if (connected) {
            statusConsumer.accept("Already connected");
            return;
        }
        try {
            socket = new Socket(host, port);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            connected = true;
            statusConsumer.accept("Connected to " + host + ":" + port);
        } catch (IOException ex) {
            statusConsumer.accept("Connect failed: " + ex.getClass().getSimpleName());
        }
    }

    public synchronized boolean register(String username, String password) {
        return sendAuthCommand(AccountProtocol.REGISTER, username, password);
    }

    public synchronized boolean login(String username, String password) {
        boolean ok = sendAuthCommand(AccountProtocol.LOGIN, username, password);
        if (ok) {
            loadHistory();
            listenerThread = new Thread(this::listenLoop, "client-listener");
            listenerThread.setDaemon(true);
            listenerThread.start();
        }
        return ok;
    }

    public List<String[]> getPendingHistory() {
        return Collections.unmodifiableList(pendingHistory);
    }

    public void clearPendingHistory() {
        pendingHistory.clear();
    }

    private void loadHistory() {
        try {
            String cmd;
            while (!(cmd = input.readUTF()).equals(Protocol.HISTORY_END)) {
                if (Protocol.HISTORY.equals(cmd)) {
                    String type = input.readUTF();
                    String sender = input.readUTF();
                    String content = input.readUTF();
                    String timestamp = input.readUTF();
                    pendingHistory.add(new String[]{type, sender, content, timestamp});
                }
            }
        } catch (IOException ignored) {
        }
    }

    public synchronized void logout() {
        if (!connected) {
            statusConsumer.accept("Not connected");
            return;
        }
        try {
            output.writeUTF(AccountProtocol.LOGOUT);
            output.flush();
        } catch (IOException ex) {
            statusConsumer.accept("Logout failed: " + ex.getClass().getSimpleName());
        }
        closeResources();
        statusConsumer.accept("Logged out — reconnect to log in again");
    }

    public synchronized void sendText(String message) {
        if (!connected) {
            statusConsumer.accept("Not connected");
            return;
        }
        try {
            output.writeUTF(Protocol.TEXT);
            output.writeUTF(message);
            output.flush();
            // ChatFrame already shows the message locally before calling sendText
        } catch (IOException ex) {
            statusConsumer.accept("Send text failed: " + ex.getClass().getSimpleName());
        }
    }

    public synchronized void sendImage(File file) {
        if (!connected) {
            statusConsumer.accept("Not connected");
            return;
        }
        if (file == null || !file.isFile()) {
            statusConsumer.accept("Invalid file");
            return;
        }
        long size = file.length();
        if (size > Protocol.MAX_IMAGE_SIZE) {
            statusConsumer.accept("File too large");
            return;
        }
        try (FileInputStream inputStream = new FileInputStream(file)) {
            output.writeUTF(Protocol.IMAGE);
            output.writeUTF(file.getName());
            output.writeLong(size);
            inputStream.transferTo(output);
            output.flush();
            statusConsumer.accept("[You sent: " + file.getName() + "]");
        } catch (IOException ex) {
            statusConsumer.accept("Send image failed: " + ex.getClass().getSimpleName());
        }
    }

    public synchronized void disconnect() {
        if (!connected) {
            return;
        }
        try {
            output.writeUTF(Protocol.DISCONNECT);
            output.flush();
        } catch (IOException ignored) {
        }
        closeResources();
        statusConsumer.accept("Disconnected");
    }

    private boolean sendAuthCommand(String command, String username, String password) {
        if (!connected) {
            statusConsumer.accept("Not connected");
            return false;
        }
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            statusConsumer.accept("Username/password cannot be blank");
            return false;
        }
        try {
            output.writeUTF(command);
            output.writeUTF(username.trim());
            output.writeUTF(password.trim());
            output.flush();
            String response = input.readUTF();
            String message = input.readUTF();
            statusConsumer.accept(message);
            return AccountProtocol.AUTH_OK.equals(response);
        } catch (IOException ex) {
            statusConsumer.accept("Auth failed: " + ex.getClass().getSimpleName());
            return false;
        }
    }

    private void listenLoop() {
        try {
            while (connected) {
                String command = input.readUTF();
                if (Protocol.TEXT.equals(command)) {
                    String message = input.readUTF();
                    statusConsumer.accept("Server: " + message);
                } else if (Protocol.DISCONNECT.equals(command)) {
                    statusConsumer.accept("Server disconnected");
                    break;
                }
            }
        } catch (IOException ex) {
            if (connected) {
                statusConsumer.accept("Connection lost: " + ex.getClass().getSimpleName());
            }
        } finally {
            closeResources();
        }
    }

    private synchronized void closeResources() {
        connected = false;
        try {
            if (input != null) {
                input.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (output != null) {
                output.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
