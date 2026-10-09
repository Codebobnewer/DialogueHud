package com.goga221.dialoguehud.session;

import com.goga221.dialoguehud.dialogue.Dialogue;
import com.goga221.dialoguehud.dialogue.DialogueLine;
import kr.toxicity.hud.api.popup.PopupUpdater;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
public final class DialogueSession {

    private final UUID playerId;
    private final Dialogue dialogue;

    @Setter
    private DialogueLine currentLine;

    @Setter
    private int selectedIndex;

    @Setter
    @Nullable
    private PopupUpdater popupUpdater;

    @Setter
    @Nullable
    private String activePopupName;

    public DialogueSession(UUID playerId, Dialogue dialogue, DialogueLine currentLine) {
        this.playerId = playerId;
        this.dialogue = dialogue;
        this.currentLine = currentLine;
        this.selectedIndex = 0;
    }
}
