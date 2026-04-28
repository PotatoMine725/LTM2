package server;

import javax.swing.SwingUtilities;

public class ServerApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerFrame().setVisible(true));
    }
}
