import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.readUTF8Line
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class UploadPerformanceTest {

    private fun runTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application {
            routing {
                put("upload") {
                    val channel = call.receiveChannel()
                    var count = 0
                    while (channel.readUTF8Line() != null) {
                        count++
                    }
                    call.respond("$count lines received")
                }
            }
        }
        block()
    }

    @Test
    fun uploadTest() = runTest {
        val time = measureTime {
            val numberOfLines = 200_000
            val response = client.put("http://localhost/upload") {
                contentType(ContentType.Text.Plain)
                setBody((1..numberOfLines).joinToString("\n") { "line $it" })
            }.bodyAsText()
            assertEquals("$numberOfLines lines received", response)
        }

        // in ktor 2.3.12 this takes ~0.3863 seconds
        // in ktor 3.0.2 this takes ~36.68 seconds
        println(time)
        assertTrue(time < 1.seconds, "Request took too long: $time")
    }
}