package client;

import javax.swing.SwingUtilities;

public class ClientLauncher {
    public static void main(String[] args) {
        LoginFrame.applyLaf();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
