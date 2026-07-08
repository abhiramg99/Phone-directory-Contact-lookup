import java.util.*;

/**
 * ContactManager.java
 *
 * The heart of the application's business logic. This class demonstrates
 * the core Collections Framework concept required by the problem statement:
 * replacing a slow, linear "scan the list" search with instant O(1)
 * average-case HashMap lookups.
 *
 * Two HashMaps are kept perfectly in sync at all times:
 *
 *   1. phoneToName : HashMap<String, String>
 *        Phone Number -> Owner Name
 *        Enables instant "search by phone number" lookups.
 *
 *   2. nameToPhones : HashMap<String, ArrayList<String>>
 *        Name -> List of Phone Numbers
 *        Enables instant "reverse lookup" (search by name), and supports
 *        the requirement that a single person can own multiple numbers.
 *
 * Every mutating method (add/update/delete) updates BOTH maps together,
 * so they never drift out of sync with one another.
 */
public class ContactManager {

    private final HashMap<String, String> phoneToName;
    private final HashMap<String, ArrayList<String>> nameToPhones;

    // Simple running counters used for the Statistics panel in the GUI.
    private int searchCount;
    private String lastAddedPhone;

    public ContactManager() {
        this.phoneToName = new HashMap<>();
        this.nameToPhones = new HashMap<>();
        this.searchCount = 0;
        this.lastAddedPhone = null;
    }

    // ----------------------------------------------------------------
    //  ADD
    // ----------------------------------------------------------------

    /**
     * Adds a new contact to both HashMaps.
     *
     * @throws IllegalArgumentException if the name/phone are invalid,
     *                                   or the phone number is already taken.
     */
    public void addContact(String name, String phone) {
        name = Utils.normalizeName(name);
        phone = Utils.normalizePhone(phone);

        if (!Utils.isValidName(name)) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        if (!Utils.isValidPhone(phone)) {
            throw new IllegalArgumentException("Phone number must be exactly 10 digits.");
        }
        if (phoneToName.containsKey(phone)) {
            throw new IllegalArgumentException("This phone number already exists for \""
                    + phoneToName.get(phone) + "\". Duplicate numbers are not allowed.");
        }

        // Update Map 1: phone -> name
        phoneToName.put(phone, name);

        // Update Map 2: name -> list of phones
        nameToPhones.computeIfAbsent(name, k -> new ArrayList<>()).add(phone);

        lastAddedPhone = phone;
    }

    // ----------------------------------------------------------------
    //  UPDATE
    // ----------------------------------------------------------------

    /**
     * Updates an existing contact identified by its current phone number.
     * Handles every combination of name-changed / phone-changed cleanly,
     * keeping both maps synchronized.
     *
     * @param oldPhone the phone number currently stored (the lookup key)
     * @param newName  the (possibly unchanged) new name
     * @param newPhone the (possibly unchanged) new phone number
     * @throws IllegalArgumentException on invalid input, missing contact,
     *                                   or duplicate phone conflict
     */
    public void updateContact(String oldPhone, String newName, String newPhone) {
        oldPhone = Utils.normalizePhone(oldPhone);
        newName = Utils.normalizeName(newName);
        newPhone = Utils.normalizePhone(newPhone);

        if (!phoneToName.containsKey(oldPhone)) {
            throw new IllegalArgumentException("Original contact not found.");
        }
        if (!Utils.isValidName(newName)) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        if (!Utils.isValidPhone(newPhone)) {
            throw new IllegalArgumentException("Phone number must be exactly 10 digits.");
        }
        // If the phone number is changing, make sure the new one isn't already taken
        // by a *different* contact.
        if (!newPhone.equals(oldPhone) && phoneToName.containsKey(newPhone)) {
            throw new IllegalArgumentException("Phone number " + newPhone + " is already in use.");
        }

        String oldName = phoneToName.get(oldPhone);

        // Remove the old phone entry from the old owner's phone list.
        ArrayList<String> oldOwnerPhones = nameToPhones.get(oldName);
        if (oldOwnerPhones != null) {
            oldOwnerPhones.remove(oldPhone);
            if (oldOwnerPhones.isEmpty()) {
                nameToPhones.remove(oldName);
            }
        }
        phoneToName.remove(oldPhone);

        // Insert the new/updated entry into both maps.
        phoneToName.put(newPhone, newName);
        nameToPhones.computeIfAbsent(newName, k -> new ArrayList<>()).add(newPhone);
    }

    // ----------------------------------------------------------------
    //  DELETE
    // ----------------------------------------------------------------

    /**
     * Deletes the contact with the given phone number from both maps.
     *
     * @return true if a contact was found and removed, false otherwise
     */
    public boolean deleteContact(String phone) {
        phone = Utils.normalizePhone(phone);
        String name = phoneToName.remove(phone);
        if (name == null) {
            return false;
        }
        ArrayList<String> phones = nameToPhones.get(name);
        if (phones != null) {
            phones.remove(phone);
            if (phones.isEmpty()) {
                nameToPhones.remove(name);
            }
        }
        return true;
    }

    // ----------------------------------------------------------------
    //  SEARCH
    // ----------------------------------------------------------------

    /**
     * Instant O(1) reverse lookup: given a phone number, find the owner's name.
     */
    public String searchByPhone(String phone) {
        searchCount++;
        return phoneToName.get(Utils.normalizePhone(phone));
    }

    /**
     * Instant O(1) lookup: given a name, find every phone number they own.
     * Performs a case-insensitive partial match across all stored names so
     * the live-search feature in the GUI feels natural to use.
     */
    public List<Contact> searchByName(String name) {
        searchCount++;
        List<Contact> results = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) {
            return results;
        }
        String query = name.trim().toLowerCase();
        for (Map.Entry<String, ArrayList<String>> entry : nameToPhones.entrySet()) {
            if (entry.getKey().toLowerCase().contains(query)) {
                for (String phone : entry.getValue()) {
                    results.add(new Contact(entry.getKey(), phone));
                }
            }
        }
        results.sort(Comparator.comparing(Contact::getName));
        return results;
    }

    /**
     * Used by the live-search bar: filters across BOTH name and phone number
     * simultaneously so typing digits or letters both work.
     */
    public List<Contact> liveFilter(String query) {
        List<Contact> all = getAllContacts();
        if (query == null || query.trim().isEmpty()) {
            return all;
        }
        String q = query.trim().toLowerCase();
        List<Contact> filtered = new ArrayList<>();
        for (Contact c : all) {
            if (c.getName().toLowerCase().contains(q) || c.getPhoneNumber().contains(q)) {
                filtered.add(c);
            }
        }
        return filtered;
    }

    // ----------------------------------------------------------------
    //  READ ALL / STATS
    // ----------------------------------------------------------------

    /**
     * Returns every contact as a flat, name-sorted list, suitable for
     * populating the JTable directly.
     */
    public List<Contact> getAllContacts() {
        List<Contact> all = new ArrayList<>();
        for (Map.Entry<String, String> entry : phoneToName.entrySet()) {
            all.add(new Contact(entry.getValue(), entry.getKey()));
        }
        all.sort(Comparator.comparing(Contact::getName).thenComparing(Contact::getPhoneNumber));
        return all;
    }

    public boolean phoneExists(String phone) {
        return phoneToName.containsKey(Utils.normalizePhone(phone));
    }

    public int getTotalContacts() {
        return phoneToName.size();
    }

    public int getTotalUniquePeople() {
        return nameToPhones.size();
    }

    public int getSearchCount() {
        return searchCount;
    }

    public String getLastAddedPhone() {
        return lastAddedPhone;
    }

    /**
     * Wipes both maps and reloads them from a fresh list, e.g. after a
     * bulk CSV import or on initial application startup.
     */
    public void loadFromList(List<Contact> contacts) {
        phoneToName.clear();
        nameToPhones.clear();
        for (Contact c : contacts) {
            String name = Utils.normalizeName(c.getName());
            String phone = Utils.normalizePhone(c.getPhoneNumber());
            if (Utils.isValidName(name) && Utils.isValidPhone(phone) && !phoneToName.containsKey(phone)) {
                phoneToName.put(phone, name);
                nameToPhones.computeIfAbsent(name, k -> new ArrayList<>()).add(phone);
            }
        }
    }

    /**
     * Merges a list of contacts into the existing data (used for CSV import),
     * skipping any duplicate phone numbers rather than overwriting them.
     *
     * @return the number of contacts successfully imported
     */
    public int mergeFromList(List<Contact> contacts) {
        int imported = 0;
        for (Contact c : contacts) {
            try {
                addContact(c.getName(), c.getPhoneNumber());
                imported++;
            } catch (IllegalArgumentException ignored) {
                // Skip duplicates / invalid rows silently during bulk import.
            }
        }
        return imported;
    }
}
