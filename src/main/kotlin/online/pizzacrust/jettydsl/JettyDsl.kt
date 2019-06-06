package online.pizzacrust.jettydsl

import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import java.net.InetAddress
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponse.SC_OK

data class ServerResponse(val response: Int = SC_OK, val responseString: String? = null)

internal class AbstractHandlerBlock(val block: (Map<String, Array<String>>, InetAddress) ->
ServerResponse):
        AbstractHandler() {
    override fun handle(target: String?, baseRequest: Request?, request: HttpServletRequest?, response: HttpServletResponse?) {
        val responseBlock = block(request!!.parameterMap, (baseRequest!!
                .remoteInetSocketAddress.address))
        response?.status = responseBlock.response
        if (responseBlock.responseString != null) {
            response?.writer?.println(responseBlock.responseString)
        }
        baseRequest?.isHandled = true
    }
}

class ServerConstructor {

    val paths: MutableList<ContextHandler> = mutableListOf()

    fun path(path: String, block: (Map<String, Array<String>>, InetAddress) -> ServerResponse) {
        val handler = ContextHandler()
        handler.contextPath = path
        handler.handler = AbstractHandlerBlock(block)
        paths.add(handler)
    }

    fun collection(): ContextHandlerCollection {
        val collection = ContextHandlerCollection()
        collection.handlers = paths.toTypedArray()
        return collection
    }

}

fun Server.startAndMerge() {
    start()
    join()
}

fun server(port: Int, block: ServerConstructor.() -> Unit) {
    val server = Server(port)
    val constructor = ServerConstructor()
    block(constructor)
    val collection = constructor.collection()
    server.handler = collection
    server.startAndMerge()
}