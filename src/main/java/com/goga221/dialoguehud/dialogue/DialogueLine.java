package com.goga221.dialoguehud.dialogue;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DialogueLine {

    private final String id;
    private final String text;
    private final List<DialogueOption> options;

    public int getOptionCount() {
        return options.size();
    }

    public DialogueOption getOption(int index) {
        return options.get(index);
    }
}
