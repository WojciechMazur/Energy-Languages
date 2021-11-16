/*
 * The Computer Language Benchmarks Game
 * http://benchmarksgame.alioth.debian.org/
 * contributed by Rex Kerr
 * based on version by John Nilsson as modified by Geoff Reedy
 */

object pidigits {
  type I = BigInt
  import BigInt._

  class LFT(q: I, r: I, t: I) {
    def compose(k: Int): LFT =
      new LFT(
        q = q * k,
        r = (q * (4 * k + 2)) + (r * (2 * k + 1)),
        t = t * (2 * k + 1)
      )

    def next(y: Int) = new LFT(
      q = q * 10,
      r = (r - (t * y)) * 10,
      t = t
    )

    def extract: Option[Int] = {
      val (y, rem) = (q * 3 + r) /% t
      if ((rem + q) < t) Some(y.intValue) else None
    }
  }

  def pi_digits = {
    /*  uses only ONE bigint division instead of TWO*/
    def digits(z: LFT, k: Int): LazyList[Int] = z.extract match {
      case Some(y) => LazyList.cons(y, digits(z next y, k))
      case None    => digits(z compose k, k + 1)
    }

    digits(new LFT(1, 0, 1), 1)
  }

  def by[T](s: LazyList[T], n: Int): LazyList[LazyList[T]] = {
    if (s.isEmpty) LazyList.empty
    else
      s.take(n) #::
        by(s.drop(n), n)
  }

  def main(args: Array[String]): Unit = {
    val limit = args(0).toInt
    run(limit)
  }
  
  def run(limit: Int): Unit ={
    for {
      (digits, batchIdx) <- by(pi_digits.take(limit), 10).zipWithIndex
      idx = 10 * batchIdx + digits.length
    } println(f"${digits.mkString}%-10s${'\t'}:${idx}")
  }
}
