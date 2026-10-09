package com.goga221.dialoguehud.dialogue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
@AllArgsConstructor
public class DialogueOption {

    private final String text;

    @Nullable
    private final String nextLine;

    @Nullable
    private final String command;

    public boolean endsDialogue() {
        return nextLine == null;
    }

    public boolean hasCommand() {
        return command != null && !command.isBlank();
    }
}
