package helper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class Changelog {

    public record ChangelogData(String versionName, int total, int newIcons, int reused, String notes, String date) {}

    public static void main(String[] args) {
        String rootDir = System.getProperty("user.dir");
        if (Paths.get(rootDir).getFileName().toString().equals("preparehelper")) rootDir = "..";

        String valuesDir = rootDir + "/app/src/main/res/values";
        String appFilter = rootDir + "/newicons/appfilter.xml";
        String changelogXml = valuesDir + "/changelog.xml";
        String generatedDir = rootDir + "/generated";

        generateChangelogs(generatedDir, valuesDir + "/custom_icon_count.xml", appFilter, changelogXml, rootDir, false);
    }

    public static void generateChangelogs(String generatedDir, String customIconCountXml, String appFilter, String changelogXml, String rootDir, boolean newRelease) {
        int countTotal = getIntegerValue(customIconCountXml);
        int countNew = countTags(generatedDir + "/newdrawables.xml", "item");
        int countFilterTotal = countTags(appFilter, "item");
        int countFilterOld = readStoredCount(generatedDir + "/countFilterTotal.txt");
        int countReused = countFilterTotal - countFilterOld - countNew;

        ChangelogData data = new ChangelogData(
                getVersionName(rootDir),
                countTotal, countNew, countReused,
                getReleaseNotes(rootDir),
                LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        );

        // Generate all formats
        saveMarkdown(data, generatedDir + "/changelog.md");
        saveXml(data, changelogXml);
        saveFDroidNotes(data, rootDir);
        updateMainChangelog(data, rootDir);

        if (newRelease) {
            safeWrite(String.valueOf(countFilterTotal), generatedDir + "/countFilterTotal.txt");
        }
    }

    private static void updateMainChangelog(ChangelogData d, String rootDir) {
        Path path = Paths.get(rootDir, "CHANGELOG.md");
        
        StringBuilder entryBuilder = new StringBuilder();
        entryBuilder.append(String.format(Locale.ROOT, "### %s\n###### Released %s\n", d.versionName, d.date));
        entryBuilder.append(String.format(Locale.ROOT, "- 🎉 **%d** new and updated icons!\n", d.newIcons));
        entryBuilder.append(String.format(Locale.ROOT, "- 💡 Added support for **%d** apps using existing icons.\n", d.reused));
        entryBuilder.append(String.format(Locale.ROOT, "- 🔥 **%d** icons in total!\n", d.total));

        if (!d.notes.isEmpty()) {
            entryBuilder.append("\n---\n"); // Internal separator
            for (String line : d.notes.split("\n")) {
                if (!line.isBlank()) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.startsWith("-") || trimmedLine.startsWith("*")) {
                        entryBuilder.append(trimmedLine).append("\n");
                    } else {
                        entryBuilder.append("- ").append(trimmedLine).append("\n");
                    }
                }
            }
        }

        String githubUrl = String.format(Locale.ROOT, "https://github.com/baitmooth/snow/releases/tag/v%s", d.versionName);
        entryBuilder.append(String.format(Locale.ROOT, "\n[Visit Release Page](%s)\n", githubUrl));
        entryBuilder.append("\n***\n"); // End of release separator

        String newEntry = entryBuilder.toString();

        try {
            String content = Files.exists(path) ? Files.readString(path).replace("\r\n", "\n") : "";
            String versionHeader = "### " + d.versionName + "\n";

            if (content.contains(versionHeader)) {
                // Idempotent update: replace existing version entry
                int start = content.indexOf(versionHeader);
                int end = -1;
                int searchStart = start + versionHeader.length();
                while (true) {
                    int nextH3 = content.indexOf("\n### ", searchStart);
                    if (nextH3 == -1) break;
                    if (!content.startsWith("####", nextH3 + 1)) {
                        end = nextH3 + 1;
                        break;
                    }
                    searchStart = nextH3 + 5;
                }
                
                if (end == -1) end = content.length();

                content = content.substring(0, start) + newEntry + "\n" + content.substring(end).stripLeading();
            } else {
                // Prepend new version
                content = newEntry + "\n" + content;
            }

            safeWrite(content.strip(), path.toString());
        } catch (IOException e) {
            System.err.println("Error updating main CHANGELOG.md: " + e.getMessage());
        }
    }

    private static void saveMarkdown(ChangelogData d, String path) {
        StringBuilder content = new StringBuilder();

        // Header bullet points
        content.append(String.format(Locale.ROOT,"* 🎉 **%d** new and updated icons!\n", d.newIcons));
        content.append(String.format(Locale.ROOT,"* 💡 Added support for **%d** apps using existing icons.\n", d.reused));
        content.append(String.format(Locale.ROOT,"* 🔥 **%d** icons in total!", d.total));

        // Process notes into bullet points
        if (!d.notes.isEmpty()) {
            for (String line : d.notes.split("\n")) {
                if (!line.isBlank()) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.startsWith("-") || trimmedLine.startsWith("*")) {
                        content.append("\n* ").append(trimmedLine.substring(1).trim());
                    } else {
                        content.append("\n* ").append(trimmedLine);
                    }
                }
            }
        }

        safeWrite(content.toString(), path);
    }

    private static void saveFDroidNotes(ChangelogData d, String rootDir) {
        int versionCode = getVersionCode(rootDir);
        if (versionCode == 0) {
            System.err.println("Could not determine versionCode, skipping F-Droid changelog.");
            return;
        }

        String content = String.format(Locale.ROOT,
                "🎉 %d new and updated icons!\n💡 Added support for %d apps using existing icons.\n🔥 %d icons in total!%s",
                d.newIcons, d.reused, d.total, d.notes.isEmpty() ? "" : "\n\n" + d.notes);

        String path = String.format(Locale.ROOT, "%s/fastlane/metadata/android/en-US/changelogs/%d.txt", rootDir, versionCode);
        safeWrite(content, path);
    }

    private static String getVersionName(String rootDir) {
        try {
            Path gradlePath = Paths.get(rootDir, "app", "build.gradle");
            List<String> lines = Files.readAllLines(gradlePath);
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.startsWith("versionName")) {
                    return trimmedLine.split("=")[1].replace("'", "").replace("\"", "").trim();
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading versionName: " + e.getMessage());
        }
        return "Unknown";
    }

    private static int getVersionCode(String rootDir) {
        try {
            Path gradlePath = Paths.get(rootDir, "app", "build.gradle");
            List<String> lines = Files.readAllLines(gradlePath);
            for (String line : lines) {
                // Trim to handle potential indentation
                String trimmedLine = line.trim();
                if (trimmedLine.startsWith("versionCode")) {
                    String[] parts = trimmedLine.split("=");
                    if (parts.length > 1) {
                        String value = parts[1].trim();
                        // Try to parse, if it fails, it's not a valid number
                        return Integer.parseInt(value);
                    }
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: versionCode is not a valid integer.");
        } catch (Exception e) {
            System.err.println("Error reading versionCode: " + e.getMessage());
        }
        return 0;
    }

    private static void saveXml(ChangelogData d, String path) {
        StringBuilder items = new StringBuilder();
        items.append(String.format(Locale.ROOT,"        <item>🎉 <b>%d</b> new and updated icons!</item>\n", d.newIcons));
        items.append(String.format(Locale.ROOT,"        <item>💡 Added support for <b>%d</b> apps using existing icons.</item>\n", d.reused));
        items.append(String.format(Locale.ROOT,"        <item>🔥 <b>%d</b> icons in total!</item>\n", d.total));

        if (!d.notes.isEmpty()) {
            for (String line : d.notes.split("\n")) {
                if (!line.isBlank()) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.startsWith("-") || trimmedLine.startsWith("*")) {
                        trimmedLine = trimmedLine.substring(1).trim();
                    }
                    items.append("        <item>").append(trimmedLine).append("</item>\n");
                }
            }
        }

        String xml = String.format(Locale.ROOT,"""
                <?xml version="1.0" encoding="utf-8"?>
                <resources>
                    <string name="changelog_date">%s</string>
                    <string-array name="changelog">
                %s    </string-array>
                </resources>""", d.date, items);
        safeWrite(xml, path);
    }

    // --- Helper Logic ---

    private static String getReleaseNotes(String rootDir) {
        try {
            return Files.readString(Paths.get(rootDir, "metadata", "additionalReleaseNotes.md")).strip();
        } catch (IOException e) { return ""; }
    }


    private static int readStoredCount(String path) {
        try {
            return Integer.parseInt(Files.readString(Paths.get(path)).strip());
        } catch (Exception e) { return 0; }
    }

    private static void safeWrite(String content, String pathStr) {
        try {
            Path path = Paths.get(pathStr);
            Files.createDirectories(path.getParent()); // Ensure folders exist
            Files.writeString(path, content);
            System.out.println("Saved: " + pathStr);
        } catch (IOException e) {
            System.err.println("Error writing " + pathStr + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static int countTags(String path, String tagName) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(path));
            return doc.getElementsByTagName(tagName).getLength();
        } catch (Exception e) { return 0; }
    }

    private static int getIntegerValue(String path) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(path));
            return Integer.parseInt(doc.getElementsByTagName("integer").item(0).getTextContent());
        } catch (Exception e) { return 0; }
    }
}
