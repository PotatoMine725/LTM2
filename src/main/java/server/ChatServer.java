package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ChatServer {
    private final Consumer<String> logConsumer;
    private final Consumer<String> messageConsumer;
    private final Consumer<String> statusConsumer;
    private final Set<Thread> workers = Collections.synchronizedSet(new HashSet<>());
    private final UserStore userStore = new UserStore();
    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public ChatServer(Consumer<String> logConsumer, Consumer<String> messageConsumer, Consumer<String> statusConsumer) {
        this.logConsumer = logConsumer;
        this.messageConsumer = messageConsumer;
        this.statusConsumer = statusConsumer;
    }

    public synchronized void start(int port) {
        if (running) {
            logEvent("Server already running");
            return;
        }
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            running = true;
            statusConsumer.accept("Running on port " + port);
            acceptThread = new Thread(this::acceptLoop, "server-accept-thread");
            acceptThread.start();
            logEvent("Server started on port " + port);
        } catch (IOException ex) {
            logError("Failed to start server", ex);
            statusConsumer.accept("Stopped");
        }
    }

    public synchronized void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ex) {
            logError("Error while stopping server", ex);
        } finally {
            statusConsumer.accept("Stopped");
            logEvent("Server stopped");
        }
    }

    public UserStore getUserStore() {
        return userStore;
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                ClientHandler handler = new ClientHandler(socket, messageConsumer, logConsumer, () -> workers.remove(Thread.currentThread()), userStore);
                Thread worker = new Thread(handler, "client-handler-" + socket.getPort());
                workers.add(worker);
                worker.start();
                logEvent("Client connected");
            } catch (IOException ex) {
                if (running) {
                    logError("Accept error", ex);
                }
            }
        }
    }

    private void logEvent(String text) {
        logConsumer.accept("[INFO] " + text);
    }

    private void logError(String context, Exception ex) {
        logConsumer.accept("[ERROR] " + context + ": " + ex.getClass().getSimpleName());
    }
}
