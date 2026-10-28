package dfs.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

// wraps a socket so the rest of the code can send and receive messages without touching streams
public final class Connection implements Closeable {

    // longest text line accepted, so a client that never sends a newline can't fill the memory
    public static final int MAX_LINE = 8 * 1024;

    // timeouts so that a client that vanishes wouldn't block a thread forever
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int CONNECT_TIMEOUT_MS = 5_000;

    private final Socket socket;
    private final String remoteAddress;
    private final DataInputStream in;
    private final OutputStream out;

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        this.remoteAddress = String.valueOf(socket.getRemoteSocketAddress());
        socket.setSoTimeout(READ_TIMEOUT_MS);
        socket.setTcpNoDelay(true);
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream(), 64 * 1024));
        this.out = new BufferedOutputStream(socket.getOutputStream(), 64 * 1024);
    }

    public static Connection open(String host, int port) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            return new Connection(socket);
        } catch (IOException e) {
            closeQuietly(socket);
            throw e;
        }
    }

    // returns null when the client hangs up cleanly between messages
    public String readLine() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(128);
        while (true) {
            int b = in.read();
            if (b == -1) {
                if (buffer.size() == 0) {
                    return null;
                }
                throw new EOFException("stream ended mid-line");
            }
            if (b == '\n') {
                break;
            }
            buffer.write(b);
            if (buffer.size() > MAX_LINE) {
                throw new ProtocolException("line longer than " + MAX_LINE + " bytes");
            }
        }
        String line = new String(buffer.toByteArray(), StandardCharsets.US_ASCII);
        return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
    }

    public void writeLine(String line) throws IOException {
        out.write(line.getBytes(StandardCharsets.US_ASCII));
        out.write('\n');
        // flush prevents line sitting in buffer both sides waiting on eachother
        out.flush();
    }

    // a plain read can return after the first few KB, readFully keeps going until it has "len" amount bytes
    public void readFully(byte[] dest, int off, int len) throws IOException {
        in.readFully(dest, off, len);
    }

    public void writeBytes(byte[] src, int off, int len) throws IOException {
        out.write(src, off, len);
        out.flush();
    }

    public String remoteAddress() {
        return remoteAddress;
    }

    @Override
    public void close() {
        closeQuietly(socket);
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // already closing, nothing useful to do
        }
    }
}