package com.gymshark.buildlogic

// Single source of truth for SDK levels (docs/CONVENTIONS.md §2). Only
// changed here — never hardcoded again in any module's build file.
object SdkConfig {
    const val COMPILE_SDK = 36
    const val TARGET_SDK = 36
    const val MIN_SDK = 26
}
