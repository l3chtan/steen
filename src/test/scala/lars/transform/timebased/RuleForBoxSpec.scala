package lars.transform.timebased

import core.StringValue
import core.lars.{Box, SlidingTimeWindow, WindowAtom}
import lars.transform.TransformLarsSpec
import org.scalatest.Matchers._


/**
  * Created by FM on 05.05.16.
  */
class RuleForBoxSpec extends TransformLarsSpec {

  def rulesForBox(windowAtom: WindowAtom) = allWindowRules(DefaultLarsToPinnedProgram.slidingTime(windowAtom.windowFunction.asInstanceOf[SlidingTimeWindow], windowAtom))

  val w_te_1_b_a = WindowAtom(SlidingTimeWindow(1), Box, a)

  "The rule for w^1 b a" should "contain now(T)" in {
    (rulesForBox(w_te_1_b_a) flatMap (_.body)) should contain(now(T))
  }
  it should "have head w_te_1_b_a(T)" in {
    rulesForBox(w_te_1_b_a).head.head.toString should include("w_te_1_b_a")
  }
  it should "contain a(T)" in {
    (rulesForBox(w_te_1_b_a) flatMap (_.body)) should contain(a(T))
  }
  it should "contain a(T - 1)" in {
    (rulesForBox(w_te_1_b_a) flatMap (_.body)) should contain(a(T - 1))
  }

  "The rule for w^3 b a" should "contain a(T) a(T -1), a(T -2), a(T -3)" in {
    (rulesForBox(WindowAtom(SlidingTimeWindow(3), Box, a)) flatMap (_.body)) should contain allOf(a(T), a(T - 1), a(T - 2), a(T - 3))
  }

  val w_te_1_b_a_1 = WindowAtom(SlidingTimeWindow(1), Box, a(StringValue("1")))
  "The rule for w^1 b a(1)" should "have head w_te_1_b_a(1, T)" in {
    val head = rulesForBox(w_te_1_b_a_1).head.head
    head.toString should include("w_te_1_b_a")
    head.arguments should contain inOrder(StringValue("1"), T)
  }
}
