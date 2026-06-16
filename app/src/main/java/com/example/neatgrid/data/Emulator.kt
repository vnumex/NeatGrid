package com.example.neatgrid.data

import android.content.Context
import android.content.pm.PackageManager

enum class Emulator(
    val label: String,
    val packageName: String,
    val systems: List<String>,
    val extensions: List<String>
) {
    PPSSPP("PPSSPP", "org.ppsspp.ppsspp", listOf("PSP"), listOf("iso", "cso", "pbp", "elf")),
    PPSSPP_GOLD("PPSSPP Gold", "org.ppsspp.ppssppgold", listOf("PSP"), listOf("iso", "cso", "pbp", "elf")),
    DOLPHIN("Dolphin", "org.dolphinemu.dolphinemu", listOf("GameCube", "Wii"), listOf("iso", "gcm", "wbfs", "rvz", "dol")),
    DUCKSTATION("DuckStation", "com.github.stenzek.duckstation", listOf("PlayStation 1"), listOf("cue", "bin", "chd", "iso")),
    AETHERSX2("AetherSX2", "xyz.aethersx2.android", listOf("PlayStation 2"), listOf("iso", "chd", "cso")),
    AETHERSX2_FREE("AetherSX2 Free", "com.tahlreth.aethersx2.free", listOf("PlayStation 2"), listOf("iso", "chd", "cso")),
    CITRA("Citra", "org.citra.citra_emu", listOf("Nintendo 3DS"), listOf("3ds", "cci", "cxi")),
    YUZU("Yuzu", "org.yuzu.yuzu_emu", listOf("Nintendo Switch"), listOf("nsp", "xci")),
    DRASTIC("DraStic", "com.dsemu.drastic", listOf("Nintendo DS"), listOf("nds", "zip")),
    MY_BOY("My Boy!", "com.fastemulator.gba", listOf("Game Boy Advance"), listOf("gba")),
    SNES9X("Snes9x EX+", "com.snes9x.ex", listOf("Super Nintendo"), listOf("smc", "sfc", "fig")),
    MUPEN64("Mupen64Plus FZ", "org.mupen64plusae.v3.fivid", listOf("Nintendo 64"), listOf("z64", "n64", "v64")),
    RETROARCH("RetroArch", "com.retroarch", listOf("Multi-System"), listOf("bin", "zip", "nes", "sfc", "gba", "gbc", "md")),
    RETROARCH_64("RetroArch Plus", "com.retroarch.aarch64", listOf("Multi-System"), listOf("bin", "zip", "nes", "sfc", "gba", "gbc", "md")),
    LEMUROID("Lemuroid", "com.swordfish.lemuroid", listOf("Multi-System"), listOf("bin", "zip", "nes", "sfc", "gba", "gbc", "md"));

    companion object {
        fun getInstalledEmulators(context: Context): List<Emulator> {
            val pm = context.packageManager
            return values().filter { emulator ->
                try {
                    pm.getPackageInfo(emulator.packageName, 0)
                    true
                } catch (e: PackageManager.NameNotFoundException) {
                    false
                }
            }
        }

        fun getEmulatorForExtension(context: Context, extension: String): Emulator? {
            val installed = getInstalledEmulators(context)
            // First look for a specific emulator matching this extension
            val matched = installed.firstOrNull { it.extensions.contains(extension) && it != RETROARCH && it != RETROARCH_64 && it != LEMUROID }
            if (matched != null) return matched
            // Fallback to general emulators
            return installed.firstOrNull { it.extensions.contains(extension) }
        }
    }
}
