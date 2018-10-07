package amisync.lambda

import java.io._
import java.nio.charset.StandardCharsets.UTF_8
import java.util.stream.Collectors

import com.amazonaws.services.lambda.runtime._
import spray.json._

abstract class RequestJsonHandler[A: JsonFormat, B: JsonFormat] extends RequestStreamHandler with RequestHandler[A, B] {
  override final def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit = {
    val reader = new BufferedReader(new InputStreamReader(input, UTF_8))
    val aStr = try reader.lines().collect(Collectors.joining()) finally reader.close()
    val a = try JsonParser(aStr).convertTo[A] catch {
      case e: Exception =>
        throw new IllegalArgumentException(s"Could not parse JSON: $aStr", e)
    }
    val b = handleRequest(a, context)
    val bStr = b.toJson.compactPrint
    val writer = new BufferedWriter(new OutputStreamWriter(output, UTF_8))
    try writer.write(bStr) finally writer.close()
  }
}
