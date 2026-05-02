package server;

import shared.AccountProtocol;
import shared.Protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

final class ClientHandler implements Runnable {
    private final Socket socket;
    private final Consumer<String> messageConsumer;
    private final Consumer<String> logConsumer;
    private final Runnable onClose;
    private final UserStore userStore;
    private DataInputStream input;
    private DataOutputStream output;
    private volatile boolean active = true;
    private String username;

    ClientHandler(Socket socket, Consumer<String> messageConsumer, Consumer<String> logConsumer, Runnable onClose, UserStore userStore) {
        this.socket = socket;
        this.messageConsumer = messageConsumer;
        this.logConsumer = logConsumer;
        this.onClose = onClose;
        this.userStore = userStore;
    }

    @Override
    public void run() {
        try {
            socket.setSoTimeout(5 * 60 * 1000);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            while (active) {
                String command = input.readUTF();
                if (AccountProtocol.REGISTER.equals(command)) {
                    handleRegister();
                } else if (AccountProtocol.LOGIN.equals(command)) {
                    handleLogin();
                } else if (AccountProtocol.LOGOUT.equals(command)) {
                    handleLogout();
                } else if (Protocol.TEXT.equals(command)) {
                    handleText();
                } else if (Protocol.IMAGE.equals(command)) {
                    handleImage();
                } else if (Protocol.DISCONNECT.equals(command)) {
                    break;
                } else {
                    logEvent("Unknown command received");
                }
            }
        } catch (IOException ex) {
            logError("Client error", ex);
        } finally {
            close();
        }
    }

    private void handleRegister() throws IOException {
        String user = sanitize(input.readUTF());
        String pass = input.readUTF();
        boolean ok = userStore != null && userStore.register(user, pass);
        output.writeUTF(ok ? AccountProtocol.AUTH_OK : AccountProtocol.AUTH_FAIL);
        output.writeUTF(ok ? "Registered" : "Register failed");
        output.flush();
        if (ok) {
            logEvent("Account registered: " + user);
        }
    }

    private void handleLogin() throws IOException {
        String user = sanitize(input.readUTF());
        String pass = input.readUTF();
        boolean ok = userStore != null && userStore.authenticate(user, pass);
        output.writeUTF(ok ? AccountProtocol.AUTH_OK : AccountProtocol.AUTH_FAIL);
        output.writeUTF(ok ? "Login successful" : "Login failed");
        output.flush();
        if (ok) {
            username = user;
            logEvent("Account logged in: " + user);
            sendHistory();
        } else {
            logEvent("Login failed");
            output.writeUTF(Protocol.HISTORY_END);
            output.flush();
        }
    }

    private void sendHistory() throws IOException {
        if (userStore != null) {
            List<String[]> history = userStore.getRecentMessages(50);
            for (String[] item : history) {
                output.writeUTF(Protocol.HISTORY);
                output.writeUTF(item[0]); // type
                output.writeUTF(item[1]); // sender
                output.writeUTF(item[2]); // content
                output.writeUTF(item[3]); // timestamp ms
            }
        }
        output.writeUTF(Protocol.HISTORY_END);
        output.flush();
    }

    private void handleLogout() throws IOException {
        username = null;
        output.writeUTF(Protocol.TEXT);
        output.writeUTF("Logged out");
        output.flush();
        logEvent("Account logged out");
    }

    private void handleText() throws IOException {
        String message = input.readUTF();
        if (message.length() > 1000) {
            throw new IOException("Message too long");
        }
        if (username != null && userStore != null) {
            userStore.saveMessage(username, "TEXT", message);
        }
        messageConsumer.accept(prefix() + escape(message));
        output.writeUTF(Protocol.TEXT);
        output.writeUTF("Received: " + message);
        output.flush();
    }

    private void handleImage() throws IOException {
        String filename = input.readUTF();
        long size = input.readLong();
        if (size < 0 || size > Protocol.MAX_IMAGE_SIZE) {
            throw new IOException("Invalid image size: " + size);
        }
        File dir = new File(Protocol.IMAGE_SAVE_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create image directory");
        }
        String safeName = Paths.get(filename).getFileName().toString();
        if (safeName.isBlank() || safeName.contains("..") || safeName.contains("/") || safeName.contains("\\")) {
            throw new IOException("Invalid filename");
        }
        File target = new File(dir, safeName);
        try (FileOutputStream fileOut = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            long remaining = size;
            while (remaining > 0) {
                int read = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) {
                    throw new IOException("Unexpected end of image stream");
                }
                fileOut.write(buffer, 0, read);
                remaining -= read;
            }
        }
        if (username != null && userStore != null) {
            userStore.saveMessage(username, "IMAGE", safeName);
        }
        messageConsumer.accept(prefix() + "[IMAGE] " + safeName + " (" + size + " bytes)");
        output.writeUTF(Protocol.TEXT);
        output.writeUTF("Saved image");
        output.flush();
    }

    private String prefix() {
        return (username != null ? username : socket.getRemoteSocketAddress().toString()) + ": ";
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    private String escape(String message) {
        return message.replace("\n", " ").replace("\r", " ");
    }

    private void close() {
        active = false;
        onClose.run();
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
            socket.close();
        } catch (IOException ignored) {
        }
        logEvent("Client disconnected");
    }

    private void logEvent(String message) {
        logConsumer.accept("[INFO] " + message);
    }

    private void logError(String context, Exception ex) {
        logConsumer.accept("[ERROR] " + context + ": " + ex.getClass().getSimpleName());
    }
}
