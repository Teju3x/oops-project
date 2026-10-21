import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Server {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        startServer();
    }

    public static void startServer() throws IOException {
        // 1. Ensure latest data is loaded into memory
        Main.loadData();

        // 2. Initialize HttpServer on port 8080
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
        } catch (IOException e) {
            System.err.println("Could not bind to port " + PORT + ". Attempting port 8081...");
            server = HttpServer.create(new InetSocketAddress(8081), 0);
        }

        int activePort = server.getAddress().getPort();

        // Route: Home Dashboard
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod()) && path.equals("/")) {
                handleShowPage(exchange);
            } else if (!path.equals("/")) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
            }
        });

        // Route: Add Lost Item
        server.createContext("/add-lost", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleAddLost(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        });

        // Route: Add Found Item
        server.createContext("/add-found", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleAddFound(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        });

        // Route: Claim Item (?id=101)
        server.createContext("/claim", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleClaim(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        });

        // Route: Delete Item (?id=101)
        server.createContext("/delete", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleDelete(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        });

        // Route: REST API for items (/api/items)
        server.createContext("/api/items", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleApiItems(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        });

        server.setExecutor(null); // default multithreaded executor
        server.start();

        System.out.println("======================================================");
        System.out.println("   Campus Lost & Found HTTP Server is Running!");
        System.out.println("   Access Frontend Dashboard at: http://localhost:" + activePort);
        System.out.println("   Total Loaded Items: " + Main.list.size());
        System.out.println("   Persistence File  : " + Main.getStorageFile().getAbsolutePath());
        System.out.println("======================================================");
    }

    // Serve index.html with dynamically injected statistics, table rows, and smart matches
    static void handleShowPage(HttpExchange exchange) throws IOException {
        String html = readHtmlFile();

        int total = Main.list.size();
        int lostCount = 0;
        int foundCount = 0;
        int claimedCount = 0;

        for (Item item : Main.list) {
            if ("LOST".equalsIgnoreCase(item.type)) lostCount++;
            else if ("FOUND".equalsIgnoreCase(item.type)) foundCount++;
            if ("Claimed".equalsIgnoreCase(item.status)) claimedCount++;
        }

        int matchesCount = countMatches();

        // Dynamic placeholder replacements
        html = html.replace("<!-- STAT_TOTAL -->", String.valueOf(total));
        html = html.replace("<!-- STAT_LOST -->", String.valueOf(lostCount));
        html = html.replace("<!-- STAT_FOUND -->", String.valueOf(foundCount));
        html = html.replace("<!-- STAT_MATCHES -->", String.valueOf(matchesCount));
        html = html.replace("<!-- STAT_CLAIMED -->", String.valueOf(claimedCount));
        html = html.replace("<!-- MATCHES_PLACEHOLDER -->", generateMatches());
        html = html.replace("<!-- TABLE_ROWS_PLACEHOLDER -->", generateTableRows());

        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Handle POST /add-lost
    static void handleAddLost(HttpExchange exchange) throws IOException {
        Map<String, String> form = parseFormData(exchange);

        int id = ++Main.idCounter;
        String title = form.getOrDefault("title", "").trim();
        String category = form.getOrDefault("category", "Other").trim();
        String location = form.getOrDefault("location", "").trim();
        String date = form.getOrDefault("date", "").trim();
        String desc = form.getOrDefault("description", "").trim();
        String name = form.getOrDefault("name", "").trim();
        String phone = form.getOrDefault("phone", "").trim();

        if (!title.isEmpty()) {
            // Polymorphism & Upcasting: LostItem stored as Item
            Main.list.add(new LostItem(id, title, category, location, date, desc, name, phone, "Open"));
            Main.saveData();
            System.out.println(">> [SUCCESS] Added Lost Item #" + id + ": \"" + title + "\" -> Saved to " + Main.getStorageFile().getName());
        }

        redirect(exchange, "/?msg=lost_added");
    }

    // Handle POST /add-found
    static void handleAddFound(HttpExchange exchange) throws IOException {
        Map<String, String> form = parseFormData(exchange);

        int id = ++Main.idCounter;
        String title = form.getOrDefault("title", "").trim();
        String category = form.getOrDefault("category", "Other").trim();
        String location = form.getOrDefault("location", "").trim();
        String date = form.getOrDefault("date", "").trim();
        String desc = form.getOrDefault("description", "").trim();
        String name = form.getOrDefault("name", "").trim();
        String phone = form.getOrDefault("phone", "").trim();
        String storagePlace = form.getOrDefault("storagePlace", "Campus Desk").trim();

        if (!title.isEmpty()) {
            // Polymorphism & Upcasting: FoundItem stored as Item
            Main.list.add(new FoundItem(id, title, category, location, date, desc, name, phone, "Open", storagePlace));
            Main.saveData();
            System.out.println(">> [SUCCESS] Added Found Item #" + id + ": \"" + title + "\" -> Saved to " + Main.getStorageFile().getName());
        }

        redirect(exchange, "/?msg=found_added");
    }

    // Handle GET /claim?id=101
    static void handleClaim(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        if (query != null && query.contains("id=")) {
            try {
                int id = Integer.parseInt(extractParam(query, "id"));
                Main.claimItemById(id);
                System.out.println(">> [SUCCESS] Claimed Item #" + id + " -> Updated in " + Main.getStorageFile().getName());
            } catch (Exception ignored) {}
        }
        redirect(exchange, "/?msg=claimed");
    }

    // Handle GET /delete?id=101
    static void handleDelete(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        if (query != null && query.contains("id=")) {
            try {
                int id = Integer.parseInt(extractParam(query, "id"));
                Main.deleteItemById(id);
                System.out.println(">> [SUCCESS] Deleted Item #" + id + " -> Updated in " + Main.getStorageFile().getName());
            } catch (Exception ignored) {}
        }
        redirect(exchange, "/?msg=deleted");
    }

    // Handle GET /api/items (JSON endpoint)
    static void handleApiItems(HttpExchange exchange) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < Main.list.size(); i++) {
            Item item = Main.list.get(i);
            sb.append("{");
            sb.append("\"id\":").append(item.id).append(",");
            sb.append("\"type\":").append(jsonQuote(item.type)).append(",");
            sb.append("\"title\":").append(jsonQuote(item.title)).append(",");
            sb.append("\"category\":").append(jsonQuote(item.category)).append(",");
            sb.append("\"location\":").append(jsonQuote(item.location)).append(",");
            sb.append("\"date\":").append(jsonQuote(item.date)).append(",");
            sb.append("\"description\":").append(jsonQuote(item.description)).append(",");
            sb.append("\"contactName\":").append(jsonQuote(item.contactName)).append(",");
            sb.append("\"contactPhone\":").append(jsonQuote(item.contactPhone)).append(",");
            sb.append("\"status\":").append(jsonQuote(item.status));
            if (item instanceof FoundItem) {
                sb.append(",\"storagePlace\":").append(jsonQuote(((FoundItem) item).storagePlace));
            }
            sb.append("}");
            if (i < Main.list.size() - 1) sb.append(",");
        }
        sb.append("]");

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Generate table rows with data-* attributes for dynamic client-side filtering
    static String generateTableRows() {
        if (Main.list.isEmpty()) {
            return "<tr><td colspan='8' class='empty-box'>✨ No campus items logged yet! Use the form above to report a lost or found item.</td></tr>";
        }

        StringBuilder sb = new StringBuilder();
        for (Item item : Main.list) {
            String typeBadge = "LOST".equalsIgnoreCase(item.type)
                ? "<span class='badge-neo badge-lost'>LOST</span>"
                : "<span class='badge-neo badge-found'>FOUND</span>";

            String statusHtml;
            if ("Claimed".equalsIgnoreCase(item.status)) {
                statusHtml = "<span class='status-pill claimed'>CLAIMED</span>";
            } else {
                statusHtml = "<span class='status-pill open'>OPEN</span> "
                           + "<a href='/claim?id=" + item.id + "' class='btn-neo-action btn-claim' title='Mark as resolved'>CLAIM</a>";
            }

            statusHtml += " <a href='/delete?id=" + item.id + "' class='btn-neo-action btn-delete' onclick=\"return confirm('Delete item #" + item.id + "?');\" title='Delete record'>DEL</a>";

            String locationText = "<span class='loc-text'>" + escapeHtml(item.location) + "</span>";
            if (item instanceof FoundItem) {
                locationText += "<br><span class='custody-badge'>KEPT AT: " + escapeHtml(((FoundItem) item).storagePlace) + "</span>";
            }

            String catClass = "cat-" + escapeHtml(item.category.replaceAll("[^a-zA-Z]", "").toLowerCase());

            sb.append("<tr data-type=\"").append(escapeHtml(item.type)).append("\" ")
              .append("data-category=\"").append(escapeHtml(item.category)).append("\" ")
              .append("data-status=\"").append(escapeHtml(item.status)).append("\">")
              .append("<td class='col-id'>#").append(item.id).append("</td>")
              .append("<td>").append(typeBadge).append("</td>")
              .append("<td><strong class='item-title'>").append(escapeHtml(item.title)).append("</strong>")
              .append("<br><span class='item-desc'>").append(escapeHtml(item.description)).append("</span></td>")
              .append("<td><span class='badge-category ").append(catClass).append("'>").append(escapeHtml(item.category)).append("</span></td>")
              .append("<td>").append(locationText).append("</td>")
              .append("<td><span class='date-pill'>").append(escapeHtml(item.date)).append("</span></td>")
              .append("<td><strong>").append(escapeHtml(item.contactName)).append("</strong>")
              .append("<br><span class='phone-text'>").append(escapeHtml(item.contactPhone)).append("</span></td>")
              .append("<td>").append(statusHtml).append("</td>")
              .append("</tr>\n");
        }
        return sb.toString();
    }

    // Calculate count of active matches
    static int countMatches() {
        int count = 0;
        for (Item lost : Main.list) {
            if (!"LOST".equalsIgnoreCase(lost.type) || "Claimed".equalsIgnoreCase(lost.status)) continue;
            for (Item found : Main.list) {
                if (!"FOUND".equalsIgnoreCase(found.type) || "Claimed".equalsIgnoreCase(found.status)) continue;

                boolean sameCat = lost.category.equalsIgnoreCase(found.category);
                boolean matchingLoc = lost.location.toLowerCase().contains(found.location.toLowerCase()) ||
                                      found.location.toLowerCase().contains(lost.location.toLowerCase());

                if (sameCat && matchingLoc) count++;
            }
        }
        return count;
    }

    // Generate Smart Match cards
    static String generateMatches() {
        StringBuilder sb = new StringBuilder();
        int count = 0;

        for (Item lost : Main.list) {
            if (!"LOST".equalsIgnoreCase(lost.type) || "Claimed".equalsIgnoreCase(lost.status)) continue;

            for (Item found : Main.list) {
                if (!"FOUND".equalsIgnoreCase(found.type) || "Claimed".equalsIgnoreCase(found.status)) continue;

                boolean sameCat = lost.category.equalsIgnoreCase(found.category);
                boolean matchingLoc = lost.location.toLowerCase().contains(found.location.toLowerCase()) ||
                                      found.location.toLowerCase().contains(lost.location.toLowerCase());

                if (sameCat && matchingLoc) {
                    count++;
                    String storage = (found instanceof FoundItem) ? ((FoundItem) found).storagePlace : "Desk";

                    sb.append("<div class='match-card'>");
                    sb.append("<div class='match-card-header'>");
                    sb.append("<span class='match-badge'>⚡ SMART MATCH #").append(count).append(" DETECTED</span>");
                    sb.append("<div class='match-actions'>");
                    sb.append("<a href='/claim?id=").append(lost.id).append("' class='btn-neo-action btn-claim'>CLAIM LOST #").append(lost.id).append("</a> ");
                    sb.append("<a href='/claim?id=").append(found.id).append("' class='btn-neo-action btn-claim'>CLAIM FOUND #").append(found.id).append("</a>");
                    sb.append("</div>");
                    sb.append("</div>");

                    sb.append("<div class='match-content-grid'>");
                    sb.append("<div class='match-side match-lost-side'>");
                    sb.append("<div class='side-label'>🔴 LOST ITEM REPORT</div>");
                    sb.append("<div class='match-item-name'>#").append(lost.id).append(" - ").append(escapeHtml(lost.title)).append("</div>");
                    sb.append("<div class='match-detail'>📍 <strong>Lost at:</strong> ").append(escapeHtml(lost.location)).append(" (").append(escapeHtml(lost.date)).append(")</div>");
                    sb.append("<div class='match-detail'>👤 <strong>Owner:</strong> ").append(escapeHtml(lost.contactName)).append(" | 📞 ").append(escapeHtml(lost.contactPhone)).append("</div>");
                    sb.append("</div>");

                    sb.append("<div class='match-side match-found-side'>");
                    sb.append("<div class='side-label'>🟢 FOUND ITEM REPORT</div>");
                    sb.append("<div class='match-item-name'>#").append(found.id).append(" - ").append(escapeHtml(found.title)).append("</div>");
                    sb.append("<div class='match-detail'>🏢 <strong>Custody:</strong> ").append(escapeHtml(storage)).append("</div>");
                    sb.append("<div class='match-detail'>👤 <strong>Finder:</strong> ").append(escapeHtml(found.contactName)).append(" | 📞 ").append(escapeHtml(found.contactPhone)).append("</div>");
                    sb.append("</div>");
                    sb.append("</div>");

                    sb.append("</div>\n");
                }
            }
        }

        if (count == 0) {
            return "<div class='empty-match-box'>⚡ No active matches right now. When a reported lost item shares the category and location with a found item, a match card will appear here automatically!</div>";
        }
        return sb.toString();
    }

    // Robust file reader for index.html
    static String readHtmlFile() throws IOException {
        File file = getHtmlFile();
        if (!file.exists()) {
            return "<html><body><h2>Error: index.html not found!</h2><p>Expected file at: " + file.getAbsolutePath() + "</p></body></html>";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    static File getHtmlFile() {
        File f1 = new File("index.html");
        if (f1.exists()) return f1;
        File f2 = new File("project/index.html");
        if (f2.exists()) return f2;
        return f1;
    }

    // Helper: Parse URL encoded form data from POST body
    static Map<String, String> parseFormData(HttpExchange exchange) throws IOException {
        Map<String, String> map = new HashMap<>();
        try (InputStream is = exchange.getRequestBody();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String body = sb.toString();
            if (!body.isEmpty()) {
                String[] pairs = body.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    if (idx != -1) {
                        String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                        String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                        map.put(key, value);
                    }
                }
            }
        }
        return map;
    }

    // Helper: Extract query parameter
    static String extractParam(String query, String param) {
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=");
            if (parts.length == 2 && parts[0].equalsIgnoreCase(param)) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    // Helper: HTTP 303 Redirect
    static void redirect(HttpExchange exchange, String location) throws IOException {
        byte[] bytes = ("Redirecting to " + location).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(303, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Helper: XSS sanitizer
    static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    // Helper: JSON string quoter
    static String jsonQuote(String text) {
        if (text == null) return "\"\"";
        return "\"" + text.replace("\\", "\\\\")
                          .replace("\"", "\\\"")
                          .replace("\n", "\\n")
                          .replace("\r", "\\r") + "\"";
    }
}
