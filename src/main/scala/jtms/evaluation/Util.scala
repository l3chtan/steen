package jtms.evaluation

import java.util.concurrent.TimeUnit

import core._
import core.asp.{AspProgram, NormalProgram, NormalRule, UserDefinedAspRule}
import core.lars._
import engine.asp.LarsToPinnedProgram
import engine.asp.tms.{Pin, PinnedAspToIncrementalAsp}

import scala.concurrent.duration.Duration
import scala.io.{BufferedSource, Source}

/**
  * Created by hb on 8/28/16.
  */
object Util {

  def inspectGrounder(grounder: Grounder): Unit = {
    val i = grounder.inspect

    println("facts: atoms:")
    println("  ground atoms:     " + i.groundFactAtoms)
    println("  non-ground preds: " + i.nonGroundFactAtomPredicates)
    println("intensional:")
    println("  ground atoms:     " + i.groundIntensionalAtoms)
    println("  non-ground preds: " + i.nonGroundIntensionalPredicatesSansFacts)
    println()
    println("non-ground fact atoms/var in rule:\n")
    printNestedMap(i.nonGroundFactAtomsPerVariableInRule)
    println()
    println("non-ground intensional atoms/var in rule:\n")
    printNestedMap(i.nonGroundIntensionalAtomsPerVariableInRule)
    println()
    println("ground fact atom values lookup:\n")
    printNestedMap(i.groundFactAtomValuesLookup)
    println()
    println("values for predicate arg:\n")
    printNestedMap(i.valuesForPredicateArg)

  }

  def printNestedMap[T1, T2, T3](map: Map[T1, Map[T2, Set[T3]]]): Unit = {
    for ((k, v) <- map) {
      println(k + ":")
      for ((k2, set) <- v) {
        print("  " + k2 + " -> {")
        if (set.nonEmpty) {
          print(set.head)
          if (set.size > 1) {
            set.tail foreach (elem => print(", " + elem))
          }
        }
        println("}")
      }
    }
  }

  //use only for asp fragment!
  def asAspProgram(larsProgram: LarsProgram): NormalProgram = {
    val aspRules: Seq[NormalRule] = larsProgram.rules map (asAspRule(_))
    AspProgram(aspRules.toList)
  }

  def asAspRule(larsRule: LarsRule): NormalRule = {
    val head = larsRule.head.atom //we are not using @ here
    val pos = larsRule.pos map (_.asInstanceOf[Atom])
    val neg = larsRule.neg map (_.asInstanceOf[Atom])
    UserDefinedAspRule(head, pos, neg)
  }

  def program(rules: LarsRule*): LarsProgram = LarsProgram(rules)

  def ground(p: LarsProgram) = Grounder(p).groundProgram


  def aspProgramAt(groundLarsProgram: LarsProgram, time: Int, tickSize: EngineTimeUnit): NormalProgram = {

    val aspProgramWithVariables = LarsToPinnedProgram(tickSize)(groundLarsProgram)

    val incrementalProgram = PinnedAspToIncrementalAsp(aspProgramWithVariables)

    val (groundRules, nonGroundRules) = incrementalProgram.rules partition (_.isGround)

    val pin = Pin(time)
    val pinnedRules: Seq[NormalRule] = nonGroundRules map pin.ground

    AspProgram((groundRules ++ pinnedRules).toList)
  }


}