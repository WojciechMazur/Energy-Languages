import sRAPL._
import java.io._

@main()
def run(inputFile: String, configArgs: String*): Unit = {
  given config: Settings = Settings.parse(configArgs)
  
  measureEnergyConsumption{ () =>
    revcomp.run(new FileInputStream(inputFile))
  }
}