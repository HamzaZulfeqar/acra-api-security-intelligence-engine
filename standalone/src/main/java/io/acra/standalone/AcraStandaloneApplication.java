package io.acra.standalone;

import io.acra.standalone.http.StandaloneServer;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.awt.Desktop;
import java.io.IOException;
import java.net.BindException;
import java.nio.file.Path;

public final class AcraStandaloneApplication {
    private AcraStandaloneApplication() {}

    public static void main(String[] args) throws Exception {
        Config config = Config.parse(args);
        LocalWorkspaceStore store = new LocalWorkspaceStore(config.home());
        StandaloneServer server = createServer(store, config.port(), config.portExplicit());
        server.start();

        System.out.println("ACRA Standalone Security Workbench");
        System.out.println("Java runtime: " + System.getProperty("java.version"));
        System.out.println("Management bind: 127.0.0.1");
        System.out.println("Local UI: " + server.baseUri());
        if (!config.portExplicit() && config.port() != 0 && server.port() != config.port()) {
            System.out.println("Startup recovery: default port " + config.port()
                    + " was unavailable; selected loopback port " + server.port() + ".");
        }
        System.out.println("Workspace: " + store.root());
        System.out.println("Burp required: false");
        System.out.println("Burp bridge: optional loopback-only handoff");
        System.out.println("Active validation: loopback LAB + CONTROLLED_LAB only");
        System.out.println("Press Ctrl+C to stop.");

        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "acra-standalone-shutdown"));
        if (!config.noBrowser()) openBrowser(server);
    }

    private static StandaloneServer createServer(
            LocalWorkspaceStore store,
            int requestedPort,
            boolean explicit
    ) throws IOException {
        try {
            return new StandaloneServer(store, requestedPort);
        } catch (BindException ex) {
            if (explicit) throw ex;
            return new StandaloneServer(store, 0);
        }
    }

    private static void openBrowser(StandaloneServer server) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(server.baseUri());
                return;
            }
            System.err.println("Browser auto-open unavailable; open " + server.baseUri() + " manually.");
        } catch (Exception ex) {
            System.err.println("Browser auto-open unavailable; open " + server.baseUri() + " manually.");
        }
    }

    record Config(int port, boolean portExplicit, Path home, boolean noBrowser) {
        static Config parse(String[] args) {
            int port = 8787;
            boolean portExplicit = false;
            Path home = Path.of(System.getProperty("user.home"), ".acra");
            boolean noBrowser = false;

            for (String arg : args) {
                if (arg.startsWith("--port=")) {
                    port = Integer.parseInt(arg.substring("--port=".length()));
                    if (port < 0 || port > 65535) throw new IllegalArgumentException("port out of range");
                    portExplicit = true;
                } else if (arg.startsWith("--home=")) {
                    home = Path.of(arg.substring("--home=".length()));
                } else if (arg.equals("--no-browser")) {
                    noBrowser = true;
                } else {
                    throw new IllegalArgumentException("unknown argument: " + arg);
                }
            }
            return new Config(port, portExplicit, home, noBrowser);
        }
    }
}
