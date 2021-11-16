import scala.annotation.tailrec
import java.io._
/*
 * The Computer Language Benchmarks Game
 * http://benchmarksgame.alioth.debian.org/
 * contributed by Rex Kerr
 * algorithm follows Java version #4 by Anthony Donnefort
 * removed deprecated api by Benedikt Nordhoff
 */

object revcomp extends java.io.ByteArrayOutputStream {
  final val EOL = '\n'
  private val input = new Array[Byte](8192)
  private val table = Array.iterate(0.toByte, 128)(v => (v + 1).toByte)
  private val output = new BufferedOutputStream(
    new FileOutputStream(java.io.FileDescriptor.out),
    input.size
  )
  private def resetAndPrint() = {
    if (count > 0) {
      var begin = 0
      var end = count - 1
      while (buf(begin) != EOL && begin < count) { begin += 1 }
      while (begin <= end) {
        if (buf(begin) == EOL) begin += 1
        if (buf(end) == EOL) end -= 1
        if (begin <= end) {
          val temp = buf(begin)
          buf(begin) = table(buf(end))
          buf(end) = table(temp)
          begin += 1
          end -= 1
        }
      }
      System.out.write(buf, 0, count)
    }
  }

  @tailrec
  private def loop(inputStream: java.io.InputStream): Unit = {
    val chunkSize = inputStream.read(input)
    if (chunkSize > 0) {
      var idx, lastIdx = 0
      while (idx < chunkSize) {
        if (input(idx) == '>') {
          if (idx > lastIdx) write(input, lastIdx, idx - lastIdx)
          resetAndPrint()
          reset()
          lastIdx = idx
        }
        idx += 1
      }

      if (lastIdx < chunkSize) write(input, lastIdx, chunkSize - lastIdx)
      loop(inputStream)
    }
  }

  def main(args: Array[String]) = {
    run(System.in)
  }
  
  def run(inputStream: java.io.InputStream): Unit = {
    for ((i, o) <- "ACGTUMRWSYKVHDBN" zip "TGCAAKYWSRMBDHVN") {
      table(i) = o.toByte
      table(i.toLower) = o.toByte
    }

    loop(inputStream)
    resetAndPrint()
  }
}
