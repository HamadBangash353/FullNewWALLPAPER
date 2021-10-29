package video.wallpaper.videotolive.maker.`interface`

internal interface OnProgressVideoListener {
    fun updateProgress(time: Int, max: Int, scale: Float)
}