package com.goga221.dialoguehud.hud;

import com.goga221.dialoguehud.dialogue.DialogueLine;
import com.goga221.dialoguehud.session.DialogueSession;
import com.goga221.dialoguehud.session.DialogueSessionManager;
import kr.toxicity.hud.api.BetterHudAPI;
import kr.toxicity.hud.api.placeholder.HudPlaceholder;
import kr.toxicity.hud.api.player.HudPlayer;
import kr.toxicity.hud.api.popup.Popup;
import kr.toxicity.hud.api.popup.PopupUpdater;
import kr.toxicity.hud.api.update.UpdateEvent;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

/**
 * Bridges dialogue state to BetterHud, targeting the "BetterHud Dialogues" asset pack's
 * tw_option_N popups/layouts (plugins/BetterHud/{popups,layouts}/dialogue-*.yml). Those
 * layouts were originally written for the Typewriter BetterHudExtension and fed via
 * [custom_variable:...]; this plugin has no Typewriter dependency, so the layout's pattern
 * anchors were repointed at the globally-registered placeholders below instead.
 *
 * The pack ships 5 box-size variants per popup family (tw_option_1..5, backing images
 * 21px-61px tall) meant to be picked by how many lines the spoken text wraps into - the
 * story author picked one by hand in the original Typewriter workflow. This renderer picks
 * one automatically from the current line's text length instead.
 */
public final class BetterHudDialogueRenderer {

    private static final String POPUP_PREFIX = "tw_option_";
    private static final int MIN_VARIANT = 1;
    private static final int MAX_VARIANT = 5;

    // Rough estimate of characters per wrapped line at this layout's split-width/scale.
    // Deliberately conservative (undercounting) so text is more likely to end up in a
    // taller box than needed rather than overflow a too-small one again.
    private static final int CHARS_PER_LINE = 24;

    private final Logger logger;

    public BetterHudDialogueRenderer(Logger logger) {
        this.logger = logger;
    }

    public void registerPlaceholders(DialogueSessionManager sessionManager) {
        var placeholderManager = BetterHudAPI.inst().getPlaceholderManager();

        HudPlaceholder.<String>builder()
                .function((args, reason) -> hudPlayer -> {
                    DialogueSession session = sessionManager.getSession(hudPlayer.uuid());
                    return session != null ? session.getDialogue().getSpeaker() : "";
                })
                .add("dialogue_speaker", placeholderManager.getStringContainer());

        HudPlaceholder.<String>builder()
                .function((args, reason) -> hudPlayer -> {
                    DialogueSession session = sessionManager.getSession(hudPlayer.uuid());
                    return session != null ? session.getCurrentLine().getText() : "";
                })
                .add("dialogue_text", placeholderManager.getStringContainer());

        HudPlaceholder.<String>builder()
                .function((args, reason) -> hudPlayer -> resolveOptionText(sessionManager, hudPlayer, -1))
                .add("dialogue_prev_option", placeholderManager.getStringContainer());

        HudPlaceholder.<String>builder()
                .function((args, reason) -> hudPlayer -> resolveOptionText(sessionManager, hudPlayer, 0))
                .add("dialogue_selected_option", placeholderManager.getStringContainer());

        HudPlaceholder.<String>builder()
                .function((args, reason) -> hudPlayer -> resolveOptionText(sessionManager, hudPlayer, 1))
                .add("dialogue_next_option", placeholderManager.getStringContainer());

        HudPlaceholder.<Boolean>builder()
                .function((args, reason) -> hudPlayer -> sessionManager.getSession(hudPlayer.uuid()) != null)
                .add("dialogue_active", placeholderManager.getBooleanContainer());
    }

    private static String resolveOptionText(DialogueSessionManager sessionManager, HudPlayer hudPlayer, int offset) {
        DialogueSession session = sessionManager.getSession(hudPlayer.uuid());
        if (session == null) {
            return "";
        }

        DialogueLine line = session.getCurrentLine();
        int optionCount = line.getOptionCount();
        int index = session.getSelectedIndex() + offset;
        if (index < 0 || index >= optionCount) {
            return "";
        }

        return line.getOption(index).getText();
    }

    private static String popupNameFor(String text) {
        int estimatedLines = Math.max(1, (int) Math.ceil(text.length() / (double) CHARS_PER_LINE));
        int variant = Math.clamp(estimatedLines, MIN_VARIANT, MAX_VARIANT);
        return POPUP_PREFIX + variant;
    }

    /**
     * Shows (or, if the current line needs a differently-sized box than what's already
     * showing, re-shows) the popup for the session's current line.
     */
    public boolean show(Player player, DialogueSession session) {
        String popupName = popupNameFor(session.getCurrentLine().getText());

        Popup popup = BetterHudAPI.inst().getPopupManager().getPopup(popupName);
        if (popup == null) {
            logger.warning("BetterHud popup '" + popupName + "' was not found. Make sure the BetterHud Dialogues "
                    + "config was merged into plugins/BetterHud.");
            return false;
        }

        HudPlayer hudPlayer = BetterHudAPI.inst().getPlayerManager().getHudPlayer(player.getUniqueId());
        if (hudPlayer == null) {
            logger.warning("No BetterHud player data for " + player.getName() + " yet.");
            return false;
        }

        hide(session);

        PopupUpdater updater = popup.show(UpdateEvent.EMPTY, hudPlayer);
        if (updater == null) {
            logger.warning("Failed to show dialogue popup to " + player.getName() + ".");
            return false;
        }

        session.setPopupUpdater(updater);
        session.setActivePopupName(popupName);
        return true;
    }

    /**
     * Refreshes the currently-shown popup for placeholder changes, switching to a
     * differently-sized box first if the current line no longer fits the one already shown.
     */
    public void refresh(Player player, DialogueSession session) {
        String neededPopupName = popupNameFor(session.getCurrentLine().getText());
        if (!neededPopupName.equals(session.getActivePopupName())) {
            show(player, session);
            return;
        }

        PopupUpdater updater = session.getPopupUpdater();
        if (updater != null) {
            updater.update();
        }
    }

    public void hide(DialogueSession session) {
        PopupUpdater updater = session.getPopupUpdater();
        if (updater != null) {
            updater.remove();
            session.setPopupUpdater(null);
            session.setActivePopupName(null);
        }
    }
}
