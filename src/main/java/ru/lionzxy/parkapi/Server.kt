package ru.lionzxy.parkapi

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import ru.lionzxy.parkapi.helpers.ChartCreator
import ru.lionzxy.parkapi.parser.ParkMailParser
import java.util.concurrent.TimeUnit

val PERIOD_UPDATE_MILLIS = TimeUnit.MINUTES.toMillis(5)

fun main() {
    val parkMailParser = ParkMailParser()
    val chartCreator = ChartCreator()
    embeddedServer(Netty, 8200) {
        install(DefaultHeaders)
        install(ContentNegotiation) {
            gson {}
        }
        routing {
            get("image") {
                val imageFile = chartCreator.getImageFile(parkMailParser)!!
                call.respondFile(imageFile)
            }
            get("plots") {
                call.respond(parkMailParser.getCurrentState()!!)
            }
            get() {
                call.respond(parkMailParser.getCurrentState()!!.count)
            }
        }
    }.start(true)
}