/* The Computer Language Benchmarks Game
   http://benchmarksgame.alioth.debian.org/
   contributed by Isaac Gouy
   parallel by the Anh Team
   Scala Futures version by Robert Wilton
 */
import scala.concurrent._
import scala.concurrent.duration._
import scala.annotation.tailrec
import ExecutionContext.Implicits.global

object spectralnorm {
  // Ordinary and transposed versions of infinite matrix
  val A = (x: Int, y: Int) => 1.0 / ((x + y) * (x + y + 1) / 2 + x + 1)
  val At = (x: Int, y: Int) => 1.0 / ((y + x) * (y + x + 1) / 2 + y + 1)

  case class Ctx(n: Int) {
    val u, v, tmp = Array.fill(n)(1.0)
    private implicit val ctx: Ctx = this
    val multiplyAv = multiply(A) _
    val multiplyAtv = multiply(At) _

// Matrix multiplication for a given idx in range: w <- M*v
    private def multiply(
        M: (Int, Int) => Double
    )(v: Array[Double], w: Array[Double])(i: Int)(implicit
        ctx: Ctx
    ) = {
      w(i) = (0 until ctx.n)
        .foldLeft(0.0) { case (acc, j) =>
          acc + M(i, j) * v(j)
        }
    }
  }

  def main(args: Array[String]): Unit = {
    val n = args(0).toInt
    run(n)
  }
  
  def run(n: Int): Unit = {
    implicit val ctx: Ctx = Ctx(n)
    printf("%.09f\n", work())
  }

  def work()(implicit ctx: Ctx) = {
    import ctx._
    def split(f: Int => Unit) = {
      val tasks =
        Future.traverse(0 until ctx.n) { idx =>
          Future(f(idx))
        }
      Await.ready(tasks, Duration.Inf)
    }

    for (_ <- 0 until 10) {
      // Multiply by matrix & transpose
      split(multiplyAv(u, tmp))
      split(multiplyAtv(tmp, v))
      split(multiplyAv(v, tmp))
      split(multiplyAtv(tmp, u))
    }

    @tailrec
    def reduce(uv: Double, vv: Double, idx: Int): Double = {
      if (idx < n) {
        val u = ctx.u(idx)
        val v = ctx.v(idx)
        reduce(
          uv = uv + u * v,
          vv = vv + v * v,
          idx + 1
        )
      } else math.sqrt(uv / vv)
    }
    reduce(0.0, 0.0, 0)
  }
}
