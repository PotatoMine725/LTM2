package client;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.imageio.ImageIO;

public class ChatFrame extends JFrame {

    private static final int IMG_MAX_W = 380;
    private static final int IMG_MAX_H = 280;

    private final ChatClient chatClient;
    private final String username;
    private final JTextPane chatPane = new JTextPane();
    private final StyledDocument doc = chatPane.getStyledDocument();
    private final JTextField messageField = new JTextField();
    private final JButton sendButton = new JButton("Send");
    private final JButton fileButton = new JButton("Attach");
    private final JButton logoutButton = new JButton("Logout");

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // Text styles
    private final Style baseStyle;
    private final Style youStyle;
    private final Style serverStyle;
    private final Style captionStyle;
    private final Style historyStyle;

    public ChatFrame(ChatClient chatClient, String username) {
        super("LTM2 Chat — " + username);
        this.chatClient = chatClient;
        this.username = username;
        baseStyle    = initStyle("base",    null,        Color.DARK_GRAY, false);
        youStyle     = initStyle("you",     baseStyle,   new Color(0, 90, 200), true);
        serverStyle  = initStyle("server",  baseStyle,   new Color(60, 60, 60), false);
        captionStyle = initStyle("caption", baseStyle,   Color.GRAY, false);
        StyleConstants.setFontSize(captionStyle, 11);
        historyStyle = initStyle("history", baseStyle,   new Color(130, 130, 130), false);
        StyleConstants.setItalic(historyStyle, true);
        chatClient.setStatusHandler(this::appendMessage);
        buildUi();
        wireEvents();
        renderHistory();
    }

    private Style initStyle(String name, Style parent, Color color, boolean bold) {
        Style style = chatPane.addStyle(name, parent);
        StyleConstants.setForeground(style, color);
        StyleConstants.setBold(style, bold);
        StyleConstants.setFontFamily(style, Font.SANS_SERIF);
        StyleConstants.setFontSize(style, 13);
        StyleConstants.setSpaceBelow(style, 2f);
        return style;
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(860, 620);
        setLocationRelativeTo(null);

        // Header
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        JLabel userLabel = new JLabel("Logged in as  " + username);
        userLabel.setFont(userLabel.getFont().deriveFont(Font.BOLD, 13f));
        header.add(userLabel, BorderLayout.WEST);
        header.add(logoutButton, BorderLayout.EAST);

        // Chat pane
        chatPane.setEditable(false);
        chatPane.setMargin(new Insets(8, 10, 8, 10));
        JScrollPane chatScroll = new JScrollPane(chatPane);
        chatScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0,
                UIManager.getColor("Component.borderColor") != null
                        ? UIManager.getColor("Component.borderColor")
                        : Color.LIGHT_GRAY));

        // Input row
        messageField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        JPanel inputPanel = new JPanel(new BorderLayout(6, 0));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
        inputPanel.add(messageField, BorderLayout.CENTER);
        JPanel inputButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        inputButtons.add(fileButton);
        inputButtons.add(sendButton);
        inputPanel.add(inputButtons, BorderLayout.EAST);

        JPanel root = new JPanel(new BorderLayout());
        root.add(header, BorderLayout.NORTH);
        root.add(chatScroll, BorderLayout.CENTER);
        root.add(inputPanel, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void wireEvents() {
        sendButton.addActionListener(e -> handleSend());
        messageField.addActionListener(e -> handleSend());
        fileButton.addActionListener(e -> handleFile());
        logoutButton.addActionListener(e -> handleLogout());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleLogout();
            }
        });
    }

    private void handleSend() {
        String text = messageField.getText().trim();
        if (!text.isEmpty()) {
            appendStyledText("[You]: " + text + "\n", youStyle);
            chatClient.sendText(text);
            messageField.setText("");
            messageField.requestFocus();
        }
    }

    private void handleFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            appendImage(file);
            chatClient.sendImage(file);
        }
    }

    private void handleLogout() {
        chatClient.logout();
        dispose();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    // Called by ChatClient's statusHandler for incoming server messages
    private void appendMessage(String text) {
        // Filter out the redundant send-confirmation for images (image is already shown inline)
        if (text.startsWith("[You sent:")) return;
        SwingUtilities.invokeLater(() -> appendStyledText(text + "\n", serverStyle));
    }

    private void appendStyledText(String text, Style style) {
        try {
            doc.insertString(doc.getLength(), text, style);
            chatPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException ignored) {
        }
    }

    private void appendImage(File file) {
        SwingUtilities.invokeLater(() -> {
            try {
                BufferedImage raw = ImageIO.read(file);
                if (raw == null) {
                    appendStyledText("[Cannot read image: " + file.getName() + "]\n", captionStyle);
                    return;
                }
                Image scaled = scaleDown(raw, IMG_MAX_W, IMG_MAX_H);
                ImageIcon icon = new ImageIcon(scaled);

                appendStyledText("[You sent an image]\n", youStyle);

                Style imgStyle = chatPane.addStyle("img-" + System.nanoTime(), null);
                StyleConstants.setIcon(imgStyle, icon);
                try {
                    doc.insertString(doc.getLength(), " ", imgStyle);
                    doc.insertString(doc.getLength(), "\n" + file.getName() + "\n", captionStyle);
                } catch (BadLocationException ignored) {
                }
                chatPane.setCaretPosition(doc.getLength());
            } catch (IOException ex) {
                appendStyledText("[Failed to load image: " + file.getName() + "]\n", captionStyle);
            }
        });
    }

    private void renderHistory() {
        List<String[]> history = chatClient.getPendingHistory();
        if (history.isEmpty()) return;
        appendStyledText("─────── Lịch sử chat ───────\n", captionStyle);
        for (String[] item : history) {
            String type = item[0], sender = item[1], content = item[2];
            String time = LocalDateTime
                    .ofInstant(Instant.ofEpochMilli(Long.parseLong(item[3])), ZoneId.systemDefault())
                    .format(TIME_FMT);
            if ("IMAGE".equals(type)) {
                appendStyledText("[" + time + "] " + sender + " đã gửi ảnh: " + content + "\n", historyStyle);
            } else {
                appendStyledText("[" + time + "] " + sender + ": " + content + "\n", historyStyle);
            }
        }
        appendStyledText("────────────────────────────\n", captionStyle);
        chatClient.clearPendingHistory();
    }

    private static Image scaleDown(BufferedImage src, int maxW, int maxH) {
        int w = src.getWidth(), h = src.getHeight();
        double scale = Math.min((double) maxW / w, (double) maxH / h);
        if (scale >= 1.0) return src;
        int newW = (int) (w * scale);
        int newH = (int) (h * scale);
        Image scaled = src.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        // Flush to finish rendering before returning
        new ImageIcon(scaled).getImage().flush();
        return scaled;
    }
}
