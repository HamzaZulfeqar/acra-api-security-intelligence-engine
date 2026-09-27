package io.acra.burp.ui;

import io.acra.burp.bridge.BridgeRequestSerializer;
import io.acra.burp.bridge.StandaloneBridgeClient;
import io.acra.burp.bridge.StandaloneBridgeResult;
import io.acra.burp.traffic.TrafficIntelligencePipeline;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.List;

@SuppressWarnings("serial")
public final class StandaloneBridgePanel extends JPanel {
    private final TrafficIntelligencePipeline pipeline;
    private final BridgeRequestSerializer serializer = new BridgeRequestSerializer();
    private final JTextField url = new JTextField("http://127.0.0.1:8787/");
    private final JTextField projectId = new JTextField();
    private final JTextField targetId = new JTextField();
    private final JComboBox<String> transaction = new JComboBox<>();
    private final JLabel status = new JLabel("Standalone bridge is optional and idle.");
    private final JButton probe = new JButton("Probe Standalone");
    private final JButton open = new JButton("Open Standalone");
    private final JButton send = new JButton("Send Selected Transaction");
    private List<String> transactionIds = List.of();

    public StandaloneBridgePanel(TrafficIntelligencePipeline pipeline) {
        super(new BorderLayout(8, 8));
        this.pipeline = java.util.Objects.requireNonNull(pipeline);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Standalone URL"));
        form.add(url);
        form.add(new JLabel("Project ID"));
        form.add(projectId);
        form.add(new JLabel("Target ID"));
        form.add(targetId);
        form.add(new JLabel("Observed transaction"));
        form.add(transaction);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(probe);
        actions.add(open);
        actions.add(send);

        JTextArea boundary = new JTextArea(
                "Explicit handoff only. No automatic forwarding. The bridge refuses non-loopback standalone URLs.\n"
                + "Sending a transaction imports its raw request into the selected standalone project/target. "
                + "Standalone target-scope validation and evidence redaction remain authoritative.");
        boundary.setEditable(false);
        boundary.setLineWrap(true);
        boundary.setWrapStyleWord(true);
        boundary.setOpaque(false);

        JPanel north = new JPanel(new BorderLayout(8, 8));
        north.add(form, BorderLayout.NORTH);
        north.add(actions, BorderLayout.CENTER);
        north.add(boundary, BorderLayout.SOUTH);

        add(north, BorderLayout.NORTH);
        add(status, BorderLayout.SOUTH);

        probe.addActionListener(event -> probe());
        open.addActionListener(event -> openStandalone());
        send.addActionListener(event -> sendSelected());
        refresh();
    }

    public void refresh() {
        List<String> ids = pipeline.store().observations().stream()
                .map(value -> value.transactionId())
                .toList();
        if (ids.equals(transactionIds)) return;

        Object selected = transaction.getSelectedItem();
        transactionIds = ids;
        transaction.removeAllItems();
        for (int i = ids.size() - 1; i >= 0; i--) transaction.addItem(ids.get(i));
        if (selected != null && ids.contains(selected.toString())) transaction.setSelectedItem(selected);
    }

    private void probe() {
        setBusy(true, "Probing local standalone ACRA…");
        new SwingWorker<StandaloneBridgeResult, Void>() {
            @Override
            protected StandaloneBridgeResult doInBackground() {
                try {
                    return new StandaloneBridgeClient(url.getText()).probe();
                } catch (RuntimeException ex) {
                    return new StandaloneBridgeResult(false, safe(ex), 0, 0, 0);
                }
            }

            @Override
            protected void done() {
                try {
                    showResult(get());
                } catch (Exception ex) {
                    status.setText("Probe failed: " + safe(ex));
                } finally {
                    setBusy(false, null);
                }
            }
        }.execute();
    }

    private void openStandalone() {
        try {
            StandaloneBridgeClient client = new StandaloneBridgeClient(url.getText());
            if (!Desktop.isDesktopSupported()) {
                status.setText("Desktop browser integration is unavailable. Open " + client.baseUri() + " manually.");
                return;
            }
            Desktop.getDesktop().browse(client.baseUri());
            status.setText("Opened standalone ACRA: " + client.baseUri());
        } catch (IOException | RuntimeException ex) {
            status.setText("Unable to open standalone ACRA: " + safe(ex));
        }
    }

    private void sendSelected() {
        String txId = selectedTransaction();
        if (txId.isBlank()) {
            status.setText("Select a passive transaction first.");
            return;
        }
        var tx = pipeline.transaction(txId).orElse(null);
        if (tx == null) {
            status.setText("Selected transaction is no longer retained in the bounded bridge buffer.");
            return;
        }

        final String raw;
        try {
            raw = serializer.raw(tx.request());
        } catch (RuntimeException ex) {
            status.setText("Unable to prepare transaction: " + safe(ex));
            return;
        }

        String project = projectId.getText().strip();
        String target = targetId.getText().strip();
        setBusy(true, "Sending selected transaction to standalone ACRA…");
        new SwingWorker<StandaloneBridgeResult, Void>() {
            @Override
            protected StandaloneBridgeResult doInBackground() {
                try {
                    return new StandaloneBridgeClient(url.getText()).sendRawRequest(
                            project,
                            target,
                            "burp:" + txId,
                            raw);
                } catch (RuntimeException ex) {
                    return new StandaloneBridgeResult(false, safe(ex), 0, 0, 0);
                }
            }

            @Override
            protected void done() {
                try {
                    showResult(get());
                } catch (Exception ex) {
                    status.setText("Bridge handoff failed: " + safe(ex));
                } finally {
                    setBusy(false, null);
                }
            }
        }.execute();
    }

    private String selectedTransaction() {
        Object value = transaction.getSelectedItem();
        return value == null ? "" : value.toString();
    }

    private void showResult(StandaloneBridgeResult result) {
        if (!result.success()) {
            status.setText("Bridge: " + result.message());
            return;
        }
        String counts = result.observations() > 0
                ? " observations=" + result.observations()
                    + " uniqueEndpoints=" + result.uniqueEndpoints()
                    + " inventorySize=" + result.inventorySize()
                : "";
        status.setText("Bridge: " + result.message() + counts);
    }

    private void setBusy(boolean busy, String message) {
        probe.setEnabled(!busy);
        open.setEnabled(!busy);
        send.setEnabled(!busy);
        if (message != null) status.setText(message);
    }

    private static String safe(Exception ex) {
        Throwable cause = ex instanceof java.util.concurrent.ExecutionException && ex.getCause() != null
                ? ex.getCause() : ex;
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }
}
