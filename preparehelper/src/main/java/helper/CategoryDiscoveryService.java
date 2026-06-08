package helper;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CategoryDiscoveryService {
    private Map<String, List<String>> packageToCategories = new HashMap<>();
    private Map<String, List<String>> manualCategories = new HashMap<>();
    private Set<String> unmappedPackages = new HashSet<>();
    private static final String INDEX_FILE = "data/index-v1.json";
    private static final String MANUAL_CATEGORIES_FILE = "manual_categories.json";
    private static final String UNCATEGORIZED_FILE = "newicons/uncategorized.txt";

    private static class FdroidIndex {
        @SerializedName("apps")
        List<App> apps;
    }

    private static class App {
        @SerializedName("packageName")
        String packageName;
        @SerializedName("categories")
        List<String> categories;
    }

    public void initialize() throws Exception {
        // Load manual categories
        File manualFile = new File(MANUAL_CATEGORIES_FILE);
        if (manualFile.exists()) {
            System.out.println("Loading manual categories...");
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(manualFile))) {
                Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
                manualCategories = new Gson().fromJson(reader, type);
            }
        }

        File indexFile = new File(INDEX_FILE);
        if (!indexFile.exists()) {
            throw new FileNotFoundException("F-Droid index snapshot not found at: " + INDEX_FILE);
        }

        System.out.println("Parsing F-Droid index snapshot: " + INDEX_FILE);
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(indexFile))) {
            Gson gson = new Gson();
            FdroidIndex index = gson.fromJson(reader, FdroidIndex.class);
            if (index != null && index.apps != null) {
                for (App app : index.apps) {
                    if (app.packageName != null && app.categories != null) {
                        packageToCategories.put(app.packageName, app.categories);
                    }
                }
            }
        }
        System.out.println("CategoryDiscoveryService initialized. Loaded " + packageToCategories.size() + " apps and " + manualCategories.size() + " manual overrides.");
    }

    public List<String> getCategories(String packageName) {
        // Check manual overrides first
        if (manualCategories.containsKey(packageName)) {
            return manualCategories.get(packageName).stream().map(this::mapCategory).toList();
        }

        List<String> rawCategories = packageToCategories.get(packageName);
        if (rawCategories == null) {
            unmappedPackages.add(packageName);
            return null;
        }
        
        // Map raw categories to internal schema
        return rawCategories.stream().map(this::mapCategory).toList();
    }

    public void writeUnmappedReport() {
        if (!unmappedPackages.isEmpty()) {
            try {
                File outputFile = new File(UNCATEGORIZED_FILE);
                File parentDir = outputFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
                    for (String pkg : unmappedPackages) {
                        writer.println(pkg);
                    }
                    System.out.println("Wrote " + unmappedPackages.size() + " unmapped packages to " + UNCATEGORIZED_FILE);
                }
            } catch (IOException e) {
                System.err.println("Failed to write unmapped report: " + e.getMessage());
            }
        } else {
             File outputFile = new File(UNCATEGORIZED_FILE);
             if (outputFile.exists()) {
                 outputFile.delete();
             }
        }
    }

    private String mapCategory(String rawCategory) {
        return switch (rawCategory) {
            case "Finance" -> "Finance";
            case "Security", "Firewall" -> "Security";
            case "Games", "Puzzle Game", "Strategy Game", "Role-Playing Game", "Action Game", "Sport Game" -> "Games";
            case "Internet", "Browser", "News", "Forum" -> "Internet";
            case "Multimedia", "Local Media Player", "Wallpaper", "Gallery", "Text to Speech" -> "Multimedia";
            case "System", "App Manager" -> "System";
            case "Writing", "Ebook Reader", "Reading", "Science & Education" -> "Education";
            default -> rawCategory;
        };
    }
}
