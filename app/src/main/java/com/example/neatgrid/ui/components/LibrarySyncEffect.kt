package com.example.neatgrid.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.neatgrid.ui.screens.LibraryViewModel

@Composable
fun LibrarySyncEffect(viewModel: LibraryViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(viewModel, lifecycleOwner, context) {
        val packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent == null) return
                val packageName = intent.data?.schemeSpecificPart
                val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                when (intent.action) {
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        if (!replacing) packageName?.let(viewModel::handlePackageRemoved)
                    }

                    Intent.ACTION_PACKAGE_ADDED -> {
                        if (replacing) {
                            packageName?.let(viewModel::handlePackageUpdated)
                        } else {
                            viewModel.detectInstalledGames()
                        }
                    }
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_PACKAGE_REMOVED).apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(packageReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(packageReceiver, filter)
        }

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLibrary()
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            runCatching { context.unregisterReceiver(packageReceiver) }
        }
    }
}
