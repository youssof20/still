package app.still.launcher

sealed class LaunchResult {
    data object Success : LaunchResult()
    data class Failed(val reason: LaunchFailureReason) : LaunchResult()
}

enum class LaunchFailureReason {
    Missing,
    Disabled,
    Unavailable,
}
