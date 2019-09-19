package ru.lionzxy.parkapi.helpers

import org.beryx.awt.color.ColorFactory
import org.jsoup.Jsoup
import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.XYChart
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.style.Styler
import org.knowm.xchart.style.markers.SeriesMarkers
import ru.lionzxy.parkapi.PERIOD_UPDATE_MILLIS
import ru.lionzxy.parkapi.model.PlotDataModel
import ru.lionzxy.parkapi.parser.ParkMailParser
import java.io.File
import java.lang.RuntimeException
import java.util.*
import java.util.concurrent.TimeUnit

private val SAVE_FILE = File("tmp/park_mail.png")

class ChartCreator {
    private var lastRequest = 0L

    fun getImageFile(parser: ParkMailParser): File? {
        synchronized(this) {
            try {
                return getImageInternal(parser)
            } catch (ex: Exception) {
                ex.printStackTrace()
                throw ex
            }
        }
    }

    private fun getImageInternal(parser: ParkMailParser): File? {
        if (SAVE_FILE.exists() && System.currentTimeMillis() - lastRequest < PERIOD_UPDATE_MILLIS) {
            return SAVE_FILE
        }

        val plots = parser.getCurrentState()?.plots ?: throw RuntimeException("Plots is null!")

        val chart = getChart(plots)

        SAVE_FILE.parentFile.mkdirs()
        SAVE_FILE.outputStream().use {
            BitmapEncoder.saveBitmap(chart, it, BitmapEncoder.BitmapFormat.PNG)
        }
        lastRequest = System.currentTimeMillis()
        return SAVE_FILE
    }

    private fun getChart(plots: List<PlotDataModel>): XYChart {
        val chart = XYChartBuilder().xAxisTitle("Время")
                .theme(Styler.ChartTheme.Matlab)
                .yAxisTitle("Свободные места").build()
        chart.styler.timezone = TimeZone.getTimeZone("Europe/Moscow")
        chart.styler.datePattern = "HH:mm"
        chart.styler.legendPosition = Styler.LegendPosition.InsideN

        plots.forEach { chart.addSeries(it) }
        return chart
    }

    private fun XYChart.addSeries(plot: PlotDataModel) {
        if  (plot.data.isEmpty()) {
            return
        }
        val xData = ArrayList<Date>()
        val yData = ArrayList<Long>()

        plot.data.forEach {
            xData.add(Date(it[0]))
            yData.add(it[1])
        }

        val series = addSeries(plot.label, xData, yData)
        series.marker = SeriesMarkers.NONE
        val color = ColorFactory.web(plot.color.replace("black", "blue"))
        series.lineColor = color
        series.fillColor = color
    }
}