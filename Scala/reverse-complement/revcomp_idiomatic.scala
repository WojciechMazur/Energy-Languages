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
    @tailrec
    def swapAll(begin: Int, end: Int): Unit = {
      def nextBegin = begin + 1
      def nextEnd = end - 1

      if (begin <= end) {
        val beginValue = buf(begin)
        def endValue = buf(end)

        if (beginValue == EOL) swapAll(nextBegin, end)
        else if (endValue == EOL) swapAll(begin, nextEnd)
        else {
          buf(begin) = table(endValue)
          buf(end) = table(beginValue)
          swapAll(nextBegin, nextEnd)
        }
      }
    }

    if (count > 0) {
      swapAll(
        begin = buf.view.take(count).indexOf(EOL),
        end = count - 1
      )
      System.out.write(buf, 0, count)
    }
  }

  @tailrec
  private def loop(inputStream: java.io.InputStream): Unit = {
    val chunkSize = inputStream.read(input)
    if (chunkSize > 0) {
      val lastIdx = input.indices
        .take(chunkSize)
        .foldLeft(0) { case (lastIdx, idx) =>
          if (input(idx) == '>') {
            if (idx > lastIdx) write(input, lastIdx, idx - lastIdx)
            resetAndPrint()
            reset()
            idx
          } else lastIdx
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
