/* The Computer Language Benchmarks Game
   http://benchmarksgame.alioth.debian.org/
   contributed by Isaac Gouy
   modified by Meiko Rachimow
   updated for 2.8 by Rex Kerr
 */

import math._

object nbody {
  def main(args: Array[String]) = {
    val iterations = args(0).toInt
    run(iterations)
  }
  
  def run(iterations: Int): Unit = {
    val system = new JovianSystem()
    def showSystemEnergy() =
      println(f"${system.energy}%.9f")
    showSystemEnergy()
    for (i <- 0 until iterations) system.advance(0.01)
    showSystemEnergy()
  }
}

class JovianSystem() extends NBodySystem {
  import NBodySystem._
  protected val bodies = {
    val SOLAR_MASS = 4 * Pi * Pi
    val DAYS_PER_YEAR = 365.24

    val sun = new Body(
      mass = SOLAR_MASS
    )

    val jupiter = new Body(
      x = 4.84143144246472090e+00,
      y = -1.16032004402742839e+00,
      z = -1.03622044471123109e-01,
      vx = 1.66007664274403694e-03 * DAYS_PER_YEAR,
      vy = 7.69901118419740425e-03 * DAYS_PER_YEAR,
      vz = -6.90460016972063023e-05 * DAYS_PER_YEAR,
      mass = 9.54791938424326609e-04 * SOLAR_MASS
    )

    val saturn = new Body(
      x = 8.34336671824457987e+00,
      y = 4.12479856412430479e+00,
      z = -4.03523417114321381e-01,
      vx = -2.76742510726862411e-03 * DAYS_PER_YEAR,
      vy = 4.99852801234917238e-03 * DAYS_PER_YEAR,
      vz = 2.30417297573763929e-05 * DAYS_PER_YEAR,
      mass = 2.85885980666130812e-04 * SOLAR_MASS
    )

    val uranus = new Body(
      x = 1.28943695621391310e+01,
      y = -1.51111514016986312e+01,
      z = -2.23307578892655734e-01,
      vx = 2.96460137564761618e-03 * DAYS_PER_YEAR,
      vy = 2.37847173959480950e-03 * DAYS_PER_YEAR,
      vz = -2.96589568540237556e-05 * DAYS_PER_YEAR,
      mass = 4.36624404335156298e-05 * SOLAR_MASS
    )

    val neptune = new Body(
      x = 1.53796971148509165e+01,
      y = -2.59193146099879641e+01,
      z = 1.79258772950371181e-01,
      vx = 2.68067772490389322e-03 * DAYS_PER_YEAR,
      vy = 1.62824170038242295e-03 * DAYS_PER_YEAR,
      vz = -9.51592254519715870e-05 * DAYS_PER_YEAR,
      mass = 5.15138902046611451e-05 * SOLAR_MASS
    )

    val allBodies = List(sun, jupiter, saturn, uranus, neptune)

    for {
      body <- allBodies
    } {
      sun.vx -= body.vx * body.mass / SOLAR_MASS
      sun.vy -= body.vy * body.mass / SOLAR_MASS
      sun.vz -= body.vz * body.mass / SOLAR_MASS
    }

    allBodies
  }
}

abstract class NBodySystem {
  import NBodySystem._
  protected def bodies: Seq[Body]
  private lazy val interactingBodies =
    bodies.toSet
      .subsets(2)
      .map { set =>
        val l :: r :: Nil = set.toList
        l -> r
      }
      .toList

  def energy: Double = {
    def energyBetween(left: Body, right: Body): Double = {
      val dx = left.x - right.x
      val dy = left.y - right.y
      val dz = left.z - right.z
      val distance = sqrt(dx * dx + dy * dy + dz * dz)
      left.mass * right.mass / distance
    }
    
    val bodiesSelfEnergy = bodies.foldLeft(0.0)(_ + _.selfEnergy)
    interactingBodies
      .foldLeft(bodiesSelfEnergy)(_ - energyBetween.tupled(_))
  }

  def advance(dt: Double) = {
    for {
      (left, right) <- interactingBodies
    } {
      val dx = left.x - right.x
      val dy = left.y - right.y
      val dz = left.z - right.z

      val distance = sqrt(dx * dx + dy * dy + dz * dz)
      val mag = dt / (distance * distance * distance)

      left.advance(dx, dy, dz, -right.mass * mag)
      right.advance(dx, dy, dz, left.mass * mag)
    }
    bodies.foreach(_.move(dt))
  }
}

object NBodySystem {
  class Body(
      var x: Double = 0.0,
      var y: Double = 0.0,
      var z: Double = 0.0,
      var vx: Double = 0.0,
      var vy: Double = 0.0,
      var vz: Double = 0.0,
      var mass: Double = 0.0
  ) {
    def selfEnergy: Double = 0.5 * mass * speedSq
    def move(dt: Double) = {
      x += dt * vx
      y += dt * vy
      z += dt * vz
    }
    def advance(dx: Double, dy: Double, dz: Double, delta: Double) = {
      vx += dx * delta
      vy += dy * delta
      vz += dz * delta
    }
    private def speedSq = vx * vx + vy * vy + vz * vz
  }

}
