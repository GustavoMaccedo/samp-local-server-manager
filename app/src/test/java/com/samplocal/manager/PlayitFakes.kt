package com.samplocal.manager

import com.samplocal.manager.net.playit.PlayitAgent
import com.samplocal.manager.net.playit.PlayitApi
import org.json.JSONObject
import java.io.File

class MemorySecrets : com.samplocal.manager.util.SecretStore {
    val map = mutableMapOf<String, ByteArray>()
    override fun save(name: String, raw: ByteArray) { map[name] = raw }
    override fun load(name: String): ByteArray? = map[name]
    override fun remove(name: String) { map.remove(name) }
}

class FakeStarter(
    var alive: Boolean = true,
    var failStart: Boolean = false
) : PlayitAgent.ProcessStarter {
    var lastCmd: List<String>? = null
    var lastEnv: Map<String, String>? = null
    var stops = 0
    override fun start(cmd: List<String>, env: Map<String, String>): PlayitAgent.ManagedProcess {
        if (failStart) throw IllegalStateException("spawn falhou")
        lastCmd = cmd
        lastEnv = env
        val outer = this
        return object : PlayitAgent.ManagedProcess {
            override val alive get() = outer.alive
            override fun exitCode(): Int? = if (outer.alive) null else 0
            override fun stop() { stops++ }
        }
    }
}

class TestAgent(dir: File, fake: FakeStarter) : PlayitAgent(dir, dir) {
    init {
        starter = fake
    }

    override fun checkBinary(): Result<Unit> = Result.success(Unit)
}

class FakeApiTransport(val handler: (String, JSONObject) -> JSONObject) : PlayitApi.HttpTransport {
    val calls = mutableListOf<String>()
    override fun post(path: String, body: JSONObject, headers: Map<String, String>): JSONObject {
        calls.add(path)
        return handler(path, body)
    }
}
