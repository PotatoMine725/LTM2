package server;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GridLayout;

public class ServerFrame extends JFrame {
    private final JTextField portField = new JTextField(String.valueOf(shared.Protocol.DEFAULT_PORT));
    private final JButton startButton = new JButton("Start");
    private final JButton stopButton = new JButton("Stop");
    private final JLabel statusLabel = new JLabel("Stopped");
    private final JTextArea logArea = new JTextArea();
    private final JTextArea messageArea = new JTextArea();
    private final ChatServer server;

    public ServerFrame() {
        super("TCP Chat Server");
        this.server = new ChatServer(this::appendLog, this::appendMessage, this::setStatus);
        buildUi();
        wireEvents();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JPanel top = new JPanel(new GridLayout(2, 2, 8, 8));
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        top.add(new JLabel("Port:"));
        top.add(portField);
        top.add(startButton);
        top.add(stopButton);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        statusPanel.add(new JLabel("Status:"), BorderLayout.WEST);
        statusPanel.add(statusLabel, BorderLayout.CENTER);

        logArea.setEditable(false);
        messageArea.setEditable(false);

        JPanel center = new JPanel(new GridLayout(1, 2, 8, 8));
        center.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        center.add(new JScrollPane(logArea));
        center.add(new JScrollPane(messageArea));

        add(top, BorderLayout.NORTH);
        add(statusPanel, BorderLayout.PAGE_START);
        add(center, BorderLayout.CENTER);
    }

    private void wireEvents() {
        startButton.addActionListener(e -> {
            int port = Integer.parseInt(portField.getText().trim());
            server.start(port);
        });
        stopButton.addActionListener(e -> server.stop());
    }

    private void appendLog(String text) {
        SwingUtilities.invokeLater(() -> logArea.append(text + "\n"));
    }

    private void appendMessage(String text) {
        SwingUtilities.invokeLater(() -> messageArea.append(text + "\n"));
    }

    private void setStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }
}
