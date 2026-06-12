package com.initcn.powertools.feature.callblocker.domain

import com.initcn.powertools.feature.callblocker.data.CallRuleEntity

// Define the possible outcomes
sealed interface CallResult {
    data class Allow(val reason: String) : CallResult
    data class Block(val reason: String) : CallResult
}

class CallEvaluator {

    // Centralized normalization: strip everything except digits
    private fun normalize(input: String?): String {
        return input?.filter { it.isDigit() } ?: ""
    }

    // Evaluates an incoming call against all user rules.
    fun evaluateCall(
        incomingNumber: String?,
        isSavedContact: Boolean,
        isHiddenNumber: Boolean,
        whitelist: List<CallRuleEntity>,
        exactBlocklist: List<CallRuleEntity>,
        regexBlocklist: List<CallRuleEntity>,
        blockHiddenPref: Boolean,
        blockUnsavedPref: Boolean
    ): CallResult {
        val cleanNumber = normalize(incomingNumber)

        // Priority One: Whitelist
        val isWhitelisted = whitelist.any { rule ->
            normalize(rule.pattern) == cleanNumber
        }
        if (isWhitelisted) return CallResult.Allow("Whitelisted Number")

        // Priority Two: Settings
        if (blockHiddenPref && (isHiddenNumber || incomingNumber.isNullOrBlank())) {
            return CallResult.Block("Hidden/Restricted Caller ID")
        }
        if (blockUnsavedPref && !isSavedContact) {
            return CallResult.Block("Unsaved Contact")
        }

        // Priority Three: Exact Blocklist
        val isExactBlocked = exactBlocklist.any { rule ->
            normalize(rule.pattern) == cleanNumber
        }
        if (isExactBlocked) return CallResult.Block("Exact Blocklist Match")

        // Priority Four: Regex Blocklist
        // NOTE: Regex cannot be normalized by stripping digits,
        val isRegexBlocked = regexBlocklist.any { rule ->
            try {
                Regex(rule.pattern).containsMatchIn(incomingNumber ?: "")
            } catch (e: Exception) {
                false
            }
        }
        if (isRegexBlocked) return CallResult.Block("Regex Pattern Match")

        // Default
        return CallResult.Allow("No Blocking Rules Matched")
    }
}