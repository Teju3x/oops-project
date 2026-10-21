public abstract class Item {
    int id;
    String type;        
    String title;
    String category;
    String location;
    String date;
    String description;
    String contactName;
    String contactPhone;
    String status;     

    public Item(int id, String type, String title, String category, String location, String date, String description, String contactName, String contactPhone, String status) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.category = category;
        this.location = location;
        this.date = date;
        this.description = description;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.status = status;
    }

    // Abstract method
    abstract void show();
    public String toFile() {
        return "Type: " + type + "\n"
             + "ID: " + id + "\n"
             + "Title: " + title + "\n"
             + "Category: " + category + "\n"
             + "Location: " + location + "\n"
             + "Date: " + date + "\n"
             + "Description: " + description + "\n"
             + "Contact: " + contactName + "\n"
             + "Phone: " + contactPhone + "\n"
             + "Status: " + status + "\n"
             + "---\n";
    }
}
//Inheritance 
class LostItem extends Item {
    public LostItem(int id, String title, String category, String location, String date, String description, String contactName, String contactPhone, String status) {
        super(id, "LOST", title, category, location, date, description, contactName, contactPhone, status);
    }

    void show() {
        System.out.println("[LOST ITEM] #" + id + ": " + title);
        System.out.println("Category : " + category + " | Location: " + location);
        System.out.println("Date     : " + date + " | Status: " + status);
        System.out.println("Details  : " + description);
        System.out.println("Contact  : " + contactName + " (" + contactPhone + ")");
    }
}

class FoundItem extends Item {
    String storagePlace;
    public FoundItem(int id, String title, String category, String location, String date, String description, String contactName, String contactPhone, String status, String storagePlace) {
        super(id, "FOUND", title, category, location, date, description, contactName, contactPhone, status);
        this.storagePlace = storagePlace;
    }
    void show() {
        System.out.println("[FOUND ITEM] #" + id + ": " + title);
        System.out.println("Category : " + category + " | Found at: " + location);
        System.out.println("Date     : " + date + " | Status: " + status);
        System.out.println("Kept at  : " + storagePlace);
        System.out.println("Details  : " + description);
        System.out.println("Finder   : " + contactName + " (" + contactPhone + ")");
    }
    public String toFile() {
        return "Type: " + type + "\n"
             + "ID: " + id + "\n"
             + "Title: " + title + "\n"
             + "Category: " + category + "\n"
             + "Location: " + location + "\n"
             + "Date: " + date + "\n"
             + "Description: " + description + "\n"
             + "Contact: " + contactName + "\n"
             + "Phone: " + contactPhone + "\n"
             + "Status: " + status + "\n"
             + "Storage: " + storagePlace + "\n"
             + "---\n";
    }
}
