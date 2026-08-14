package com.sablednah.examplepack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sablednah.legendquest.skills.SkillContext;
import com.sablednah.legendquest.skills.SkillEffect;
import com.sablednah.legendquest.skills.SkillEffectTypes;
import com.sablednah.legendquest.skills.TargetSpec;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Three custom effect types, chosen to show the whole surface area:
 *
 * <ul>
 *   <li>{@link Lifesteal} — targeted damage that feeds the caster (the old
 *       LifeLink / every vampire trope). Shows: TargetSpec reuse, hurting,
 *       healing, scaling a number off another number.</li>
 *   <li>{@link Shockwave} — radial knockback (the old Hadouken). Shows:
 *       area effects, velocity manipulation, server-side particles.</li>
 *   <li>{@link Sense} — a report of living things nearby (the old
 *       Detect/Tracker/Hound family). Shows: non-combat utility and
 *       message output.</li>
 * </ul>
 *
 * <p>An effect is a record implementing {@link SkillEffect}: its fields ARE
 * its YAML keys (via the MapCodec), {@code type()} returns its id, and
 * {@code apply(SkillContext)} does the thing. That's the entire contract.</p>
 */
public final class ExampleEffects {

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ExamplePack.MODID, path);
    }

    /** Called once from the mod constructor. */
    public static void registerAll() {
        SkillEffectTypes.register(Lifesteal.TYPE, Lifesteal.CODEC);
        SkillEffectTypes.register(Shockwave.TYPE, Shockwave.CODEC);
        SkillEffectTypes.register(Sense.TYPE, Sense.CODEC);
    }

    /**
     * Damage the target; heal the caster for a fraction of it.
     *
     * <pre>
     * { "type": "examplepack:lifesteal", "amount": 5.0, "leech": 0.5,
     *   "target": { "kind": "trigger" } }
     * </pre>
     */
    public record Lifesteal(float amount, float leech, TargetSpec target) implements SkillEffect {
        public static final Identifier TYPE = id("lifesteal");
        public static final MapCodec<Lifesteal> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.FLOAT.fieldOf("amount").forGetter(Lifesteal::amount),
                Codec.FLOAT.optionalFieldOf("leech", 0.5F).forGetter(Lifesteal::leech),
                TargetSpec.CODEC.optionalFieldOf("target", TargetSpec.LOOKING_AT)
                        .forGetter(Lifesteal::target))
                .apply(i, Lifesteal::new));

        @Override public Identifier type() { return TYPE; }

        @Override
        public void apply(SkillContext ctx) {
            for (LivingEntity victim : target.resolveEntities(ctx)) {
                victim.hurtServer(ctx.level(),
                        ctx.level().damageSources().indirectMagic(ctx.caster(), ctx.caster()),
                        amount);
                ctx.caster().heal(amount * leech);
                // A little theatre goes a long way.
                ctx.level().sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
                        6, 0.3, 0.3, 0.3, 0.05);
                ctx.level().sendParticles(ParticleTypes.HEART,
                        ctx.caster().getX(), ctx.caster().getY() + 1.5, ctx.caster().getZ(),
                        2, 0.2, 0.2, 0.2, 0.0);
            }
        }
    }

    /**
     * Fling everything near the caster away from them.
     *
     * <pre>
     * { "type": "examplepack:shockwave", "radius": 5.0, "power": 1.2, "lift": 0.5 }
     * </pre>
     */
    public record Shockwave(double radius, double power, double lift) implements SkillEffect {
        public static final Identifier TYPE = id("shockwave");
        public static final MapCodec<Shockwave> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("radius", 5.0D).forGetter(Shockwave::radius),
                Codec.DOUBLE.optionalFieldOf("power", 1.2D).forGetter(Shockwave::power),
                Codec.DOUBLE.optionalFieldOf("lift", 0.5D).forGetter(Shockwave::lift))
                .apply(i, Shockwave::new));

        @Override public Identifier type() { return TYPE; }

        @Override
        public void apply(SkillContext ctx) {
            Vec3 centre = ctx.caster().position();
            AABB box = ctx.caster().getBoundingBox().inflate(radius);
            for (LivingEntity e : ctx.level().getEntitiesOfClass(LivingEntity.class, box)) {
                if (e == ctx.caster() || !e.isAlive()) continue;
                Vec3 away = e.position().subtract(centre);
                Vec3 dir = away.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : away.normalize();
                e.setDeltaMovement(dir.x * power, lift, dir.z * power);
                e.hurtMarked = true; // or the client never sees the shove
            }
            // A ring of clouds racing outward, drawn server-side.
            for (int deg = 0; deg < 360; deg += 15) {
                double rad = Math.toRadians(deg);
                ctx.level().sendParticles(ParticleTypes.CLOUD,
                        centre.x + Math.cos(rad) * 1.5, centre.y + 0.2,
                        centre.z + Math.sin(rad) * 1.5,
                        1, Math.cos(rad) * 0.3, 0.02, Math.sin(rad) * 0.3, 0.15);
            }
        }
    }

    /**
     * Report every living thing within radius, grouped and counted — the
     * ranger's sixth sense, no combat involved.
     *
     * <pre>
     * { "type": "examplepack:sense", "radius": 24.0 }
     * </pre>
     */
    public record Sense(double radius) implements SkillEffect {
        public static final Identifier TYPE = id("sense");
        public static final MapCodec<Sense> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("radius", 24.0D).forGetter(Sense::radius))
                .apply(i, Sense::new));

        @Override public Identifier type() { return TYPE; }

        @Override
        public void apply(SkillContext ctx) {
            Map<String, Integer> counts = new HashMap<>();
            AABB box = ctx.caster().getBoundingBox().inflate(radius);
            for (LivingEntity e : ctx.level().getEntitiesOfClass(LivingEntity.class, box)) {
                if (e == ctx.caster()) continue;
                counts.merge(e.getType().getDescription().getString(), 1, Integer::sum);
            }
            if (counts.isEmpty()) {
                ctx.caster().displayClientMessage(
                        Component.literal("§7You sense nothing nearby. Suspicious."), false);
                return;
            }
            StringBuilder sb = new StringBuilder("§6You sense:§r");
            counts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> sb.append("\n §7-§f ").append(entry.getValue())
                            .append("× ").append(entry.getKey()));
            ctx.caster().displayClientMessage(Component.literal(sb.toString()), false);
        }
    }

    private ExampleEffects() {}
}
