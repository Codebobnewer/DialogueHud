package com.goga221.dialoguehud;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.goga221.dialoguehud.command.DialogueCommand;
import com.goga221.dialoguehud.dialogue.DialogueRegistry;
import com.goga221.dialoguehud.hud.BetterHudDialogueRenderer;
import com.goga221.dialoguehud.listener.DialogueInputListener;
import com.goga221.dialoguehud.listener.FancyNpcsDialogueListener;
import com.goga221.dialoguehud.npc.NpcDialogueMapping;
import com.goga221.dialoguehud.session.DialogueSessionManager;
import com.goga221.dialoguehud.session.JumpSuppressor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class DialogueHudPlugin extends JavaPlugin {

    private TaskScheduler scheduler;
    private DialogueRegistry dialogueRegistry;
    private DialogueSessionManager sessionManager;
    private NpcDialogueMapping npcDialogueMapping;
    private JumpSuppressor jumpSuppressor;

    @Override
    public void onEnable() {
        saveResource("dialogues/greeting.yml", false);
        saveResource("npc-dialogues.yml", false);

        this.scheduler = UniversalScheduler.getScheduler(this);
        this.jumpSuppressor = new JumpSuppressor(this);

        // Self-heal: a previous /reload (not a full restart) keeps entity instances alive,
        // so a modifier added by an earlier plugin instance could otherwise linger.
        for (Player player : getServer().getOnlinePlayers()) {
            jumpSuppressor.restore(player);
        }

        this.dialogueRegistry = new DialogueRegistry(new File(getDataFolder(), "dialogues"), getLogger());
        dialogueRegistry.reload();

        this.npcDialogueMapping = new NpcDialogueMapping(new File(getDataFolder(), "npc-dialogues.yml"), getLogger());
        npcDialogueMapping.reload();

        BetterHudDialogueRenderer renderer = new BetterHudDialogueRenderer(getLogger());
        this.sessionManager = new DialogueSessionManager(renderer, scheduler, jumpSuppressor);
        renderer.registerPlaceholders(sessionManager);

        getServer().getPluginManager().registerEvents(new DialogueInputListener(sessionManager), this);
        getServer().getPluginManager().registerEvents(
                new FancyNpcsDialogueListener(npcDialogueMapping, dialogueRegistry, sessionManager), this);

        new DialogueCommand(dialogueRegistry, npcDialogueMapping, sessionManager).register(this);

        getSLF4JLogger().info("Loaded {} dialogue(s).", dialogueRegistry.getAll().size());
    }

    @Override
    public void onDisable() {
        if (jumpSuppressor != null) {
            for (Player player : getServer().getOnlinePlayers()) {
                jumpSuppressor.restore(player);
            }
        }
        if (scheduler != null) {
            scheduler.cancelTasks();
        }
    }
}
