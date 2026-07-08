# 📞 Phone Directory System

A professional **Java Swing desktop CRM application** built for the engineering mini-project problem statement:

> **PS-71 — Phone Directory / Contact Lookup**
> Sector: Telecom · Domain: Telecom / CRM · Module D: Collections Framework (List, Set, Map)

Traditional contact-lookup tools scan a list from top to bottom, getting slower as the number of contacts grows (O(n) linear search). This application solves that problem using **Java's `HashMap`**, giving **instant, constant-time (O(1) average case) lookups** — both name → phone and phone → name — while still supporting people with multiple phone numbers.

---

## ✨ Features

| Category | Feature |
|---|---|
| **Core CRUD** | Add, Update, Delete contacts |
| **Instant Lookup** | Search by phone number → owner name (HashMap) |
| **Reverse Lookup** | Search by name → list of phone numbers (HashMap of ArrayLists) |
| **Multi-number support** | One person can own several phone numbers |
| **Duplicate prevention** | A phone number can never be assigned to two people |
| **Persistence** | Contacts auto-save to `contacts.txt` after every change, and auto-load on startup |
| **Live search** | Instantly filters the table as you type (no button needed) — `DocumentListener` |
| **JTable integration** | Click a row to populate the edit fields; double-click to edit; right-click for a context menu |
| **Validation** | Empty name/phone rejected, phone must be exactly 10 digits, duplicates blocked — all via `JOptionPane` error dialogs |
| **Statistics panel** | Total Contacts, Searches Performed, Recently Added |
| **CSV Import / Export** | Bulk-load contacts from a CSV file, or export the directory to CSV |
| **Print** | Print the visible directory table |
| **Dark Mode** | Toggle from the View menu |
| **Keyboard shortcuts** | `Ctrl+N` new contact, `Ctrl+S` save, `Ctrl+F` focus search, `Delete` remove selected row |
| **About / Help dialogs** | Built-in usage guide and app info |
| **Modern UI** | Dark-blue header, rounded buttons with hover effects, color-coded actions, tooltips, professional status bar |

---

## 🗂️ Folder Structure

```
PhoneDirectorySystem/
│
├── src/
│   ├── Main.java               → Application entry point (launches GUI on EDT)
│   ├── PhoneDirectoryGUI.java  → Swing UI: layout, events, styling (View + Controller)
│   ├── Contact.java            → Simple Name/Phone data model (POJO)
│   ├── ContactManager.java     → Business logic: the two HashMaps + CRUD + search
│   ├── FileManager.java        → File I/O: load/save contacts.txt, CSV import/export
│   └── Utils.java              → Static validation & formatting helpers
│
├── icons/                      → Placeholder folder for optional custom icon images
├── contacts.txt                → Auto-generated/auto-loaded data file (sample data included)
└── README.md                   → This file
```

### Architecture (MVC-like)

```
        ┌───────────────────┐
        │  PhoneDirectoryGUI │  <- View + Controller (Swing)
        └─────────┬──────────┘
                   │ calls
                   ▼
        ┌───────────────────┐
        │   ContactManager   │  <- Model / business logic
        │  (2 sync'd HashMaps)│
        └─────────┬──────────┘
                   │ delegates I/O
                   ▼
        ┌───────────────────┐
        │    FileManager     │  <- Persistence layer
        └───────────────────┘
```

**Core data structures** (in `ContactManager`):

```java
HashMap<String, String> phoneToName;              // Phone Number → Name   (instant reverse lookup)
HashMap<String, ArrayList<String>> nameToPhones;   // Name → [Phone Numbers] (supports multiple numbers/person)
```

Both maps are updated together inside every `addContact`, `updateContact`, and `deleteContact` call, so they can never drift out of sync.

---

## ▶️ How to Run

### Option A — Command Line (javac / java)

```bash
cd PhoneDirectorySystem
javac -d bin src/*.java
java -cp bin Main
```

### Option B — VS Code

1. Install the **"Extension Pack for Java"** from the VS Code Marketplace.
2. Open the `PhoneDirectorySystem` folder in VS Code (`File → Open Folder`).
3. Open `src/Main.java`.
4. Click the **▶ Run** button above `public static void main(...)`, or press `F5`.
5. VS Code will auto-compile and launch the app.

### Option C — IntelliJ IDEA

1. `File → Open` → select the `PhoneDirectorySystem` folder.
2. When prompted, let IntelliJ auto-detect it as a Java project (no build system needed — Maven/Gradle are NOT used).
3. Right-click `src` → **Mark Directory as → Sources Root** (if not already marked).
4. Open `Main.java`, click the green ▶ run icon next to `public static void main`.

> **No external libraries or build tools required** — pure Java SE + Swing (verified to compile cleanly with `javac` on JDK 21, zero errors/warnings).

---

## 🖥️ Expected Output

On launch, the app opens a **1000×720** window with:

1. A dark-blue header: *"📞 Phone Directory System"*
2. An input row with **Name** / **Phone** fields, a **live search bar**, and 8 color-coded action buttons (Add=green, Update/Display=blue, Search=orange, Delete/Exit=red, Clear=gray).
3. A sortable `JTable` listing all contacts (Name | Phone Number), pre-loaded with 3 sample contacts from `contacts.txt`.
4. A statistics row (Total Contacts / Searches Performed / Recently Added).
5. A status bar showing the last action performed and a timestamp.

### 📸 Screenshots

*(Placeholders — replace with real screenshots after running the app)*

- `screenshots/main-window.png` — Main dashboard view
- `screenshots/add-contact.png` — Adding a new contact
- `screenshots/search-results.png` — Search-by-name results highlighted
- `screenshots/dark-mode.png` — Dark mode enabled
- `screenshots/context-menu.png` — Right-click context menu on a row

---

## ✅ Validation Rules

| Rule | Enforced by |
|---|---|
| Name cannot be empty | `Utils.isValidName()` |
| Phone cannot be empty | `Utils.isValidPhone()` |
| Phone must be digits only | Regex `^[0-9]{10}$` |
| Phone must be exactly 10 digits | Same regex |
| No duplicate phone numbers | `ContactManager.addContact()` checks `phoneToName` before inserting |

All violations surface as a `JOptionPane.ERROR_MESSAGE` dialog with a clear explanation.

---

## ⌨️ Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| `Ctrl + N` | Clear fields & focus the Name field to add a new contact |
| `Ctrl + S` | Save (Update if a row is selected, otherwise Add) |
| `Ctrl + F` | Jump focus to the Live Search bar |
| `Delete` | Delete the currently selected row (with confirmation) |
| Right-click on row | Open context menu (Edit / Delete) |
| Double-click on row | Enter edit mode (fields pre-filled) |

---

## 🚀 Future Enhancements

- Contact groups / categories (Family, Work, Emergency)
- Profile pictures per contact
- SQLite/JDBC-backed storage instead of flat file, for larger datasets
- Undo/redo stack for accidental deletes
- Cloud sync / multi-device support
- Call-log integration
- Password-protected admin mode

---

## 🧑‍💻 Tech Stack

- **Language:** Java (SE, JDK 21 compatible; compiles fine on JDK 8+)
- **UI:** Java Swing (`JFrame`, `JTable`, `JOptionPane`, custom rounded `JButton`)
- **Collections:** `HashMap`, `ArrayList`
- **Persistence:** Plain-text file I/O (`java.io`), no database
- **Build tools:** None — plain `javac`/`java`, no Maven/Gradle/Spring/JavaFX

---

*Built as a submission-ready engineering mini-project demonstrating the Java Collections Framework.*
