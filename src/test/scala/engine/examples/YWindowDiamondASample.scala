package engine.examples

import asp.{ClingoConversion, ClingoExpression}
import core.{Atom, AspProgram, not}
import engine.{At, Time}
import engine.implementations.{AspPullEvaluation, AspPushEvaluation, StreamingAspTransformation}
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.OptionValues._

/**
  * Created by FM on 23.04.16.
  */
class YWindowDiamondASample extends FlatSpec {
  val aspProgram =
    """y(T) :- w1d_a(T).

      w1d_a(T) :- a(U), now(T), U >= T - 1000, U <= T.

      #show a/1.
      #show y/1.
    """

  val aspExpressions = aspProgram.split('\n') toSet

  val y = Atom("y")
  val w1d_a = Atom("w1d_a")
  val a = Atom("a")
  val u = Atom("u")

  val now = StreamingAspTransformation.now

  val program = AspProgram(
    y("T") :- w1d_a("T"),
    w1d_a("T") :- a("U") and now("T") and u("U")
  )
  val t0 = Time(0)
  val t1 = Time(1)
  val t2 = Time(2)

  def evaluation = {
    val e = AspPullEvaluation(StreamingAspTransformation(aspExpressions))

    e.append(t1)(a)

    e
  }

  "Given 't1 -> a' " should "not lead to y at t0" in {
    evaluation.evaluate(t0).get shouldNot contain(y("0"))
  }

  it should "lead to y at t1" in {
    evaluation.evaluate(t1).get.value should contain(y("1"))
  }

  it should "lead to y at t2" in {
    evaluation.evaluate(t2).get.value should contain(y("2"))
  }

  it should "not contain y(1) at t2" in {
    evaluation.evaluate(t2).get.value shouldNot contain(y("1"))
  }
  it should "still not contain y(1) at t2 with push" in {
    val e = AspPushEvaluation(StreamingAspTransformation(aspExpressions))

    e.append(t1)(a)

    e.evaluate(t2).get shouldBe None
  }
}
