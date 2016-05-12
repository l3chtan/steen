package lars.transform

import core.lars.{Diamond, Box, SlidingTimeWindow, WindowAtom}
import engine.TransformLars
import org.scalatest.Matchers._
import org.scalatest.Inspectors._

/**
  * Created by FM on 09.05.16.
  */
class RuleForDiamondSpec extends TransformLarsSpec {
  val w_1_d_a = WindowAtom(SlidingTimeWindow(1), Diamond, a)

  "The rule for w^1 d a" should "return two rules" in {
    TransformLars.ruleForDiamond(w_1_d_a) should have size (2)
  }
  it should "contain now(T) in all rules" in {
    forAll(TransformLars.ruleForDiamond(w_1_d_a)) { rule => rule.body should contain(now(T)) }
  }
  it should "have head w_1_b_a" in {
    forAll(TransformLars.ruleForDiamond(w_1_d_a)) { rule => rule.head.toString should be("w_1_d_a") }
  }
  it should "contain a(T) for one element" in {
    forExactly(1, TransformLars.ruleForDiamond(w_1_d_a)) { rule => rule.body should contain(a(T)) }
  }
  it should "contain a(T - 1)" in {
    forExactly(1, TransformLars.ruleForDiamond(w_1_d_a)) { rule => rule.body should contain(a(T + "-1")) }
  }

  "The rule for w^3 d a" should "contain a(T -1), a(T -2), a(T -3), a(T)" in {
    TransformLars.ruleForDiamond(WindowAtom(SlidingTimeWindow(3), Diamond, a)) flatMap (_.body) should contain allOf(a(T), a(T + "-1"), a(T + "-2"), a(T + "-3"))
  }
}