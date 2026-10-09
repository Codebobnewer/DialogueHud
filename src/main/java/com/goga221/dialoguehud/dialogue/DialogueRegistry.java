package com.goga221.dialoguehud.dialogue;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class DialogueRegistry {

    private final File dialoguesFolder;
    private final Logger logger;
    private final Map<String, Dialogue> dialoguesByName = new ConcurrentHashMap<>();

    public DialogueRegistry(File dialoguesFolder, Logger logger) {
        this.dialoguesFolder = dialoguesFolder;
        this.logger = logger;
    }

    public void reload() {
        dialoguesByName.clear();

        if (!dialoguesFolder.exists() && !dialoguesFolder.mkdirs()) {
            logger.warning("Could not create dialogues folder: " + dialoguesFolder);
            return;
        }

        File[] files = dialoguesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            String name = file.getName().substring(0, file.getName().length() - ".yml".length());
            Dialogue dialogue = load(name, file);
            if (dialogue != null) {
                dialoguesByName.put(name, dialogue);
            }
        }
    }

    @Nullable
    private Dialogue load(String name, File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String start = config.getString("start");
        if (start == null) {
            logger.warning("Dialogue '" + name + "' has no 'start' line defined, skipping.");
            return null;
        }

        String speaker = config.getString("speaker", "");

        ConfigurationSection linesSection = config.getConfigurationSection("lines");
        if (linesSection == null) {
            logger.warning("Dialogue '" + name + "' has no 'lines' section, skipping.");
            return null;
        }

        Map<String, DialogueLine> lines = new HashMap<>();
        for (String lineId : linesSection.getKeys(false)) {
            ConfigurationSection lineSection = linesSection.getConfigurationSection(lineId);
            if (lineSection == null) {
                continue;
            }

            String text = lineSection.getString("text", "");
            List<DialogueOption> options = new ArrayList<>();
            for (Map<?, ?> optionMap : lineSection.getMapList("options")) {
                Object textObj = optionMap.get("text");
                if (textObj == null) {
                    continue;
                }
                Object nextObj = optionMap.get("next");
                Object commandObj = optionMap.get("command");
                options.add(new DialogueOption(
                        textObj.toString(),
                        nextObj != null ? nextObj.toString() : null,
                        commandObj != null ? commandObj.toString() : null
                ));
            }

            lines.put(lineId, new DialogueLine(lineId, text, options));
        }

        if (!lines.containsKey(start)) {
            logger.warning("Dialogue '" + name + "' references unknown start line '" + start + "', skipping.");
            return null;
        }

        for (DialogueLine line : lines.values()) {
            for (DialogueOption option : line.getOptions()) {
                if (!option.endsDialogue() && !lines.containsKey(option.getNextLine())) {
                    logger.warning("Dialogue '" + name + "' line '" + line.getId()
                            + "' has an option pointing to unknown line '" + option.getNextLine() + "'.");
                }
            }
        }

        return new Dialogue(name, speaker, start, lines);
    }

    @Nullable
    public Dialogue get(String name) {
        return dialoguesByName.get(name);
    }

    public Map<String, Dialogue> getAll() {
        return dialoguesByName;
    }
}
