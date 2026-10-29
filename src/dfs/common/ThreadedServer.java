package dfs.common;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

// listens on a port and gives each connection to a worker thread, which passes every command it recieves to handler
public final class ThreadedServer {

    private final String name;
    private final int port;
    private final int poolSize;
    private final RequestHandler handler;
    private final Log log;

    private volatile boolean running;
    private ServerSocket serverSocket;
    private ExecutorService pool;

    public ThreadedServer(String name, int port, int poolSize, RequestHandler handler) {
        this.name = name;
        this.port = port;
        this.poolSize = poolSize;
        this.handler = handler;
        this.log = new Log(name);
    }

    // opens the port, creates the worker pool and starts the thread that accepts connections
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        pool = Executors.newFixedThreadPool(poolSize);
        running = true;
        new Thread(this::acceptLoop, name + "-accept").start();
        log.info("listening on port %d (pool=%d)", port, poolSize);
    }

    // waits for new connections and hands each one to a worker until the server is stopped
    private void acceptLoop() {
        while (running) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                if (running) {
                    log.warn("accept failed: %s", e.getMessage());
                }
                continue;
            }
            try {
                pool.execute(() -> serve(socket));
            } catch (RejectedExecutionException e) {
                closeQuietly(socket);
            }
        }
    }

    // runs on a worker, reads commands from one connection and passes them to the handler
    private void serve(Socket socket) {
        Connection connection;
        try {
            connection = new Connection(socket);
        } catch (IOException e) {
            closeQuietly(socket);
            return;
        }

        try (Connection conn = connection) {
            String line;
            while ((line = conn.readLine()) != null) {
                try {
                    handler.handle(line, conn);
                } catch (ProtocolException e) {
                    conn.writeLine(Protocol.ERROR + " " + e.getMessage());
                }
            }
        } catch (SocketTimeoutException e) {
            log.info("closing idle connection %s", connection.remoteAddress());
        } catch (IOException e) {
            // the other side went away
        }
    }

    // closing the server socket wakes up the blocked accept() so the accept loop can exit
    public void stop() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        if (pool != null) {
            pool.shutdownNow();
            try {
                pool.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("stopped");
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}