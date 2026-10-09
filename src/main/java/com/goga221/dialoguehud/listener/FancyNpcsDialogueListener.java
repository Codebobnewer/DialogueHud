package com.goga221.dialoguehud.listener;

import com.goga221.dialoguehud.dialogue.Dialogue;
import com.goga221.dialoguehud.dialogue.DialogueRegistry;
import com.goga221.dialoguehud.npc.NpcDialogueMapping;
import com.goga221.dialoguehud.session.DialogueSessionManager;
import com.goga221.dialoguehud.util.Messages;
import de.oliver.fancynpcs.api.events.NpcInteractEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Starts a dialogue when a player interacts with a FancyNpcs NPC that has a mapping in
 * npc-dialogues.yml, hosting the dialogue on that NPC rather than requiring a command.
 */
public final class FancyNpcsDialogueListener implements Listener {

    private final NpcDialogueMapping npcDialogueMapping;
    private final DialogueRegistry dialogueRegistry;
    private final DialogueSessionManager sessionManager;

    public FancyNpcsDialogueListener(NpcDialogueMapping npcDialogueMapping, DialogueRegistry dialogueRegistry, DialogueSessionManager sessionManager) {
        this.npcDialogueMapping = npcDialogueMapping;
        this.dialogueRegistry = dialogueRegistry;
        this.sessionManager = sessionManager;
    }

    @EventHandler
    public void onNpcInteract(NpcInteractEvent event) {
        String npcName = event.getNpc().getData().getName();
        String dialogueName = npcDialogueMapping.getDialogueName(npcName);
        if (dialogueName == null) {
            return;
        }

        Dialogue dialogue = dialogueRegistry.get(dialogueName);
        if (dialogue == null) {
            return;
        }

        Player player = event.getPlayer();
        if (sessionManager.hasSession(player.getUniqueId())) {
            return;
        }

        if (!sessionManager.start(player, dialogue)) {
            Messages.send(player, "<red>Could not start this NPC's dialogue. Check the console.</red>");
        }
    }
}
