
// 强制覆盖 aapt2 路径为我们刚下载的 aarch64 版本
androidComponents {
    onVariants(selector().all()) { variant ->
        val aapt2Path = file("${projectDir}/build-tools/aarch64/aapt2").absolutePath
        println("Using custom aapt2: $aapt2Path")
        // 根据 AGP 版本不同，这里可能需要调整。对于 AGP 8+，通过 android.aaptOptions 并不总是生效，
        // 但最稳妥的方法是在 gradle.properties 里面全局指定
    }
}
