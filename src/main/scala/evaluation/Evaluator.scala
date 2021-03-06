package evaluation

import engine.{StreamEntry, EvaluationEngine}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by hb on 8/29/16.
  */
case class Evaluator(instance: String, engineProvider: () => EvaluationEngine) {

  def streamAsFastAsPossible(warmUps: Int = 2, repetitions: Int = 5)(inputs: Seq[StreamEntry]): TimingsConfigurationResult = {
    val appendExecutionTimes = ArrayBuffer[scala.concurrent.duration.Duration]()
    val evaluateExecutionTimes = ArrayBuffer[scala.concurrent.duration.Duration]()

    def test = {
      val engine = TimedEvaluationEngine(engineProvider(), appendExecutionTimes, evaluateExecutionTimes)

      inputs.foreach { i => {
        engine.append(i.time)(i.atoms.toSeq: _*)

        engine.evaluate(i.time)
      }}
    }

    // warm up - we need to clear execution times afterwards
    (1 to warmUps) foreach { i => {
      test
      Console.println("Warm-up " + i)
    }}

    appendExecutionTimes.clear()
    evaluateExecutionTimes.clear()

    profile.profileR(repetitions)(test)

    TimingsConfigurationResult(
      instance,
      StatisticResult.fromExecutionTimes(appendExecutionTimes),
      StatisticResult.fromExecutionTimes(evaluateExecutionTimes)
    )
  }

  def successfulModelComputations(inputs: Seq[StreamEntry]): SuccessConfigurationResult = {
    val engine = engineProvider()

    val modelDefined = inputs.zipWithIndex.map {
      case (entry, i) => {

        engine.append(entry.time)(entry.atoms.toSeq: _*)

        val model = engine.evaluate(entry.time)

        (i, model.get.isDefined)
      }
    }

    SuccessConfigurationResult(instance, modelDefined)
  }

  def models(inputs: Seq[StreamEntry]): ModelsResult = {
    val engine = engineProvider()

    val modelDefined = inputs.zipWithIndex.map {
      case (entry, i) => {

        engine.append(entry.time)(entry.atoms.toSeq: _*)

        val model = engine.evaluate(entry.time)

        (i, model.get)
      }
    }

    ModelsResult(instance, modelDefined)
  }
}
