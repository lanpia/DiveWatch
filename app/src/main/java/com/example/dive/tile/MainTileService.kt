package com.example.dive.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.dive.MainActivity
import com.example.dive.data.DiveLogRepository
import com.example.dive.model.DiveMode
import com.example.dive.model.DiveSession
import com.example.dive.presentation.formatClock
import com.example.dive.presentation.formatDate
import com.example.dive.presentation.modeLabel
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService

private const val RESOURCES_VERSION = "0"

/**
 * 최근 다이브 요약을 보여주는 타일. 탭하면 앱을 연다.
 */
@OptIn(ExperimentalHorologistApi::class)
class MainTileService : SuspendingTileService() {

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ) = resources(requestParams)

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ): TileBuilders.Tile {
        val last = DiveLogRepository(this).loadAll().firstOrNull()
        return tile(requestParams, this, last)
    }
}

private fun resources(
    requestParams: RequestBuilders.ResourcesRequest
): ResourceBuilders.Resources {
    return ResourceBuilders.Resources.Builder()
        .setVersion(RESOURCES_VERSION)
        .build()
}

private fun tile(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
    last: DiveSession?,
): TileBuilders.Tile {
    val singleTileTimeline = TimelineBuilders.Timeline.Builder()
        .addTimelineEntry(
            TimelineBuilders.TimelineEntry.Builder()
                .setLayout(
                    LayoutElementBuilders.Layout.Builder()
                        .setRoot(tileLayout(requestParams, context, last))
                        .build()
                )
                .build()
        )
        .build()

    return TileBuilders.Tile.Builder()
        .setResourcesVersion(RESOURCES_VERSION)
        .setTileTimeline(singleTileTimeline)
        .build()
}

private fun tileLayout(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
    last: DiveSession?,
): LayoutElementBuilders.LayoutElement {
    val line1 = if (last == null) "기록 없음" else "최대 %.1f m".format(last.maxDepth)
    val line2 = when {
        last == null -> "앱을 열어 시작"
        last.mode == DiveMode.FREEDIVING -> "${modeLabel(last.mode)} · ${last.diveCount}회"
        else -> "${modeLabel(last.mode)} · ${formatClock(last.totalDiveSec)}"
    }
    val line3 = last?.let { formatDate(it.startTime) }

    val column = LayoutElementBuilders.Column.Builder()
        .addContent(
            Text.Builder(context, "다이브")
                .setColor(argb(Colors.DEFAULT.primary))
                .setTypography(Typography.TYPOGRAPHY_TITLE3)
                .build()
        )
        .addContent(
            Text.Builder(context, line1)
                .setColor(argb(Colors.DEFAULT.onSurface))
                .setTypography(Typography.TYPOGRAPHY_BODY1)
                .build()
        )
        .addContent(
            Text.Builder(context, line2)
                .setColor(argb(Colors.DEFAULT.onSurface))
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .build()
        )
    if (line3 != null) {
        column.addContent(
            Text.Builder(context, line3)
                .setColor(argb(Colors.DEFAULT.onSurface))
                .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                .build()
        )
    }

    return PrimaryLayout.Builder(requestParams.deviceConfiguration)
        .setResponsiveContentInsetEnabled(true)
        .setContent(
            LayoutElementBuilders.Box.Builder()
                .setModifiers(launchModifiers(context))
                .addContent(column.build())
                .build()
        )
        .build()
}

/** 타일 전체를 탭하면 MainActivity 실행 */
private fun launchModifiers(context: Context): ModifiersBuilders.Modifiers =
    ModifiersBuilders.Modifiers.Builder()
        .setClickable(
            ModifiersBuilders.Clickable.Builder()
                .setId("launch")
                .setOnClick(
                    ActionBuilders.LaunchAction.Builder()
                        .setAndroidActivity(
                            ActionBuilders.AndroidActivity.Builder()
                                .setClassName(MainActivity::class.java.name)
                                .setPackageName(context.packageName)
                                .build()
                        )
                        .build()
                )
                .build()
        )
        .build()

@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
fun tilePreview(context: Context) = TilePreviewData(::resources) {
    tile(it, context, null)
}
