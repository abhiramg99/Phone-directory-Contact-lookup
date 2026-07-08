import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * FileManager.java
 *
 * Handles all disk I/O for the application. Contacts are persisted to a
 * simple CSV-style text file (contacts.txt) in the format:
 *
 *      Name,PhoneNumber
 *
 * one contact per line. This class is intentionally kept separate from
 * ContactManager so that the in-memory data structure logic and the
 * file-persistence logic are decoupled (separation of concerns).
 */
public class FileManager {

    /** Default data file used for normal load/save operations. */
    public static final String DATA_FILE = "contacts.txt";

    /**
     * Loads all contacts from the given file.
     * If the file does not exist yet, an empty list is returned and the
     * file will simply be created the first time contacts are saved.
     *
     * @param filePath path to the data file
     * @return list of Contact objects read from disk
     */
    public List<Contact> loadContacts(String filePath) {
        List<Contact> contacts = new ArrayList<>();
        File file = new File(filePath);

        if (!file.exists()) {
            return contacts; // Nothing to load yet - fresh install.
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    String name = parts[0].trim();
                    String phone = parts[1].trim();
                    if (!name.isEmpty() && !phone.isEmpty()) {
                        contacts.add(new Contact(name, phone));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading contacts from file: " + e.getMessage());
        }

        return contacts;
    }

    /**
     * Overwrites the data file with the full, current contact list.
     * Called automatically after every Add / Update / Delete so the file
     * on disk always mirrors the in-memory state.
     *
     * @param filePath path to the data file
     * @param contacts the complete, current list of contacts to persist
     */
    public void saveContacts(String filePath, List<Contact> contacts) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
            for (Contact c : contacts) {
                writer.write(c.getName() + "," + c.getPhoneNumber());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving contacts to file: " + e.getMessage());
        }
    }

    /**
     * Exports the given contact list to an arbitrary CSV file, complete with
     * a header row. Used by the "Export to CSV" feature (distinct from the
     * internal save format, though the body format is the same).
     */
    public void exportToCSV(String filePath, List<Contact> contacts) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
            writer.write("Name,PhoneNumber");
            writer.newLine();
            for (Contact c : contacts) {
                writer.write(escapeCSV(c.getName()) + "," + escapeCSV(c.getPhoneNumber()));
                writer.newLine();
            }
        }
    }

    /**
     * Imports contacts from a CSV file that has a header row (Name,PhoneNumber).
     * Malformed lines are silently skipped.
     */
    public List<Contact> importFromCSV(String filePath) throws IOException {
        List<Contact> contacts = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    // Skip header row if it looks like one.
                    if (line.toLowerCase().contains("name") && line.toLowerCase().contains("phone")) {
                        continue;
                    }
                }
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    String name = parts[0].trim();
                    String phone = parts[1].trim();
                    if (!name.isEmpty() && !phone.isEmpty()) {
                        contacts.add(new Contact(name, phone));
                    }
                }
            }
        }
        return contacts;
    }

    private String escapeCSV(String value) {
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
