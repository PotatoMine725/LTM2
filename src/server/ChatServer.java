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
            log("Server already running");
            return;
        }
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            statusConsumer.accept("Running on port " + port);
            acceptThread = new Thread(this::acceptLoop, "server-accept-thread");
            acceptThread.start();
            log("Server started on port " + port);
        } catch (IOException ex) {
            log("Failed to start server: " + ex.getMessage());
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
            log("Error while stopping server: " + ex.getMessage());
        } finally {
            statusConsumer.accept("Stopped");
            log("Server stopped");
        }
    }

    public UserStore getUserStore() {
        return userStore;
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, messageConsumer, logConsumer, () -> workers.remove(Thread.currentThread()), userStore);
                Thread worker = new Thread(handler, "client-handler-" + socket.getPort());
                workers.add(worker);
                worker.start();
                log("Client connected: " + socket.getRemoteSocketAddress());
            } catch (IOException ex) {
                if (running) {
                    log("Accept error: " + ex.getMessage());
                }
            }
        }
    }

    private void log(String text) {
        logConsumer.accept(text);
    }
}
