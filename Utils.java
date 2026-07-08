/**
 * Utils.java
 *
 * A stateless utility class containing static helper/validation methods
 * used across the application. Keeping validation logic here (rather than
 * scattered inside the GUI class) keeps PhoneDirectoryGUI focused purely
 * on presentation, following the Single Responsibility Principle.
 */
public final class Utils {

    // Regex: exactly 10 digits, no letters/symbols/spaces.
    private static final String PHONE_REGEX = "^[0-9]{10}$";

    // Private constructor - this is a utility class and should never be instantiated.
    private Utils() {
    }

    /**
     * Checks that a name is non-null and not just whitespace.
     */
    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty();
    }

    /**
     * Checks that a phone number is non-null, digits only, and exactly 10 digits long.
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && phone.trim().matches(PHONE_REGEX);
    }

    /**
     * Normalizes a name: trims whitespace and collapses internal
     * multiple spaces into a single space, then title-cases each word.
     */
    public static String normalizeName(String name) {
        if (name == null) return "";
        String trimmed = name.trim().replaceAll("\\s+", " ");
        StringBuilder result = new StringBuilder();
        for (String word : trimmed.split(" ")) {
            if (word.isEmpty()) continue;
            result.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
        }
        return result.toString().trim();
    }

    /**
     * Strips whitespace from a phone number string.
     */
    public static String normalizePhone(String phone) {
        return phone == null ? "" : phone.trim();
    }

    /**
     * Returns the current timestamp formatted as HH:mm:ss for status bar / log messages.
     */
    public static String currentTimestamp() {
        return new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
    }
}
