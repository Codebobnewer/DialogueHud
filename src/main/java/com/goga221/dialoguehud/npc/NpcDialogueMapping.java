package com.goga221.dialoguehud.npc;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Maps a FancyNpcs NPC name to the dialogue it should start when a player interacts with it,
 * configured in npc-dialogues.yml (npc name -> dialogue name).
 */
public final class NpcDialogueMapping {

    private final File file;
    private final Logger logger;
    private final Map<String, String> dialogueByNpcName = new HashMap<>();

    public NpcDialogueMapping(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    public void reload() {
        dialogueByNpcName.clear();

        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String npcName : config.getKeys(false)) {
            String dialogueName = config.getString(npcName);
            if (dialogueName != null) {
                dialogueByNpcName.put(npcName, dialogueName);
            }
        }

        logger.info("Loaded " + dialogueByNpcName.size() + " NPC dialogue mapping(s).");
    }

    @Nullable
    public String getDialogueName(String npcName) {
        return dialogueByNpcName.get(npcName);
    }
}
