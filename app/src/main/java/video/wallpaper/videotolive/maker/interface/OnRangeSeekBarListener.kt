package video.wallpaper.videotolive.maker.`interface`

import video.wallpaper.videotolive.maker.view.RangeSeekBarVIews

interface OnRangeSeekBarListener {
    fun onCreate(rangeSeekBarView: RangeSeekBarVIews?, index: Int, value: Float)
    fun onSeek(rangeSeekBarView: RangeSeekBarVIews?, index: Int, value: Float)
    fun onSeekStart(rangeSeekBarView: RangeSeekBarVIews?, index: Int, value: Float)
    fun onSeekStop(rangeSeekBarView: RangeSeekBarVIews?, index: Int, value: Float)
}