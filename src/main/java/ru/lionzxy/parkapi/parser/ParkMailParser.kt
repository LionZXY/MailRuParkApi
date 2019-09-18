package ru.lionzxy.parkapi.parser

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import jdk.nashorn.internal.ir.*
import jdk.nashorn.internal.parser.Parser
import jdk.nashorn.internal.runtime.Context
import jdk.nashorn.internal.runtime.ErrorManager
import jdk.nashorn.internal.runtime.Source
import jdk.nashorn.internal.runtime.options.Options
import ru.lionzxy.parkapi.model.PlotDataModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import ru.lionzxy.parkapi.PERIOD_UPDATE_MILLIS
import ru.lionzxy.parkapi.model.ParkModel
import java.lang.Exception
import javax.script.ScriptEngineManager
import javax.script.Invocable


private const val PARK_URL = "http://p.corp.mail.ru/"
private val gson = Gson().newBuilder().create()
private val typeToken = object : TypeToken<List<PlotDataModel>>() {}.type

class ParkMailParser {
    private var lastRequest = 0L
    private var lastRequst: ParkModel? = null

    public fun getCurrentState(): ParkModel? {
        synchronized(this) {
            try {
                return getCurrentStateInternal()
            } catch (ex: Exception) {
                ex.printStackTrace()
                throw ex
            }
        }
    }

    private fun getCurrentStateInternal(): ParkModel? {
        if (lastRequst != null && System.currentTimeMillis() - lastRequest < PERIOD_UPDATE_MILLIS) {
            return lastRequst
        }

        val document = Jsoup.connect(PARK_URL).get()
        val availableCount = document.body().getElementById("refresh-link").text().toInt()
        val scriptBlocks = document.head().getElementsByTag("script")
        var json: String? = null
        for (element in scriptBlocks) {
            json = getJsonInternal(element)
            if (json != null) {
                break;
            }
        }

        val plots = gson.fromJson<List<PlotDataModel>>(json ?: return null, typeToken)
        lastRequest = System.currentTimeMillis()
        return ParkModel(availableCount, plots)
    }

    private fun getJsonInternal(element: Element): String? {
        val jsText = element.data()
        if (jsText.isNullOrEmpty()) {
            return null
        }
        val options = Options("nashorn")
        options.set("anon.functions", true)
        options.set("parse.only", true)
        options.set("scripting", true)

        val errors = ErrorManager()
        val context = Context(options, errors, Thread.currentThread().contextClassLoader)
        val source = Source.sourceFor("jsPark", jsText)
        val parser = Parser(context.env, source, errors)
        val node = parser.parse()
        val statements = node.body.statements
        val findStatement = (statements.firstOrNull() ?: return null) as? ExpressionStatement ?: return null
        val firstExpression = findStatement.expression as? CallNode ?: return null
        val functionNode = firstExpression.args.firstOrNull() as? FunctionNode ?: return null
        val plotNode = functionNode.body.statements.firstOrNull() as? VarNode ?: return null
        val callPlotNode = plotNode.init as? CallNode ?: return null
        if (callPlotNode.args.size < 2) {
            return null
        }
        val plotArray = callPlotNode.args[1] as? LiteralNode.ArrayLiteralNode ?: return null
        val plotJson = plotArray.toString().replace("{U%}", "")
        return jsObjectToJson(plotJson)
    }

    private fun jsObjectToJson(jsModel: String): String {
        val manager = ScriptEngineManager()
        val engine = manager.getEngineByName("JavaScript")
        engine.eval("function printJson() { var data = $jsModel; return JSON.stringify(data);}")
        val inv = engine as Invocable
        return inv.invokeFunction("printJson") as String
    }
}
