package com.example.dive.report

import com.example.dive.model.DiveMode
import com.example.dive.model.DiveSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 다이브 세션들을 자체 완결형 HTML 리포트로 변환한다(폰에서 생성).
 * 프리다이빙/스쿠버 탭 + 세션별 상세(수심 프로파일 SVG).
 */
object HtmlReport {

    fun build(sessions: List<DiveSession>, nowMs: Long): String {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html lang=\"ko\"><head>")
        sb.append("<meta charset=\"utf-8\">")
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
        sb.append("<title>다이브 로그 리포트</title>")
        sb.append("<style>").append(css()).append("</style>")
        sb.append("</head><body>")

        sb.append("<h1>🤿 다이브 로그 리포트</h1>")
        sb.append("<p class=\"gen\">생성: ").append(dateTime(nowMs)).append("</p>")

        if (sessions.isEmpty()) {
            sb.append("<p class=\"muted\">저장된 다이브 세션이 없습니다.</p>")
        } else {
            sb.append("<div class=\"tabs\">")
            sb.append("<button id=\"btn-free\" class=\"tab active\" onclick=\"showTab('free')\">프리다이빙</button>")
            sb.append("<button id=\"btn-scuba\" class=\"tab\" onclick=\"showTab('scuba')\">스쿠버</button>")
            sb.append("</div>")
            appendModeTab(sb, "free", sessions, DiveMode.FREEDIVING, visible = true)
            appendModeTab(sb, "scuba", sessions, DiveMode.SCUBA, visible = false)
            sb.append(tabScript())
        }

        sb.append("</body></html>")
        return sb.toString()
    }

    private fun appendModeTab(
        sb: StringBuilder,
        paneId: String,
        allSessions: List<DiveSession>,
        mode: DiveMode,
        visible: Boolean
    ) {
        sb.append("<div id=\"tab-").append(paneId).append("\" class=\"tabpane\"")
        if (!visible) sb.append(" style=\"display:none\"")
        sb.append(">")

        val indexed = allSessions.withIndex().filter { it.value.mode == mode }
        if (indexed.isEmpty()) {
            sb.append("<p class=\"muted\">기록이 없습니다.</p></div>")
            return
        }

        val sessions = indexed.map { it.value }
        val totalDives = sessions.sumOf { it.diveCount }
        val deepest = sessions.maxOfOrNull { it.maxDepth } ?: 0f
        val totalTime = sessions.sumOf { it.totalDiveSec }

        sb.append("<div class=\"cards\">")
        card(sb, "${sessions.size}", "세션")
        card(sb, "$totalDives", "총 다이브")
        card(sb, "${f1(deepest)} m", "최고 수심")
        card(sb, fmtClock(totalTime), "총 시간")
        sb.append("</div>")

        sb.append("<table><tr><th>날짜</th><th>최대</th><th>시간/회수</th></tr>")
        indexed.forEach { (i, s) ->
            val tail = if (s.mode == DiveMode.FREEDIVING) "${s.diveCount}회" else fmtClock(s.totalDiveSec)
            sb.append("<tr><td><a href=\"#s$i\">").append(dateShort(s.startTime)).append("</a></td>")
            sb.append("<td>").append(f1(s.maxDepth)).append(" m</td>")
            sb.append("<td>").append(tail).append("</td></tr>")
        }
        sb.append("</table>")

        sb.append("<h2>세션 상세</h2>")
        indexed.forEach { (i, s) -> appendSession(sb, s, i) }

        sb.append("</div>")
    }

    private fun tabScript(): String =
        "<script>function showTab(id){" +
            "var p=document.getElementsByClassName('tabpane');for(var i=0;i<p.length;i++)p[i].style.display='none';" +
            "var t=document.getElementsByClassName('tab');for(var i=0;i<t.length;i++)t[i].classList.remove('active');" +
            "document.getElementById('tab-'+id).style.display='block';" +
            "document.getElementById('btn-'+id).classList.add('active');}</script>"

    private fun appendSession(sb: StringBuilder, s: DiveSession, index: Int) {
        sb.append("<div class=\"session\" id=\"s$index\">")
        sb.append("<h3>").append(dateTime(s.startTime))
            .append("<span class=\"tag\">").append(modeLabel(s.mode)).append("</span></h3>")

        val placeLabel = s.placeName
        val lat = s.latitude
        val lng = s.longitude
        if (placeLabel != null || (lat != null && lng != null)) {
            val label = placeLabel ?: String.format(Locale.US, "%.5f, %.5f", lat, lng)
            sb.append("<p class=\"muted\">📍 ")
            if (lat != null && lng != null) {
                sb.append("<a href=\"https://www.google.com/maps?q=").append(lat).append(",").append(lng)
                    .append("\">").append(escapeHtml(label)).append("</a>")
            } else {
                sb.append(escapeHtml(label))
            }
            sb.append("</p>")
        }

        sb.append("<div class=\"cards\">")
        card(sb, "${f1(s.maxDepth)} m", "최대 수심")
        if (s.mode == DiveMode.FREEDIVING) {
            card(sb, "${s.diveCount}회", "다이브")
            card(sb, fmtClock(s.totalDiveSec), "총 시간")
        } else {
            val d = s.dives.firstOrNull()
            card(sb, "${f1(d?.avgDepth ?: 0f)} m", "평균 수심")
            card(sb, fmtClock(s.totalDiveSec), "시간")
        }
        sb.append("</div>")

        sb.append(depthChartSvg(s))

        if (s.mode == DiveMode.FREEDIVING && s.dives.isNotEmpty()) {
            sb.append("<table><tr><th>#</th><th>최대 수심</th><th>시간</th></tr>")
            s.dives.forEachIndexed { i, d ->
                sb.append("<tr><td>${i + 1}</td><td>").append(f1(d.maxDepth)).append(" m</td><td>")
                    .append(fmtClock(d.durationSec)).append("</td></tr>")
            }
            sb.append("</table>")
        }
        sb.append("</div>")
    }

    private fun depthChartSvg(session: DiveSession): String {
        val depths = ArrayList<Float>()
        session.dives.forEach { dive -> dive.samples.forEach { depths.add(it.depth) } }
        if (depths.isEmpty()) return "<p class=\"muted\">프로파일 데이터 없음</p>"

        val maxD = (depths.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val w = 320f
        val h = 140f
        val pad = 18f
        val plotW = w - 2 * pad
        val plotH = h - 2 * pad
        val n = depths.size

        fun px(i: Int) = pad + if (n <= 1) 0f else plotW * i / (n - 1)
        fun py(d: Float) = pad + plotH * (d / maxD)

        val line = StringBuilder()
        val area = StringBuilder("M ${f1(px(0))},${f1(pad + plotH)} ")
        depths.forEachIndexed { i, d ->
            if (i > 0) line.append(' ')
            line.append(f1(px(i))).append(',').append(f1(py(d)))
            area.append("L ").append(f1(px(i))).append(',').append(f1(py(d))).append(' ')
        }
        area.append("L ${f1(px(n - 1))},${f1(pad + plotH)} Z")

        val svg = StringBuilder()
        svg.append("<svg viewBox=\"0 0 ${w.toInt()} ${h.toInt()}\" xmlns=\"http://www.w3.org/2000/svg\">")
        svg.append("<line x1=\"${f1(pad)}\" y1=\"${f1(pad)}\" x2=\"${f1(w - pad)}\" y2=\"${f1(pad)}\" stroke=\"#1f4356\" stroke-width=\"1\"/>")
        svg.append("<path d=\"$area\" fill=\"#4fc3f7\" fill-opacity=\"0.2\"/>")
        svg.append("<polyline points=\"$line\" fill=\"none\" stroke=\"#4fc3f7\" stroke-width=\"2\"/>")
        svg.append("<text x=\"${f1(pad)}\" y=\"${f1(pad - 6)}\" fill=\"#9bbccb\" font-size=\"9\">0m</text>")
        svg.append("<text x=\"${f1(pad)}\" y=\"${f1(h - 4)}\" fill=\"#9bbccb\" font-size=\"9\">최대 ${f1(maxD)}m</text>")
        svg.append("</svg>")
        return svg.toString()
    }

    private fun card(sb: StringBuilder, value: String, label: String) {
        sb.append("<div class=\"card\"><div class=\"v\">").append(value)
            .append("</div><div class=\"l\">").append(label).append("</div></div>")
    }

    private fun modeLabel(mode: DiveMode) = if (mode == DiveMode.FREEDIVING) "프리다이빙" else "스쿠버"

    private fun escapeHtml(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun f1(v: Float) = String.format(Locale.US, "%.1f", v)

    private fun fmtClock(totalSec: Long): String {
        val hr = totalSec / 3600
        val min = (totalSec % 3600) / 60
        val sec = totalSec % 60
        return if (hr > 0) "%d:%02d:%02d".format(hr, min, sec) else "%d:%02d".format(min, sec)
    }

    private fun dateTime(ms: Long) =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ms))

    private fun dateShort(ms: Long) =
        SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(ms))

    private fun css(): String = """
        body{font-family:-apple-system,Roboto,'Noto Sans KR',sans-serif;background:#0b1f2a;color:#e6f2f7;margin:0;padding:16px;}
        h1{font-size:20px;margin:0 0 4px;}
        h2{font-size:16px;border-bottom:1px solid #1f4356;padding-bottom:4px;margin-top:28px;}
        h3{margin:0 0 6px;font-size:15px;}
        .gen{color:#7fa8bd;font-size:12px;margin-top:0;}
        .muted{color:#9bbccb;font-size:12px;}
        .cards{display:flex;flex-wrap:wrap;gap:8px;margin:12px 0;}
        .tabs{display:flex;gap:8px;margin:14px 0;}
        .tab{flex:1;background:#12303d;color:#9bbccb;border:none;border-radius:10px;padding:12px 8px;font-size:15px;}
        .tab.active{background:#4fc3f7;color:#08161d;font-weight:500;}
        .card{background:#12303d;border-radius:10px;padding:10px 14px;min-width:84px;}
        .card .v{font-size:18px;font-weight:bold;color:#4fc3f7;}
        .card .l{font-size:11px;color:#9bbccb;}
        table{width:100%;border-collapse:collapse;font-size:13px;margin:8px 0;}
        th,td{text-align:left;padding:6px 8px;border-bottom:1px solid #1f4356;}
        th{color:#9bbccb;font-weight:600;}
        a{color:#4fc3f7;text-decoration:none;}
        .session{background:#0f2833;border-radius:12px;padding:12px;margin:12px 0;}
        .tag{display:inline-block;font-size:11px;padding:2px 8px;border-radius:8px;background:#1f4356;color:#bfe3f2;margin-left:6px;vertical-align:middle;}
        svg{width:100%;height:auto;background:#08161d;border-radius:8px;margin-top:8px;display:block;}
    """.trimIndent()
}
