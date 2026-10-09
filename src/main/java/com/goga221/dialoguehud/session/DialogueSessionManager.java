package com.goga221.dialoguehud.session;

import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.goga221.dialoguehud.dialogue.Dialogue;
import com.goga221.dialoguehud.dialogue.DialogueLine;
import com.goga221.dialoguehud.dialogue.DialogueOption;
import com.goga221.dialoguehud.hud.BetterHudDialogueRenderer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DialogueSessionManager {

    private final BetterHudDialogueRenderer renderer;
    private final TaskScheduler scheduler;
    private final JumpSuppressor jumpSuppressor;
    private final Map<UUID, DialogueSession> sessions = new ConcurrentHashMap<>();

    public DialogueSessionManager(BetterHudDialogueRenderer renderer, TaskScheduler scheduler, JumpSuppressor jumpSuppressor) {
        this.renderer = renderer;
        this.scheduler = scheduler;
        this.jumpSuppressor = jumpSuppressor;
    }

    @Nullable
    public DialogueSession getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    public boolean hasSession(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public boolean start(Player player, Dialogue dialogue) {
        DialogueLine start = dialogue.getStart();
        if (start == null) {
            return false;
        }

        end(player);

        DialogueSession session = new DialogueSession(player.getUniqueId(), dialogue, start);
        sessions.put(player.getUniqueId(), session);

        if (!renderer.show(player, session)) {
            sessions.remove(player.getUniqueId());
            return false;
        }

        // Space doubles as "select"; suppressing jump stops the player from actually
        // jumping when they press it, rather than fighting the client's own jump prediction.
        jumpSuppressor.suppress(player);
        return true;
    }

    public void scroll(Player player, int direction) {
        DialogueSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }

        int optionCount = session.getCurrentLine().getOptionCount();
        if (optionCount == 0) {
            return;
        }

        int newIndex = Math.clamp(session.getSelectedIndex() + direction, 0, optionCount - 1);
        session.setSelectedIndex(newIndex);
        renderer.refresh(player, session);
    }

    public void select(Player player) {
        DialogueSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }

        DialogueLine line = session.getCurrentLine();
        if (line.getOptionCount() == 0) {
            end(player);
            return;
        }

        DialogueOption option = line.getOption(session.getSelectedIndex());

        if (option.hasCommand()) {
            String command = option.getCommand().replace("%player%", player.getName());
            // Console command dispatch must run on Folia's global tick thread, not this
            // entity's region thread - CraftServer#dispatchCommand asserts on it.
            scheduler.runTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        }

        if (option.endsDialogue()) {
            end(player);
            return;
        }

        DialogueLine nextLine = session.getDialogue().getLine(option.getNextLine());
        if (nextLine == null) {
            end(player);
            return;
        }

        session.setCurrentLine(nextLine);
        session.setSelectedIndex(0);
        renderer.refresh(player, session);
    }

    public void end(Player player) {
        DialogueSession session = sessions.remove(player.getUniqueId());
        if (session != null) {
            renderer.hide(session);
        }
        jumpSuppressor.restore(player);
    }

    public void forget(Player player) {
        sessions.remove(player.getUniqueId());
        jumpSuppressor.restore(player);
    }
}
