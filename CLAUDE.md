# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

DialogueHud: a standalone Minecraft plugin/server project targeting **Folia + Paper compatibility**.
It implements a scroll-to-navigate, space-to-select, shift-to-exit dialogue system rendered entirely
through BetterHud, kept separate from the text chat.

## Requirements

- Java 21
- Maven (build tool)
- Must remain compatible with both Folia and Paper (avoid APIs that break under Folia's regionized threading model — e.g. no global `BukkitScheduler` assumptions; use the scheduler abstraction below)

## Dependencies

- CommandAPI — command registration/handling (external plugin dependency, not shaded)
- BetterHud — renders the dialogue box/options (external plugin dependency, not shaded); see `kr.toxicity.hud.api.*`.
  Pinned to 1.14.1 (`BetterHud-bukkit-api` + `BetterHud-standard-api` + `BetterCommand`, all `provided`) — 2.0.0+'s
  core classes are compiled for Java 25 and won't even load on this project's Java 21 toolchain/server.
- FancyNpcs — hosts the NPC a player interacts with to trigger a dialogue (external plugin dependency, not shaded).
  Every FancyNpcs build on Maven/its own repo is also Java 25-only; `libs/FancyNpcs-2.10.1-java21.jar` (FancyNpcs'
  own Java-21-targeted Modrinth build) is checked into this repo and referenced via a `system`-scope dependency —
  before bumping this project to Java 25, check whether that's still necessary.
- UniversalScheduler — Folia/Paper-safe task scheduling (always use this instead of raw Bukkit scheduler calls)
- Lombok — reduce boilerplate (getters/setters/builders/etc.)
- Adventure text-minimessage — command feedback text uses MiniMessage via Adventure, not legacy `§` color codes; the dialogue content itself is never sent through chat/Adventure, only through BetterHud

## Visuals

The BetterHud popup/layout config (`plugins/BetterHud/{popups,layouts}/dialogue-*.yml` on the server) comes from
the purchased "BetterHud Dialogues" asset pack at `C:\BetterHudDialogues`, originally built for the Typewriter
BetterHudExtension. This project does not use Typewriter: the layout's `[custom_variable:...]` pattern anchors
were repointed at DialogueHud's own globally-registered placeholders (registered in `BetterHudDialogueRenderer`)
and the `dialogue_options`/`option_scroll` visibility conditions now check `dialogue_active` instead of the
Typewriter-only `is_complete`/`papi:typewriter_in_dialogue`.

The pack ships 5 box-size variants per popup family - `tw_option_1`..`tw_option_5`, backed by
`dialogue_background_1.png`..`_5.png` at 21px/31px/41px/51px/61px tall (a `_6` at 71px exists as an asset but
isn't wired into any layout entry). In the original Typewriter workflow the story author picked the variant
matching how many lines that node's text needed; `BetterHudDialogueRenderer.popupNameFor()` now does that
automatically from `text.length() / CHARS_PER_LINE` (a rough, deliberately-conservative per-line character
estimate, not a real measurement of BetterHud's font-metric text wrapping) clamped to 1-5. Moving to a line that
needs a different-sized box hides the current popup and shows the new one (`refresh()` compares the needed name
against `DialogueSession.activePopupName` and calls `show()` again on a mismatch) - a plain `updater.update()`
can't swap which popup is displayed. If dialogue text still overflows in practice, tune `CHARS_PER_LINE` down
(smaller = triggers a bigger box sooner) rather than assuming the wrapping math needs to be exact.

**BetterHud placeholder bracket syntax is `[placeholder_name:arg1,arg2]`, NOT `[type:placeholder_name]`.**
`string`/`number`/`boolean` are not type prefixes you write in a reference — they're just which container you
register a placeholder into (`getStringContainer()` etc. in `PlaceholderManager`), and `string`/`boolean` also
happen to be the *names* of BetterHud's own built-in placeholders that read a per-player local-variable map. So
`[string:dialogue_speaker]` doesn't mean "the string placeholder named dialogue_speaker" — it invokes the builtin
placeholder literally named `string`, passing `dialogue_speaker` as its argument, which silently renders `<none>`
since nothing populates that variable map. The fix: reference registered placeholders by their bare name, e.g.
`[dialogue_speaker]`, `[dialogue_text:some_arg]`. The `(boolean)`/`(number)`/`(string)` prefix seen in conditions
(`(boolean)dialogue_active`) is a separate, legitimate feature — an optional type-cast applied to whatever bare
placeholder name follows it — don't confuse the two.

The pack's "skip/continue" hint text elements were removed entirely (not just left unreferenced) because
referencing the nonexistent `papi:typewriter_in_dialogue`/`custom_variable:*` placeholders without Typewriter and
PlaceholderAPI installed doesn't fail gracefully — BetterHud throws inside its per-player render loop the moment
that layout actually renders, permanently killing that loop for the affected player. Two places used the old
`is_complete` gate: the `dialogue_options` text element AND, easy to miss, the `option_scroll` image element right
below `images:` in every `typewriter_dialogue_option_N` layout. If this layout is ever touched again, grep for
`custom_variable`, `papi`, and any `string:`/`boolean:`/`number:` prefix before assuming a partial fix was complete.

## Space-to-select without jumping

`JumpSuppressor` adds/removes a **transient** `AttributeModifier` (`MULTIPLY_SCALAR_1`, amount `-1.0`) on
`Attribute.JUMP_STRENGTH` for the session's duration - this is what actually makes pressing space produce no
visible movement. Two earlier approaches were tried and discarded first:
- Cancelling `PlayerJumpEvent` (and/or zeroing velocity in its handler): didn't reliably suppress the
  client-predicted jump on Folia in practice, despite Paper's docs claiming a cancelled jump teleports the
  player back to `getFrom()`.
- Calling `AttributeInstance.setBaseValue(0)` directly: this **did** stop the jump, but a base-value change is
  part of the player's persistent saved data. If `end()`/`restore()` is ever skipped (a crash, a bug, a session
  never properly closing) that 0 sticks forever - a real player got stuck permanently unable to jump this way
  during development. `addTransientModifier` never touches base value and is never written to disk, so the
  exact same failure mode is now inert: worst case is a modifier that outlives its session in memory until the
  player's next relog, never a permanently broken attribute. Never go back to mutating base value for this.

Zeroing jump strength has a side effect: with no physical jump occurring, `PlayerJumpEvent` (Paper's "the server
detected an actual jump" event) **stops firing entirely** - space appeared to do nothing in survival, and in
creative it took two presses because the first was invisible to us and the second tripped vanilla's own
double-tap-space flight toggle (an unrelated, non-physics-based detection). `DialogueInputListener` therefore
listens for `PlayerInputEvent` / `Input.isJump()` instead - Paper's raw per-tick client input state, independent
of whatever the resulting physics does - and manually tracks a per-player held-key set to turn that continuous
state into a single rising-edge trigger. If jump suppression is ever reworked, keep detection on the raw input
event, not on anything that depends on a jump actually happening.

### If a database is needed

- HikariCP — connection pooling
- sqlite-jdbc — SQLite driver

(Not currently used — dialogues are stored as YAML files.)

## Architecture

- Strict OOP: favor clear class hierarchies, encapsulation, and single-responsibility classes over procedural/utility-dump style code.

## Author preferences

- Refer to the user as goga221.
