import java.util.*;
import java.io.*;

public class Main {
    static ArrayList<Item> list = new ArrayList<>();
    static Scanner sc = new Scanner(System.in);
    static int idCounter = 100;

    public static void main(String[] args) {
        loadData();

        while (true) {
            System.out.println("\n  CAMPUS LOST & FOUND  ");
            System.out.println("1. Add Lost Item");
            System.out.println("2. Add Found Item");
            System.out.println("3. Show All Items");
            System.out.println("4. Search Items");
            System.out.println("5. Find Matches");
            System.out.println("6. Claim Item");
            System.out.println("7. Launch Web Frontend UI (Server)");
            System.out.println("8. Exit");

            if (!sc.hasNextInt()) break;
            int choice = sc.nextInt();
            sc.nextLine(); 

            if (choice == 1) {
                addLostItem();
            } else if (choice == 2) {
                addFoundItem();
            } else if (choice == 3) {
                showAll();
            } else if (choice == 4) {
                searchItem();
            } else if (choice == 5) {
                findMatches();
            } else if (choice == 6) {
                claimItem();
            } else if (choice == 7) {
                try {
                    Server.main(new String[0]);
                } catch (Exception e) {
                    System.out.println("Error launching web server: " + e.getMessage());
                }
            } else if (choice == 8) {
                System.out.println("Exiting program.");
                break;
            } else {
                System.out.println("Invalid choice. Please choose 1 to 8.");
            }
        }
    }

    static void addLostItem() {
        System.out.println("\n   Report a Lost Item. ");
        System.out.print("Enter Title: ");
        String title = sc.nextLine();

        System.out.print("Enter Category: ");
        String category = sc.nextLine();

        System.out.print("Enter Location Lost: ");
        String location = sc.nextLine();

        System.out.print("Enter Date (YYYY-MM-DD): ");
        String date = sc.nextLine();

        System.out.print("Enter Description: ");
        String desc = sc.nextLine();

        System.out.print("Enter Owner Name: ");
        String name = sc.nextLine();

        System.out.print("Enter Phone: ");
        String phone = sc.nextLine();

        int id = ++idCounter;

        // Upcasting: LostItem added to ArrayList<Item>
        list.add(new LostItem(id, title, category, location, date, desc, name, phone, "Open"));
        saveData();
        System.out.println("Lost Item #" + id + " added successfully!");
    }

    static void addFoundItem() {
        System.out.println("\n   Report a Found Item   ");
        System.out.print("Enter Title: ");
        String title = sc.nextLine();

        System.out.print("Enter Category: ");
        String category = sc.nextLine();

        System.out.print("Enter Location Found: ");
        String location = sc.nextLine();

        System.out.print("Enter Date (YYYY-MM-DD): ");
        String date = sc.nextLine();

        System.out.print("Enter Description: ");
        String desc = sc.nextLine();

        System.out.print("Enter Finder Name: ");
        String name = sc.nextLine();

        System.out.print("Enter Phone: ");
        String phone = sc.nextLine();

        System.out.print("Enter Storage Place (e.g. Security Desk): ");
        String place = sc.nextLine();

        int id = ++idCounter;

        // Upcasting: FoundItem added to ArrayList<Item>
        list.add(new FoundItem(id, title, category, location, date, desc, name, phone, "Open", place));
        saveData();
        System.out.println("Found Item #" + id + " added successfully!");
    }

    // 3. Show All Items
    static void showAll() {
        System.out.println("\n  All Campus Items (" + list.size() + ")  ");
        if (list.isEmpty()) {
            System.out.println("No items in list.");
            return;
        }

        // Polymorphism: item.show() calls the subclass method
        for (Item item : list) {
            item.show();
        }
    }

    static void searchItem() {
        System.out.print("Enter search keyword or category: ");
        String key = sc.nextLine().toLowerCase().trim();

        int count = 0;
        for (Item item : list) {
            // String Handling: equalsIgnoreCase and contains
            if (item.category.equalsIgnoreCase(key) || 
                item.location.toLowerCase().contains(key) || 
                item.title.toLowerCase().contains(key) ||
                item.description.toLowerCase().contains(key)) {
                item.show();
                count++;
            }
        }

        if (count == 0) {
            System.out.println("No matching items found.");
        } else {
            System.out.println("Found " + count + " item.");
        }
    }

    static void findMatches() {
        System.out.println("\n  Potential Matches  ");
        int count = 0;

        for (Item lost : list) {
            if (!lost.type.equals("LOST") || lost.status.equals("Claimed")) continue;

            for (Item found : list) {
                if (!found.type.equals("FOUND") || found.status.equals("Claimed")) continue;

                // Match condition: same category AND similar location
                boolean sameCategory = lost.category.equalsIgnoreCase(found.category);
                boolean sameLocation = lost.location.toLowerCase().contains(found.location.toLowerCase()) || 
                                       found.location.toLowerCase().contains(lost.location.toLowerCase());

                if (sameCategory && sameLocation) {
                    count++;
                    System.out.println("MATCH FOUND!");
                    System.out.println("Lost Item : #" + lost.id + " - " + lost.title + " (Owner: " + lost.contactName + ", Phone: " + lost.contactPhone + ")");
                    System.out.println("Found Item: #" + found.id + " - " + found.title + " (Finder: " + found.contactName + ", Kept at: " + ((FoundItem)found).storagePlace + ")");
                    System.out.println("    ");
                }
            }
        }

        if (count == 0) {
            System.out.println("No matches found right now.");
        }
    }

    static void claimItem() {
        System.out.print("Enter Item ID to claim: ");
        int id = sc.nextInt();
        sc.nextLine();

        if (claimItemById(id)) {
            System.out.println("Item #" + id + " marked as Claimed!");
        } else {
            System.out.println("Item #" + id + " not found!");
        }
    }

    public static boolean claimItemById(int id) {
        for (Item item : list) {
            if (item.id == id) {
                item.status = "Claimed";
                saveData();
                return true;
            }
        }
        return false;
    }

    public static boolean deleteItemById(int id) {
        Iterator<Item> it = list.iterator();
        while (it.hasNext()) {
            if (it.next().id == id) {
                it.remove();
                saveData();
                return true;
            }
        }
        return false;
    }

    public static File getStorageFile() {
        File f1 = new File("items.txt");
        if (f1.exists()) return f1;
        File f2 = new File("project/items.txt");
        if (f2.exists()) return f2;
        return f1;
    }

    // 7. File Handling
    public static void saveData() {
        try {
            File file = getStorageFile();
            FileWriter fw = new FileWriter(file);
            for (Item item : list) {
                fw.write(item.toFile());
            }
            fw.close();
        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
        }
    }

    // 8. File Handling: Load items 
    public static void loadData() {
        list.clear();
        File file = getStorageFile();
        if (!file.exists()) return;

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            String type = "", title = "", category = "", location = "", date = "", desc = "", name = "", phone = "", status = "", storage = "";
            int id = 0;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.equals("---")) {
                    if (id > idCounter) idCounter = id;

                    if ("LOST".equalsIgnoreCase(type)) {
                        list.add(new LostItem(id, title, category, location, date, desc, name, phone, status));
                    } else if ("FOUND".equalsIgnoreCase(type)) {
                        list.add(new FoundItem(id, title, category, location, date, desc, name, phone, status, storage));
                    }

                    // Reset variables for next item
                    type = ""; title = ""; category = ""; location = ""; date = ""; desc = ""; name = ""; phone = ""; status = ""; storage = "";
                    id = 0;
                } else {
                    int colon = line.indexOf(": ");
                    if (colon != -1) {
                        String key = line.substring(0, colon).trim();
                        String val = line.substring(colon + 2).trim();

                        if (key.equalsIgnoreCase("Type")) type = val;
                        else if (key.equalsIgnoreCase("ID")) id = Integer.parseInt(val);
                        else if (key.equalsIgnoreCase("Title")) title = val;
                        else if (key.equalsIgnoreCase("Category")) category = val;
                        else if (key.equalsIgnoreCase("Location")) location = val;
                        else if (key.equalsIgnoreCase("Date")) date = val;
                        else if (key.equalsIgnoreCase("Description")) desc = val;
                        else if (key.equalsIgnoreCase("Contact")) name = val;
                        else if (key.equalsIgnoreCase("Phone")) phone = val;
                        else if (key.equalsIgnoreCase("Status")) status = val;
                        else if (key.equalsIgnoreCase("Storage")) storage = val;
                    }
                }
            }
            br.close();
        } catch (Exception e) {
            System.out.println("Error loading file: " + e.getMessage());
        }
    }
}
