import sRAPL._

@main()
def run(n: Int, configArgs: String*): Unit = {
  given config: Settings = Settings.parse(configArgs)
  
  measureEnergyConsumption{ () =>
    FannkuchRedux.run(n)
  }
}