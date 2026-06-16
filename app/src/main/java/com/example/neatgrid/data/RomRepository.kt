package com.example.neatgrid.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.util.Locale

data class RomFile(
    val name: String,
    val uriString: String,
    val extension: String,
    val matchingEmulator: Emulator?
)

data class RomData(
    val emulatorPackage: String,
    val label: String,
    val uriString: String
)

class RomRepository {

    fun scanRomFolder(context: Context, folderUriString: String): List<RomFile> {
        if (folderUriString.isEmpty()) return emptyList()
        val result = mutableListOf<RomFile>()
        
        try {
            val rootUri = Uri.parse(folderUriString)
            val rootId = try {
                DocumentsContract.getTreeDocumentId(rootUri)
            } catch (e: Exception) {
                DocumentsContract.getDocumentId(rootUri)
            }
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, rootId)
            
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )
            
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                
                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idCol)
                    val name = cursor.getString(nameCol)
                    val mimeType = cursor.getString(mimeCol)
                    
                    if (mimeType != DocumentsContract.Document.MIME_TYPE_DIR) {
                        val fileUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, docId)
                        val extension = name.substringAfterLast('.', "").lowercase(Locale.getDefault())
                        val matchingEmulator = Emulator.getEmulatorForExtension(context, extension)
                        result.add(
                            RomFile(
                                name = name,
                                uriString = fileUri.toString(),
                                extension = extension,
                                matchingEmulator = matchingEmulator
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    companion object {
        fun buildPackageName(emulatorPackage: String, label: String, uri: String): String {
            return "rom:$emulatorPackage|$label|$uri"
        }

        fun isRom(packageName: String): Boolean {
            return packageName.startsWith("rom:")
        }

        fun parse(packageName: String): RomData? {
            if (!isRom(packageName)) return null
            try {
                val content = packageName.substring(4)
                val parts = content.split("|")
                if (parts.size >= 3) {
                    val emulatorPackage = parts[0]
                    val label = parts[1]
                    val uri = parts.subList(2, parts.size).joinToString("|")
                    return RomData(emulatorPackage, label, uri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        fun launchRom(context: Context, romData: RomData): Boolean {
            return try {
                if (romData.emulatorPackage == "com.swordfish.lemuroid") {
                    val pm = context.packageManager
                    val intent = pm.getLaunchIntentForPackage(romData.emulatorPackage)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        return true
                    }
                    return false
                }

                val intent = Intent(Intent.ACTION_VIEW)
                val uri = Uri.parse(romData.uriString)
                intent.setData(uri)
                intent.setPackage(romData.emulatorPackage)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                // Safety fallback: launch the emulator application itself
                try {
                    val pm = context.packageManager
                    val intent = pm.getLaunchIntentForPackage(romData.emulatorPackage)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        return true
                    }
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
                false
            }
        }
    }
}