package com.example.neatgrid.data

import java.util.Locale

object EmulatorApps {

    private val knownPackages: Set<String> =
        Emulator.entries.map { it.packageName }.toSet() + setOf(
            "com.retroarch.ra32",
            "me.magnum.melonds",
            "io.github.lime3ds.android",
            "io.recompiled.redream",
            "org.mupen64plusae.v3.fzurita",
            "com.iisulauncher",
            "com.magneticchen.daijishou",
            "org.es_de.frontend",
            "org.pegasus_frontend.android"
        )

    private val knownPrefixes = listOf("com.explusalpha.")

    private val nameHints = listOf("emulator", "_emu")

    fun isEmulatorOrFrontend(packageName: String): Boolean {
        val normalized = packageName.lowercase(Locale.ROOT)
        return normalized in knownPackages ||
                knownPrefixes.any { normalized.startsWith(it) } ||
                nameHints.any { it in normalized }
    }
}
