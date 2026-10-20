package com.retrohall.core

enum class CoreSource(val label: String, val description: String) {
    GITHUB("GitHub · LemuroidCores", "与 LibretroDroid 配套测试过的稳定版本（推荐）"),
    BUILDBOT("libretro 官方构建", "buildbot.libretro.com 每日构建，版本最新"),
}

/** Download locations for prebuilt Android libretro cores. */
object CoreDownloads {
    /** Tag of github.com/Swordfish90/LemuroidCores whose binaries match LibretroDroid 0.14. */
    const val LEMUROID_CORES_TAG = "1.17.0"

    /** ABIs both download sources publish, in order of preference. */
    val SUPPORTED_ABIS = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

    fun preferredAbi(deviceAbis: List<String>): String? = deviceAbis.firstOrNull { it in SUPPORTED_ABIS }

    /**
     * @param mirrorPrefix optional GitHub proxy put in front of the URL (for example `https://ghfast.top/`),
     *   useful where raw.githubusercontent.com is slow.
     */
    fun githubUrl(core: EmulatorCore, abi: String, mirrorPrefix: String = ""): String =
        normalizeMirror(mirrorPrefix) +
            "https://raw.githubusercontent.com/Swordfish90/LemuroidCores/$LEMUROID_CORES_TAG/" +
            "lemuroid_core_${core.coreName}/src/main/jniLibs/$abi/${core.libraryFileName}"

    /** The buildbot serves zips containing `<core>_libretro_android.so` (without the `lib` prefix). */
    fun buildbotUrl(core: EmulatorCore, abi: String): String =
        "https://buildbot.libretro.com/nightly/android/latest/$abi/${core.coreName}_libretro_android.so.zip"

    fun buildbotEntryName(core: EmulatorCore): String = "${core.coreName}_libretro_android.so"

    fun normalizeMirror(prefix: String): String {
        val trimmed = prefix.trim()
        return when {
            trimmed.isEmpty() -> ""
            trimmed.endsWith("/") -> trimmed
            else -> "$trimmed/"
        }
    }
}
