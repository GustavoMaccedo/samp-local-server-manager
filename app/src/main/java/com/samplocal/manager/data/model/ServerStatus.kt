package com.samplocal.manager.data.model

enum class ServerStatus {
    STOPPED,
    INSTALLING,
    STARTING,
    RUNNING,
    STOPPING,
    CRASHED,
    ERROR;

    val isActive: Boolean get() = this == STARTING || this == RUNNING
}
