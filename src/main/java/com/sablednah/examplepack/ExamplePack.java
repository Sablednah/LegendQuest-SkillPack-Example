package com.sablednah.examplepack;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * The reference LegendQuest skill pack. The entire integration is:
 *
 * <ol>
 *   <li>depend on the LegendQuest jar (see build.gradle + neoforge.mods.toml),</li>
 *   <li>call {@code SkillEffectTypes.register(id, codec)} for each new effect
 *       type in your mod constructor (before datapack registries load),</li>
 *   <li>ship skills as plain JSON in
 *       {@code data/<yourmodid>/legendquest/skill/*.json} — they land in
 *       LegendQuest's skill registry under YOUR namespace, and server owners
 *       can reference them from any race/class/feat (or override them with
 *       YAML in {@code config/legendquest/skills/}).</li>
 * </ol>
 *
 * <p>Everything else — cooldowns, mana, karma gates, loadouts, hotkeys, the
 * handbook page, the buy chip — LegendQuest provides for free. Your effect
 * only ever sees a {@code SkillContext} and does its one thing.</p>
 */
@Mod(ExamplePack.MODID)
public class ExamplePack {

    public static final String MODID = "examplepack";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ExamplePack() {
        // Registration is a plain static call — no events, no registries of
        // your own. Must happen before world load; the mod constructor is
        // the right place.
        ExampleEffects.registerAll();
        LOGGER.info("Example skill pack: registered {} effect types", 3);
    }
}
