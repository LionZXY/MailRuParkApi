package ru.lionzxy.parkapi

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import ru.lionzxy.parkapi.helpers.ChartCreator
import ru.lionzxy.parkapi.parser.ParkMailParser
import java.util.concurrent.TimeUnit

val PERIOD_UPDATE_MILLIS = TimeUnit.MINUTES.toMillis(5)

fun main() {
    val parkMailParser = ParkMailParser()
    val chartCreator = ChartCreator()
    embeddedServer(Netty, 8000) {
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
