package com.goga221.dialoguehud.listener;

import com.goga221.dialoguehud.session.DialogueSessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repurposes the hotbar scroll wheel, jump key and sneak key as dialogue navigation:
 * scroll to move the highlighted option, space to select it, shift to exit at any time.
 * This keeps dialogue entirely off the text chat, per the plugin's design.
 */
public final class DialogueInputListener implements Listener {

    private final DialogueSessionManager sessionManager;

    // Tracks players currently holding the jump key, to turn PlayerInputEvent's continuous
    // per-tick state into a single "just pressed" trigger (a rising edge) instead of firing
    // once per tick for as long as the key is held.
    private final Set<UUID> jumpHeld = ConcurrentHashMap.newKeySet();

    public DialogueInputListener(DialogueSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onScroll(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!sessionManager.hasSession(player.getUniqueId())) {
            return;
        }

        int previous = event.getPreviousSlot();
        int next = event.getNewSlot();

        int forwardDistance = Math.floorMod(next - previous, 9);
        int backwardDistance = Math.floorMod(previous - next, 9);
        int direction = forwardDistance <= backwardDistance ? 1 : -1;

        sessionManager.scroll(player, direction);

        // Deliberately not resetting the held slot back to `previous` here: the client has
        // already applied the scroll locally, and correcting it produces a visible flicker
        // as the client's predicted slot and the server's correction fight each other on
        // fast scrolling. Letting the hotbar itself visibly scroll during dialogue is a
        // minor cosmetic trade-off, but reliable.
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInput(PlayerInputEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!sessionManager.hasSession(uuid)) {
            jumpHeld.remove(uuid);
            return;
        }

        boolean jumping = event.getInput().isJump();
        boolean wasJumping = jumpHeld.contains(uuid);

        if (jumping && !wasJumping) {
            // Space also doubles as jump; DialogueSessionManager zeroes JUMP_STRENGTH for
            // the session so pressing it produces no visible movement, but that also means
            // PlayerJumpEvent never fires - this raw input event reports the key press
            // regardless of whether an actual jump results from it.
            sessionManager.select(player);
        }

        if (jumping) {
            jumpHeld.add(uuid);
        } else {
            jumpHeld.remove(uuid);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }

        Player player = event.getPlayer();
        if (!sessionManager.hasSession(player.getUniqueId())) {
            return;
        }

        sessionManager.end(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        jumpHeld.remove(event.getPlayer().getUniqueId());
        sessionManager.forget(event.getPlayer());
    }
}
