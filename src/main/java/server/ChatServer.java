package server;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

public class ChatServer {
    private final Consumer<String> logConsumer;
    private final Consumer<String> messageConsumer;
    private final Consumer<String> statusConsumer;
    private final Set<Thread> workers = Collections.synchronizedSet(new HashSet<>());
    // L2: giới hạn tối đa 100 kết nối đồng thời — chống connection flood
    private static final int MAX_CONNECTIONS = 100;
    private final Semaphore connectionLimit = new Semaphore(MAX_CONNECTIONS);
    private final UserStore userStore = new UserStore();
    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    // C1: TLS keystore được bundle trong classpath
    private static final String KEYSTORE_RESOURCE = "/keystore/server.keystore";
    private static final char[] KEYSTORE_PASS = "ltm2pass".toCharArray();

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
            // C1: khởi động SSLServerSocket thay vì ServerSocket thông thường
            serverSocket = createSslServerSocket(port);
            serverSocket.setReuseAddress(true);
            running = true;
            statusConsumer.accept("Running on port " + port + " (TLS)");
            acceptThread = new Thread(this::acceptLoop, "server-accept-thread");
            acceptThread.start();
            logEvent("Server started on port " + port + " with TLS");
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

    /** C1: Tạo SSLServerSocket từ keystore bundled trong classpath */
    private ServerSocket createSslServerSocket(int port) throws IOException {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            try (InputStream ksStream = ChatServer.class.getResourceAsStream(KEYSTORE_RESOURCE)) {
                if (ksStream == null) {
                    throw new IOException("Keystore not found in classpath: " + KEYSTORE_RESOURCE);
                }
                ks.load(ksStream, KEYSTORE_PASS);
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, KEYSTORE_PASS);
            SSLContext sslCtx = SSLContext.getInstance("TLS");
            sslCtx.init(kmf.getKeyManagers(), null, null);
            SSLServerSocketFactory factory = sslCtx.getServerSocketFactory();
            SSLServerSocket sslSocket = (SSLServerSocket) factory.createServerSocket(port);
            sslSocket.setNeedClientAuth(false);
            return sslSocket;
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Failed to create TLS server socket: " + ex.getMessage(), ex);
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                // L2: từ chối nếu vượt quá giới hạn kết nối
                if (!connectionLimit.tryAcquire()) {
                    socket.close();
                    logEvent("Connection limit reached (" + MAX_CONNECTIONS + ") — client rejected");
                    continue;
                }
                socket.setTcpNoDelay(true);
                Runnable onClose = () -> {
                    connectionLimit.release();
                    workers.remove(Thread.currentThread());
                };
                ClientHandler handler = new ClientHandler(socket, messageConsumer, logConsumer, onClose, userStore);
                Thread worker = new Thread(handler, "client-handler-" + socket.getPort());
                workers.add(worker);
                worker.start();
                logEvent("Client connected (TLS)");
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
