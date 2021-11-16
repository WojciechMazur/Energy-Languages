/* The Computer Language Benchmarks Game
http://benchmarksgame.alioth.debian.org/

regex-dna program contributed by Isaac Gouy
modified and updated for 2.8 by Rex Kerr
converted from regex-dna program
 */

import java.io._
import scala.io.Source
import scala.concurrent.Future

object regexredux {
  val sequenceMatchPatterns = Seq(
    "agggtaaa|tttaccct",
    "[cgt]gggtaaa|tttaccc[acg]",
    "a[act]ggtaaa|tttacc[agt]t",
    "ag[act]gtaaa|tttac[agt]ct",
    "agg[act]taaa|ttta[agt]cct",
    "aggg[acg]aaa|ttt[cgt]ccct",
    "agggt[cgt]aa|tt[acg]accct",
    "agggta[cgt]a|t[acg]taccct",
    "agggtaa[cgt]|[acg]ttaccct"
  )

  val sequenceSubstitutions = Seq(
    "tHa[Nt]" -> "<4>",
    "aND|caN|Ha[DS]|WaS" -> "<3>",
    "a[NSt]|BY" -> "<2>",
    "<[^>]*>" -> "|",
    "\\|[^|][^|]*\\|" -> "-"
  )

  def main(args: Array[String]) = {
    run(System.in)
  }

  def run(inputStream: java.io.InputStream): Unit = {
    var sequence = io.Source.fromInputStream(inputStream).mkString
    val initialLength = sequence.length

    def matching(s: String) =
      java.util.regex.Pattern.compile(s).matcher(sequence)

    // remove FASTA sequence descriptions and new-lines
    sequence = matching(">.*\n|\n").replaceAll("")
    val codeLength = sequence.length

    sequenceMatchPatterns.foreach { pattern =>
      var count = 0
      val m = matching(pattern)
      while (m.find()) count += 1
      println(pattern + " " + count)
    }
    sequenceSubstitutions.foreach { case (pattern, replacement) =>
      sequence = matching(pattern).replaceAll(replacement)
    }

    println(s"""
    |$initialLength
    |$codeLength
    |${sequence.length()}""".stripMargin)
  }

}
