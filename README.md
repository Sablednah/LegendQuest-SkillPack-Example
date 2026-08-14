# LegendQuest Example Skill Pack

The reference third-party skill pack for
[LegendQuest ReForged](https://github.com/Sablednah/LegendQuest-ReForged) —
three custom effect types, four skills, heavily commented. **Fork me.**

Skill packs are ordinary NeoForge mods. LegendQuest handles cooldowns, mana,
karma gates, level requirements, skill points, loadouts, hotkeys, the
handbook page and the buy button; your pack contributes only the one thing
LegendQuest can't know: *what your effect actually does*.

## The whole integration, in three steps

**1. Depend on LegendQuest** (`build.gradle` + `neoforge.mods.toml`):

```gradle
dependencies {
    implementation files('libs/legendquest-2.0.0-alpha.1.jar')
}
```

```toml
[[dependencies.examplepack]]
modId = "legendquest"
type = "required"
ordering = "AFTER"
```

**2. Register your effect types** in your mod constructor — one static call
each, no events, no registries of your own:

```java
SkillEffectTypes.register(Lifesteal.TYPE, Lifesteal.CODEC);
```

An effect is a record implementing `SkillEffect`: its fields are its YAML
keys (via the `MapCodec`), `type()` returns its id, `apply(SkillContext)`
does the work. The context gives you the caster, the level, the skill's
level, and (for triggered skills) the other party of the combat event.
`TargetSpec` is reusable — accept one as a field and your effect supports
`self` / `looking_at` / `nearby` / `trigger` / `party` targeting for free.

**3. Ship skills as data** at `data/<yourmodid>/legendquest/skill/*.json`.
They land in LegendQuest's skill registry under your namespace. Server
owners reference them from any race/class/feat (`examplepack:thunderclap`),
override them with YAML in `config/legendquest/skills/`, and see them in
the handbook automatically.

## What this pack ships

| Skill | Type | Shows off |
|---|---|---|
| **Vampiric Strike** | triggered (25% on melee hit) | `examplepack:lifesteal` — damage that feeds the caster; `trigger` targeting |
| **Thunderclap** | active | `examplepack:shockwave` — radial knockback + server-side particle ring |
| **Sixth Sense** | active | `examplepack:sense` — non-combat utility; counts every living thing nearby |
| **Storm Entrance** | active | **composition**: pack effects mixing with core LegendQuest effects (lightning, message) in one skill |

Give them to a class in two lines of that class's YAML:

```yaml
skills:
  examplepack:thunderclap: { level: 10, cost: 5 }
```

## Building

```
./gradlew build     # jar in build/libs/
```

Copy a current `legendquest-*.jar` into `libs/` first (from
LegendQuest-ReForged's `build/libs/`). Java 21, NeoForge 21.11.42.

## Rules of the road

- Register effect types in your **mod constructor** — datapack registries
  load after all constructors, and a skill referencing an unregistered type
  fails the world load loudly (by design: typos should not be silent).
- Effects run **server-side only**. If you want client flair, LegendQuest's
  core `sound`, `particle_line` and `lightning` effects cover most of it —
  compose rather than writing client code.
- Re-registering an existing id is refused with a logged error, so two packs
  can't silently fight over `yourmod:lifesteal`.

MIT licensed, like LegendQuest itself.
