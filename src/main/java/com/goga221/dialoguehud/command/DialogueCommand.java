package com.goga221.dialoguehud.command;

import com.goga221.dialoguehud.dialogue.Dialogue;
import com.goga221.dialoguehud.dialogue.DialogueRegistry;
import com.goga221.dialoguehud.npc.NpcDialogueMapping;
import com.goga221.dialoguehud.session.DialogueSessionManager;
import com.goga221.dialoguehud.util.Messages;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class DialogueCommand {

    private static final String ADMIN_PERMISSION = "dialoguehud.admin";

    private final DialogueRegistry registry;
    private final NpcDialogueMapping npcDialogueMapping;
    private final DialogueSessionManager sessionManager;

    public DialogueCommand(DialogueRegistry registry, NpcDialogueMapping npcDialogueMapping, DialogueSessionManager sessionManager) {
        this.registry = registry;
        this.npcDialogueMapping = npcDialogueMapping;
        this.sessionManager = sessionManager;
    }

    public void register(JavaPlugin plugin) {
        new CommandTree("dialogue")
                .withPermission(ADMIN_PERMISSION)
                .then(new LiteralArgument("start")
                        .then(new StringArgument("name")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> registry.getAll().keySet().toArray(String[]::new)))
                                .executesPlayer((player, args) -> handleStart(player, (String) args.getUnchecked("name")))))
                .then(new LiteralArgument("stop")
                        .executesPlayer((player, args) -> {
                            sessionManager.end(player);
                            Messages.send(player, "<yellow>Dialogue ended.</yellow>");
                        }))
                .then(new LiteralArgument("reload")
                        .executesPlayer((player, args) -> {
                            registry.reload();
                            npcDialogueMapping.reload();
                            Messages.send(player, "<green>Reloaded " + registry.getAll().size() + " dialogue(s).</green>");
                        }))
                .register(plugin);
    }

    private void handleStart(Player player, String name) {
        Dialogue dialogue = registry.get(name);
        if (dialogue == null) {
            Messages.send(player, "<red>No dialogue named <white>" + name + "</white> exists.</red>");
            return;
        }

        if (!sessionManager.start(player, dialogue)) {
            Messages.send(player, "<red>Could not start dialogue <white>" + name
                    + "</white>. Check the console and make sure the BetterHud popup is configured.</red>");
        }
    }
}
