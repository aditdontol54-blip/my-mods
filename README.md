# Chrono Arsenal

A NeoForge 1.20.1 mod with five futuristic weapons. It also runs on Forge 1.20.1.

| Weapon | Power | Right-click | Sneak + Right-click |
|---|---|---|---|
| Chrono Blade | Time | **Time Stop**: freeze everything within 12 blocks for 5s | **Rewind**: go back to where you were 5s ago and get lost health back |
| Plasma Repeater | Plasma | Hold for full-auto plasma fire (watch the heat bar) | Overcharge: stronger shots, double heat |
| Graviton Maul | Gravity | **Singularity**: a black hole that pulls everything in, then implodes | **Repulsor**: shockwave that also reflects projectiles |
| Rift Edge | Space | **Phase Blink**: dash 14 blocks, slashing everything in the way | **Void Swap**: trade places with the creature you look at |
| Arc Caster | Lightning | **Chain Lightning**: jumps between up to 6 enemies | **Storm Call**: real lightning where you look |

Every weapon has its own creative tab entry ("Chrono Arsenal") and a crafting recipe built around the **Quantum Core**
(amethyst shards, redstone and a diamond).

## Getting the jar (GitHub)

1. Upload everything in this folder (including the hidden `.github` folder) to a GitHub repository.
2. Open the **Actions** tab and wait for the green check (about 5–10 minutes).
3. Click the finished run, then download **chronoarsenal-jar** under *Artifacts*. Unzip it to get the `.jar`.
4. Put the `.jar` in your `.minecraft/mods` folder and launch Minecraft 1.20.1 with NeoForge.

## Building on your own PC

With JDK 17 installed, run `gradlew build` in this folder. The jar ends up in `build/libs/`.
