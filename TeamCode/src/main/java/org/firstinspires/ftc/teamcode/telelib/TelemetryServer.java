package org.firstinspires.ftc.teamcode.telelib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Robot-hosted telemetry server with a tiny text protocol.
 *
 * Client -> Server:
 * HELLO
 * SUB field1,field2 rate=20
 * SUB ALL rate=20
 * LISTCFG
 * SET key=value
 *
 * Server -> Client:
 * FIELDS name,type,unit;name,type,unit;...
 * CFG name,type,min,max;name,type,min,max;...
 * OK
 * DATA v1,v2,v3
 */
public class TelemetryServer implements AutoCloseable {
    private final int port;
    private final FieldCatalog catalog;
    private final ConfigRegistry configRegistry;
    private final int minIntervalMs;
    private final CopyOnWriteArrayList<ClientSession> sessions = new CopyOnWriteArrayList<>();
    private volatile boolean running;
    private volatile TelemetrySnapshot latestSnapshot;
    private Thread acceptThread;
    private Thread broadcastThread;

    /**
     * Create a server without live-config support.
     */
    public TelemetryServer(int port, FieldCatalog catalog) {
        this(port, catalog, null);
    }

    /**
     * Create a server that can list and update live-config values.
     */
    public TelemetryServer(int port, FieldCatalog catalog, ConfigRegistry configRegistry) {
        this(port, catalog, configRegistry, 100);
    }

    /**
     * Create a server that can list and update live-config values with a max client rate.
     */
    public TelemetryServer(
            int port, FieldCatalog catalog, ConfigRegistry configRegistry, int maxRateHz) {
        // Save the port and the list of fields the client can request.
        this.port = port;
        this.catalog = catalog;
        this.configRegistry = configRegistry;
        this.minIntervalMs = Math.max(1, 1000 / Math.max(1, maxRateHz));
        this.latestSnapshot = TelemetrySnapshot.empty(catalog.size());
    }

    /**
     * Start the accept and broadcast loops in background threads.
     */
    public void start() {
        if (running) {
            return;
        }
        // Start background threads for accepting clients and broadcasting data.
        running = true;
        acceptThread = new Thread(this::runAcceptLoop, "TelemetryServerAccept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        broadcastThread = new Thread(this::runBroadcastLoop, "TelemetryServerBroadcast");
        broadcastThread.setDaemon(true);
        broadcastThread.start();
    }

    /**
     * Update the latest telemetry snapshot; sent out on the next broadcast tick.
     */
    public void setSnapshot(TelemetrySnapshot snapshot) {
        // Update the latest data snapshot (called once per loop).
        this.latestSnapshot = snapshot;
    }

    /**
     * Stop the server and close all sessions.
     */
    @Override
    public void close() {
        // Stop the server and close all client connections.
        running = false;
        for (ClientSession session : sessions) {
            session.close();
        }
        sessions.clear();
    }

    private void runAcceptLoop() {
        /*
         * Accept incoming clients and create a session per connection.
         * Each session handles commands and subscription rate.
         */
        try (ServerSocket server = new ServerSocket()) {
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress("0.0.0.0", port));
            while (running) {
                // Wait for a laptop to connect.
                Socket client = server.accept();
                ClientSession session = new ClientSession(client);
                sessions.add(session);
                session.start();
            }
        } catch (IOException ignored) {
        } finally {
            running = false;
        }
    }

    private void runBroadcastLoop() {
        /*
         * Push the latest snapshot to each session at its configured rate.
         */
        while (running) {
            long now = System.currentTimeMillis();
            TelemetrySnapshot snapshot = latestSnapshot;
            for (ClientSession session : sessions) {
                session.maybeSend(snapshot, now);
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private class ClientSession implements AutoCloseable {
        private final Socket socket;
        private final BufferedReader reader;
        private final BufferedWriter writer;
        private volatile int[] fields = new int[0];
        private volatile long intervalMs = 50;
        private long lastSentMs = 0;
        private Thread readThread;

        ClientSession(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            socket.setTcpNoDelay(true);
        }

        void start() {
            // Start a thread that reads commands from this client.
            readThread = new Thread(this::readLoop, "TelemetryServerClient");
            readThread.setDaemon(true);
            readThread.start();
        }

        void maybeSend(TelemetrySnapshot snapshot, long nowMs) {
            if (fields.length == 0) {
                return;
            }
            if (nowMs - lastSentMs < intervalMs) {
                return;
            }
            // Send one CSV line with only the fields this client requested.
            String line = snapshot.toCsv(fields);
            try {
                writer.write("DATA ");
                writer.write(line);
                writer.write('\n');
                writer.flush();
                lastSentMs = nowMs;
            } catch (IOException ignored) {
                close();
            }
        }

        private void readLoop() {
            /*
             * Read client commands until disconnect.
             */
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    handleCommand(line.trim());
                }
            } catch (IOException ignored) {
            } finally {
                close();
            }
        }

        private void handleCommand(String line) throws IOException {
            if (line.isEmpty()) {
                return;
            }
            if (line.equalsIgnoreCase("HELLO") || line.equalsIgnoreCase("FIELDS")) {
                // Send the list of all available fields.
                sendFields();
                return;
            }
            if (line.equalsIgnoreCase("LISTCFG")) {
                sendConfig();
                return;
            }
            if (line.toUpperCase().startsWith("SET")) {
                handleSet(line.substring(3).trim());
                return;
            }
            if (line.toUpperCase().startsWith("SUB")) {
                // Client wants to subscribe to specific fields.
                parseSub(line.substring(3).trim());
                writer.write("OK\n");
                writer.flush();
                return;
            }
            writer.write("ERR unknown\n");
            writer.flush();
        }

        private void sendFields() throws IOException {
            /*
             * Send the full field catalog as a single FIELDS line.
             */
            // Format "name,type,unit" for each field.
            List<SchemaField> defs = catalog.getFields();
            StringBuilder out = new StringBuilder(defs.size() * 20);
            out.append("FIELDS ");
            for (int i = 0; i < defs.size(); i++) {
                if (i > 0) {
                    out.append(';');
                }
                SchemaField def = defs.get(i);
                out.append(def.name)
                        .append(',')
                        .append(def.type)
                        .append(',')
                        .append(def.unit == null ? "" : def.unit);
            }
            out.append('\n');
            writer.write(out.toString());
            writer.flush();
        }

        private void sendConfig() throws IOException {
            /*
             * Send live-config entries (CFG line) or an empty list.
             */
            if (configRegistry == null) {
                writer.write("CFG \n");
                writer.flush();
                return;
            }
            List<ConfigRegistry.ConfigEntry> entries = configRegistry.list();
            StringBuilder out = new StringBuilder(entries.size() * 24);
            out.append("CFG ");
            for (int i = 0; i < entries.size(); i++) {
                if (i > 0) {
                    out.append(';');
                }
                ConfigRegistry.ConfigEntry entry = entries.get(i);
                out.append(entry.getName())
                        .append(',')
                        .append(entry.getType())
                        .append(',')
                        .append(entry.getMin())
                        .append(',')
                        .append(entry.getMax());
            }
            out.append('\n');
            writer.write(out.toString());
            writer.flush();
        }

        private void handleSet(String args) throws IOException {
            /*
             * Apply SET name=value to the ConfigRegistry.
             */
            if (configRegistry == null) {
                writer.write("ERR no-config\n");
                writer.flush();
                return;
            }
            String[] parts = args.split("=", 2);
            if (parts.length != 2) {
                writer.write("ERR bad-format\n");
                writer.flush();
                return;
            }
            String name = parts[0].trim();
            String value = parts[1].trim();
            boolean ok = configRegistry.set(name, value);
            writer.write(ok ? "OK\n" : "ERR invalid\n");
            writer.flush();
        }

        private void parseSub(String args) {
            // Parse the field list and optional rate=... setting.
            String[] parts = args.split("\\s+");
            String fieldList = parts.length > 0 ? parts[0] : "";
            int rate = 20;
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i];
                if (part.startsWith("rate=")) {
                    try {
                        rate = Integer.parseInt(part.substring(5));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            if (rate <= 0) {
                rate = 20;
            }
            intervalMs = Math.max(minIntervalMs, 1000 / rate);

            if (fieldList.equalsIgnoreCase("ALL") || fieldList.equals("*")) {
                // "ALL" means every field in the catalog.
                int[] all = new int[catalog.size()];
                for (int i = 0; i < all.length; i++) {
                    all[i] = i;
                }
                fields = all;
                return;
            }

            String[] names = fieldList.split(",");
            int[] idx = new int[names.length];
            int count = 0;
            for (String name : names) {
                // Ignore unknown field names.
                Integer index = catalog.indexOf(name.trim());
                if (index != null) {
                    idx[count++] = index;
                }
            }
            fields = Arrays.copyOf(idx, count);
        }

        @Override
        public void close() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            sessions.remove(this);
        }
    }
}
