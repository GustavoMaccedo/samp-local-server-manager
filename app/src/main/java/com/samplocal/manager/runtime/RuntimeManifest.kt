package com.samplocal.manager.runtime

import org.json.JSONObject

data class RuntimeFileEntry(
    val path: String,
    val sha256: String,
    val size: Long,
    val executable: Boolean,

    val source: String = "asset"
) {
    val isNativeLib: Boolean get() = source == "nativelib"
}

data class RuntimeManifest(
    val version: String,
    val backend: String,
    val backendVersion: String,
    val guest: String,
    val files: List<RuntimeFileEntry>
) {
    companion object {
        const val MANIFEST_PATH = "manifest.json"

        fun parse(json: String): RuntimeManifest {
            val o = JSONObject(json)
            val version = o.optString("version", "").trim()
            require(version.isNotEmpty()) { "manifest sem version" }
            val files = mutableListOf<RuntimeFileEntry>()
            val arr = o.optJSONArray("files") ?: throw IllegalArgumentException("manifest sem files")
            for (i in 0 until arr.length()) {
                val f = arr.getJSONObject(i)
                val path = f.optString("path", "")
                val sha = f.optString("sha256", "")
                require(path.isNotEmpty() && sha.length == 64) { "entrada invalida no manifest: $path" }
                val source = f.optString("source", "asset")
                require(source == "asset" || source == "nativelib") { "source invalido: $source" }

                require(!path.startsWith("/") && ".." !in path && ":" !in path) {
                    "caminho inseguro no manifest: $path"
                }
                files.add(
                    RuntimeFileEntry(
                        path = path,
                        sha256 = sha.lowercase(),
                        size = f.optLong("size", -1),
                        executable = f.optBoolean("executable", false),
                        source = source
                    )
                )
            }
            require(files.isNotEmpty()) { "manifest sem arquivos" }
            return RuntimeManifest(
                version = version,
                backend = o.optString("backend", "qemu-i386"),
                backendVersion = o.optString("backendVersion", ""),
                guest = o.optString("guest", ""),
                files = files
            )
        }

        fun load(assets: AssetSource): RuntimeManifest =
            assets.open(MANIFEST_PATH).use { parse(it.readBytes().toString(Charsets.UTF_8)) }
    }
}
