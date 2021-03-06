package evaluation

import core.Atom
import core.lars.{LarsProgram, TimePoint}
import engine.config.{BuildEngine, EvaluationModifier, EvaluationTypes}
import engine.{EvaluationEngine, StreamEntry}

import scala.util.Random

object PrepareEvaluator {

  def buildEngineFromArguments(args: Seq[String], program: LarsProgram): EvaluationEngine = {

    if (args.length != 2) {
      printUsageAndExit(args, "Supply the correct arguments")
    }

    val evaluationType = EvaluationTypes withName args(0) //tms or clingo
    val evaluationStrategy = EvaluationModifier withName args(1) //greedy, learn or doyle; resp. pull or push

    val engine = BuildEngine.
      withProgram(program).
      withConfiguration(evaluationType, evaluationStrategy)

    if (engine.isDefined) {
      Console.println(f"Engine: $evaluationType $evaluationStrategy")
      return engine.get
    } else {
      printUsageAndExit(args, "wrong combination of evaluation-type/modifier specified")
      return null
    }
  }


  def printUsageAndExit(args: Seq[String], exitMessage: String) = {
    Console.err.println(exitMessage)
    Console.err.println()

    Console.out.println("You specified: " + args.mkString(" "))

    throw new RuntimeException("wrong arguments")
  }

  def fromArguments(args: Seq[String], instance: String, program: LarsProgram) = {
    Console.out.println(f"Evaluating ${instance}")
    def engineBuilder(): EvaluationEngine = PrepareEvaluator.buildEngineFromArguments(args, program)
    Evaluator(instance, engineBuilder)
  }

  def generateSignals(probabilities: Map[Atom, Double], random: Random, t0: TimePoint, t1: TimePoint) = {
    (t0.value to t1.value) map {
      t => {
        val atoms = selectAtoms(random)(probabilities)
        StreamEntry(TimePoint(t), atoms)
      }
    }
  }

  def selectAtoms(random: Random)(probabilities: Map[Atom, Double]): Set[Atom] = {
    val atoms = probabilities filter {
      case (_, probability) => random.nextDouble() <= probability
    }
    atoms keySet
  }

}

