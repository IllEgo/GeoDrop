package com.kitheapp.util

object R6RolloutPolicy {
    const val SUPPORTED_CONTRACT_VERSION = 1

    fun isTargetBackendUsable(enabled: Boolean, minimumContractVersion: Long): Boolean =
        enabled && minimumContractVersion in 1..SUPPORTED_CONTRACT_VERSION.toLong()
}
