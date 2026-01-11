package org.firstinspires.ftc.teamcode.telelib;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Simple TCP tunnel to forward Limelight traffic from the Control Hub to the network.
 *
 * This lets laptops connect to the Control Hub IP instead of the Limelight USB IP.
 * We forward raw TCP bytes, so it works for MJPEG, API, and websocket ports.
 */
public class LimelightTunnel implements AutoCloseable {
    // Where the Limelight actually lives (USB network).
    private final String remoteHost;
    private final int remotePort;
    // Where we listen on the Control Hub.
    private final String bindHost;
    private final int bindPort;

    // When false, stop accepting clients and stop relays.
    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    /**
     * Create a TCP tunnel from the bind host/port to the Limelight host/port.
     */
    public LimelightTunnel(String bindHost, int bindPort, String remoteHost, int remotePort) {
        this.bindHost = bindHost;
        this.bindPort = bindPort;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    /** Start the tunnel accept loop in a background thread. */
    public void start() {
        if (running) {
            return;
        }
        running = true;
        acceptThread = new Thread(this::runAcceptLoop, "LimelightTunnel");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    @Override
    public void close() {
        // Stop listening and close the server socket.
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void runAcceptLoop() {
        /*
         * Accept incoming TCP clients and spawn a relay thread for each one.
         * Each connection gets a fresh socket to the Limelight device.
         */
        try (ServerSocket server = new ServerSocket()) {
            serverSocket = server;
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(bindHost, bindPort));
            while (running) {
                Socket client = server.accept();
                Thread thread = new Thread(() -> handleClient(client), "LimelightTunnelClient");
                thread.setDaemon(true);
                thread.start();
            }
        } catch (IOException ignored) {
        } finally {
            running = false;
        }
    }

    private void handleClient(Socket client) {
        /*
         * Connect to the Limelight and relay traffic in both directions.
         * The downstream thread is Limelight -> client, this thread is client -> Limelight.
         */
        // Each client gets its own remote Limelight connection.
        try (Socket remote = new Socket()) {
            remote.connect(new InetSocketAddress(remoteHost, remotePort), 1000);
            client.setTcpNoDelay(true);
            remote.setTcpNoDelay(true);
            client.setSoTimeout(2000);
            remote.setSoTimeout(2000);

            // Two-way relay: remote -> client and client -> remote.
            Thread downstream = new Thread(() -> relay(remote, client), "LimelightTunnelDownstream");
            downstream.setDaemon(true);
            downstream.start();
            relay(client, remote);
        } catch (IOException ignored) {
        } finally {
            try {
                client.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void relay(Socket from, Socket to) {
        /*
         * Pump bytes until EOF or shutdown; timeouts allow periodic shutdown checks.
         */
        try {
            InputStream in = from.getInputStream();
            OutputStream out = to.getOutputStream();
            byte[] buffer = new byte[4096];
            while (running) {
                try {
                    int read = in.read(buffer);
                    if (read == -1) {
                        return;
                    }
                    out.write(buffer, 0, read);
                    out.flush();
                } catch (SocketTimeoutException ignored) {
                    if (!running) {
                        return;
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }
}
