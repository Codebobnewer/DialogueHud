package com.goga221.dialoguehud.dialogue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Getter
@AllArgsConstructor
public class Dialogue {

    private final String name;
    private final String speaker;
    private final String startLine;
    private final Map<String, DialogueLine> lines;

    @Nullable
    public DialogueLine getLine(String id) {
        return lines.get(id);
    }

    @Nullable
    public DialogueLine getStart() {
        return lines.get(startLine);
    }
}
