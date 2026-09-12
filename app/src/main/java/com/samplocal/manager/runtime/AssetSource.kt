package com.samplocal.manager.runtime

import android.content.Context
import java.io.File
import java.io.InputStream

interface AssetSource {
    fun open(path: String): InputStream
    fun exists(path: String): Boolean
}

class AndroidAssetSource(private val context: Context) : AssetSource {
    override fun open(path: String): InputStream =
        context.assets.open("runtime/$path")

    override fun exists(path: String): Boolean {
        return try {
            context.assets.open("runtime/$path").use { true }
        } catch (_: Exception) {
            false
        }
    }
}

class RootAssetSource(private val context: Context) : AssetSource {
    override fun open(path: String): InputStream =
        context.assets.open(path)

    override fun exists(path: String): Boolean {
        return try {
            context.assets.open(path).use { true }
        } catch (_: Exception) {
            false
        }
    }
}

class DirAssetSource(private val root: File) : AssetSource {
    override fun open(path: String): InputStream =
        File(root, path).inputStream()

    override fun exists(path: String): Boolean =
        File(root, path).isFile
}
