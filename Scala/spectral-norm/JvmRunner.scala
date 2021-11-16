import sRAPL._
import java.io._

@main()
def run(n: Int, configArgs: String*): Unit = {
  given config: Settings = Settings.parse(configArgs)
  
  measureEnergyConsumption{ () =>
    spectralnorm.run(n)
  }
}