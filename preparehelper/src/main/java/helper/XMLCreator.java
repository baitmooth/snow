package helper;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class XMLCreator {
    private static final Pattern DRAWABLE_PATTERN = Pattern.compile("drawable=\"([\\w_]+)\"");
    private static final Locale LOCALE = Locale.ROOT;
    private static final Set<String> NON_ALPHABETICAL_CATEGORIES = Set.of("Folders", "Calendar");

    public static void mergeNewDrawables(String valuesDir, String generatedDir, String assetPath, String iconsDir,
                                         String xmlDir, String appFilterPath, CategoryDiscoveryService categoryDiscoveryService) throws IOException {

        // Build drawable to package name map
        Map<String, String> drawableToPackageName = new HashMap<>();
        Path appFilter = Paths.get(appFilterPath);
        if (Files.exists(appFilter)) {
            String content = Files.readString(appFilter);
            Matcher m = Pattern.compile("component=\"ComponentInfo\\{([\\w.]+)/[\\w.]+\\}\"\\s+drawable=\"([\\w_]+)\"").matcher(content);
            while (m.find()) {
                String packageName = m.group(1);
                String drawableName = m.group(2);
                drawableToPackageName.putIfAbsent(drawableName, packageName);
            }
        }

        // 1. Load all available icon names from the directory first
        Set<String> availableIcons = new HashSet<>();
        Path iconsPath = Paths.get(iconsDir);
        if (Files.exists(iconsPath)) {
            try (Stream<Path> stream = Files.list(iconsPath)) {
                stream.map(p -> p.getFileName().toString())
                        .filter(name -> name.contains("."))
                        .map(name -> name.substring(0, name.lastIndexOf('.')))
                        .forEach(availableIcons::add);
            }
        }

        Set<String> newDrawables = new TreeSet<>();
        Set<String> games = new TreeSet<>();
        Set<String> system = new TreeSet<>();

        Map<String, Set<String>> categories = new LinkedHashMap<>();
        categories.put("New", newDrawables);
        categories.put("Folders", new TreeSet<>());
        categories.put("Calendar", new TreeSet<>());
        categories.put("Google", new TreeSet<>());
        categories.put("Microsoft", new TreeSet<>());
        categories.put("Games", games);
        categories.put("System", system);
        categories.put("Emoji", new TreeSet<>());
        categories.put("Symbols", new TreeSet<>());
        categories.put("Numbers", new TreeSet<>());
        categories.put("Letters", new TreeSet<>());
        categories.put("0-9", new TreeSet<>());

        // Pre-initialize A-Z categories
        for (char c = 'A'; c <= 'Z'; c++) {
            categories.put(String.valueOf(c), new TreeSet<>());
        }

        // 2. Load existing data, strictly filtering against availableIcons
        // Note: Arcticons looks for newdrawables.xml in generatedDir
        loadDrawablesFromXml(Paths.get(generatedDir, "newdrawables.xml"), newDrawables, availableIcons);

        Path pathGames = Paths.get(generatedDir, "games.xml");
        loadLinesToSet(pathGames, games, availableIcons);

        Path pathSystem = Paths.get(generatedDir, "system.xml");
        loadLinesToSet(pathSystem, system, availableIcons);

        // 3. Classify all icons (using the already loaded set)
        availableIcons.forEach(name -> classify(name, categories, drawableToPackageName, categoryDiscoveryService));

        // Save total count
        int totalIcons = categories.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet())
                .size();

        createCustomIconCountFile(Paths.get(valuesDir, "custom_icon_count.xml"), totalIcons);

        // Note: We write back the filtered lists. This cleans up the source files if items were deleted.
        if (Files.exists(pathGames)) {
            Files.write(pathGames, (Iterable<String>) games.stream().sorted()::iterator);
        }
        if (Files.exists(pathSystem)) {
            Files.write(pathSystem, (Iterable<String>) system.stream().sorted()::iterator);
        }

        // Build XML Output
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n<version>1</version>\n");
        categories.forEach((title, items) -> {
            if (!items.isEmpty()) {
                String escapedTitle = title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
                xml.append(String.format(LOCALE, "\n\t<category title=\"%s\" />\n", escapedTitle));
                for (String item : items) {
                    xml.append(String.format(LOCALE, "\t<item drawable=\"%s\" />\n", item));
                }
            }
        });
        xml.append("\n</resources>");

        // Write and sync files
        Path drawableXml = Paths.get(xmlDir, "drawable.xml");
        Files.writeString(drawableXml, xml.toString());

        syncFiles(drawableXml, Path.of(assetPath, "drawable.xml"));
        syncFiles(Path.of(appFilterPath), Path.of(assetPath, "appfilter.xml"),
                Path.of(xmlDir, "appfilter.xml"), Path.of(assetPath, "icon_config.xml"),
                Path.of(xmlDir, "icon_config.xml"));
    }

    private static void classify(String name, Map<String, Set<String>> categories, Map<String, String> drawableToPackageName, CategoryDiscoveryService categoryDiscoveryService) {
        String category = null;

        // Try to get category from F-Droid
        String packageName = drawableToPackageName.get(name);
        if (packageName != null) {
            List<String> fDroidCategories = categoryDiscoveryService.getCategories(packageName);
            if (fDroidCategories != null && !fDroidCategories.isEmpty()) {
                category = fDroidCategories.get(0);
            }
        }
        
        // Fallback patterns if no F-Droid category found
        if (category == null) {
            if (name.startsWith("folder_")) category = "Folders";
            else if (name.startsWith("calendar_")) category = "Calendar";
            else if (name.startsWith("google_")) category = "Google";
            else if (name.startsWith("microsoft_") || name.startsWith("xbox")) category = "Microsoft";
            else if (name.startsWith("emoji_")) category = "Emoji";
            else if (name.startsWith("letter_")) category = "Letters";
            else if (name.startsWith("currency_") || name.startsWith("symbol_")) category = "Symbols";
            else if (name.startsWith("number_")) category = "Numbers";
            else if (name.startsWith("_")) category = "0-9";
        }

        // Add to thematic category
        if (category != null) {
            categories.computeIfAbsent(category, k -> new TreeSet<>()).add(name);
        }

        // Alphabetical assignment (skip if specifically excluded)
        if (category == null || !NON_ALPHABETICAL_CATEGORIES.contains(category)) {
            if (name.startsWith("_")) {
                categories.get("0-9").add(name);
            } else {
                String firstLetter = name.substring(0, 1).toUpperCase();
                if (categories.containsKey(firstLetter)) {
                    categories.get(firstLetter).add(name);
                } else {
                    categories.get("Symbols").add(name);
                }
            }
        }
    }

    private static void loadDrawablesFromXml(Path path, Set<String> target, Set<String> validIcons) {
        if (!Files.exists(path)) return;
        try {
            String content = Files.readString(path);
            Matcher m = DRAWABLE_PATTERN.matcher(content);
            while (m.find()) {
                String drawableName = m.group(1);
                if (validIcons.contains(drawableName)) {
                    target.add(drawableName);
                }
            }
        } catch (IOException ignored) {}
    }

    private static void loadLinesToSet(Path path, Set<String> target, Set<String> validIcons) {
        if (!Files.exists(path)) return;
        try (Stream<String> lines = Files.lines(path)) {
            lines.filter(l -> !l.isBlank())
                    .filter(validIcons::contains)
                    .forEach(target::add);
        } catch (IOException ignored) {}
    }

    private static void createCustomIconCountFile(Path path, int count) throws IOException {
        String xml = String.format(LOCALE, """
                <?xml version="1.0" encoding="utf-8"?>
                <resources>
                   <integer name="custom_icons_count">%d</integer>
                </resources>""", count);
        Files.writeString(path, xml);
    }

    private static void syncFiles(Path source, Path... destinations) throws IOException {
        for (Path dest : destinations) {
            Files.createDirectories(dest.getParent());
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
