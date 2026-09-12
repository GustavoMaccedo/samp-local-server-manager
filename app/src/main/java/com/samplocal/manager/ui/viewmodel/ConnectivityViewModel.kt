package com.samplocal.manager.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samplocal.manager.SampApp
import com.samplocal.manager.net.ConnectivityMode
import com.samplocal.manager.net.ConnectivityRepository
import com.samplocal.manager.ui.strings.Strings
import com.samplocal.manager.ui.strings.stringsFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectivityViewModel(app: Application) : AndroidViewModel(app) {

    private val sampApp get() = getApplication<SampApp>()
    private val repo: ConnectivityRepository get() = sampApp.connectivity

    val state get() = repo.state
    val logs get() = repo.logs

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _testing = MutableStateFlow(false)
    val testing: StateFlow<Boolean> = _testing.asStateFlow()

    fun start(mainVm: MainViewModel) {
        repo.bind(mainVm.selected)
        repo.startMonitoring()
        repo.refresh(quick = true)
    }

    fun stop() {
        repo.stopMonitoring()
    }

    fun setMode(mode: ConnectivityMode) = repo.setMode(mode)

    fun test() {
        viewModelScope.launch {
            _testing.value = true
            try {
                repo.testConnectivity()
            } finally {

                kotlinx.coroutines.delay(500)
                _testing.value = false
            }
        }
    }

    fun refresh() = repo.refresh(quick = false)

    fun testRelay() = repo.testRelay()

    fun restartConnection() = repo.restartConnection()

    fun infraSource(): String? = repo.infraSource()

    fun setInfraSource(url: String) {
        repo.setInfraSource(url)
        _message.value = uiStrings().mSrcUpdated
    }

    fun clearRelay() = repo.clearRelay()

    fun playitLinked(): Boolean = try {
        repo.playit.isLinked()
    } catch (_: Exception) { false }

    fun playitAgentAlive(): Boolean = try {
        repo.playit.agentAlive()
    } catch (_: Exception) { false }

    val claim: StateFlow<ConnectivityRepository.ClaimUi?> = repo.claim

    fun startClaim() {
        repo.startClaim()
    }

    fun cancelClaim() {
        repo.cancelClaim()
    }

    fun copyClaimUrl(ctx: Context, url: String) {
        try {
            val cm = ctx.getSystemService(ClipboardManager::class.java)
            cm?.setPrimaryClip(ClipData.newPlainText("playit", url))
            _message.value = uiStrings().mLinkCopied
        } catch (_: Exception) { }
    }

    fun unlinkPlayit() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.unlinkPlayit()
                _message.value = uiStrings().mUnlinked
                repo.refresh(quick = true)
            } catch (e: Exception) {
                _message.value = uiStrings().mFail.format(e.message)
            }
        }
    }

    fun relinkPlayit() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.unlinkPlayit()
                _message.value = uiStrings().mRelinked
                repo.startClaim()
            } catch (e: Exception) {
                _message.value = uiStrings().mFail.format(e.message)
            }
        }
    }

    val agentProc: StateFlow<ConnectivityRepository.AgentProc> = repo.agentProc
    val agentDetail: StateFlow<String> = repo.agentDetail

    fun proxyStatus(): String = try {
        repo.proxyStatus()
    } catch (_: Exception) { "—" }

    fun copyAddress(ctx: Context) {
        val addr = repo.copyAddress()
        if (addr == null) {
            _message.value = uiStrings().mNoExt
            return
        }
        try {
            val cm = ctx.getSystemService(ClipboardManager::class.java)
            cm?.setPrimaryClip(ClipData.newPlainText("endereco", addr))
            _message.value = uiStrings().mAddrCopied.format(addr)
        } catch (_: Exception) {
            _message.value = addr
        }
    }

    fun consumeMessage() { _message.value = null }

    fun uiStrings(): Strings = stringsFor(sampApp.appPrefs.language.value)

    fun sessionIdShort(): String = try {
        repo.signaling.sessionId.take(8)
    } catch (_: Exception) { "—" }

    fun exportOffer(ctx: Context, port: Int) {
        try {
            val st = state.value
            val cands = buildList {
                st.localIp?.let { ip ->
                    add(
                        com.samplocal.manager.net.IceCandidate(
                            com.samplocal.manager.net.CandidateType.HOST, ip, port,
                            "h${ip.hashCode() and 0xFFFF}",
                            com.samplocal.manager.net.IceCandidate.priority(
                                com.samplocal.manager.net.CandidateType.HOST
                            )
                        )
                    )
                }
                if (st.publicIp != null && st.publicPort != null) {
                    add(
                        com.samplocal.manager.net.IceCandidate.srflx(
                            java.net.InetAddress.getByName(st.publicIp),
                            st.publicPort, st.localIp ?: ""
                        )
                    )
                }
            }
            val offer = repo.signaling.buildOffer(cands, port)
            val cm = ctx.getSystemService(ClipboardManager::class.java)
            cm?.setPrimaryClip(ClipData.newPlainText("oferta", offer))
            _message.value = uiStrings().mOfferCopied.format(repo.signaling.sessionId.take(8))
        } catch (e: Exception) {
            _message.value = uiStrings().mExportFail.format(e.message)
        }
    }

    fun importAnswer(text: String) {
        val r = repo.signaling.parseAnswer(text)
        _message.value = if (r.isSuccess) {
            uiStrings().mCandidates.format(r.getOrNull()?.size ?: 0)
        } else {
            uiStrings().mBadAnswer.format(r.exceptionOrNull()?.message)
        }
    }

}
