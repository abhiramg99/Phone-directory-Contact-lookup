import javax.swing.SwingUtilities;

/**
 * Main.java
 *
 * Application entry point. Launches the Swing GUI on the Event Dispatch
 * Thread (EDT), which is required for thread-safe Swing rendering.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PhoneDirectoryGUI gui = new PhoneDirectoryGUI();
            gui.setVisible(true);
        });
    }
}
