import sRAPL._

@main()
def run(depth: Int, configArgs: String*): Unit = {
  given config: Settings = Settings.parse(configArgs)
  
  measureEnergyConsumption{ () =>
    BinaryTrees.run(depth)
  }
}