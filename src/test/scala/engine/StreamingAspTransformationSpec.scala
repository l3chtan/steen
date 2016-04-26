package engine

import asp.AspExpression
import core.Atom
import engine.implementations.StreamingAspTransformation
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

/**
  * Created by FM on 22.04.16.
  */
class StreamingAspTransformationSpec extends FlatSpec {
  val t1 = At.second(1)
  val t2 = At.second(2)
  val t3 = At.second(3)

  val a = Atom("a")

  "No atom and only time t1" should "be translated into the fact 'now(1000)'" in {
    assert(StreamingAspTransformation.transform(t1, Set()) == Set(AspExpression("now(1000).")))
  }

  it should "be empty with transformAtoms" in {
    assert(StreamingAspTransformation.transformAtoms(t1, Set()) == Set())
  }
  it should "be empty with evaluation" in {
    assert(StreamingAspTransformation.transform(Set()) == Set())
  }

  "One atom and time t2" should "be translated into the facts 'now(2000). a(2000). a.'" in {
    assertResult(Set(AspExpression("now(2000)."), AspExpression("a(2000)."), AspExpression("a."))) {
      StreamingAspTransformation.transform(t2, Set(Evaluation(t2, Set(a))))
    }
  }
  it should "be translated into the facts 'a(2000).'" in {
    assertResult(Set(AspExpression("a(2000)."))) {
      StreamingAspTransformation.transformAtoms(t2, Set(a))
    }
  }
  it should "be translated into the facts 'a(2000)." in {
    assertResult(Set(AspExpression("a(2000)."))) {
      StreamingAspTransformation.transform(Set(Evaluation(t2, Set(a))))
    }
  }


  "Two atoms with arity and time t3" should "be translated into the facts 'now(3000). a(3000). b(1,3000). b." in {
    val b = Atom("b").apply("1")

    assertResult(Set(AspExpression("now(3000)."), AspExpression("a(3000)."), AspExpression("a."), AspExpression("b(1,3000)."), AspExpression("b(1)."))) {
      StreamingAspTransformation.transform(t3, Set(Evaluation(t3, Set(a, b))))
    }
  }

  "An empty set of ASP-Expressions" should "return a result with only time t1" in {
    val convert = StreamingAspTransformation(Set())
    pendingUntilFixed {
      //TODO: discuss what's correct
      assert(convert.prepare(t1, Set()).get contains Set(StreamingAspTransformation.now(t1.timePoint.toString)))
    }
  }

  "A fact in an ASP-Program" should "still be part of the result and remain unchanged" in {
    val convert = StreamingAspTransformation(Set(AspExpression("a.")))
    val result = convert.prepare(t1, Set()).get
    //TODO: discuss what's correct
    result.get should contain only (a)
  }

  "Facts from the program and the parameters" should "be part of the result" in {
    val convert = StreamingAspTransformation(Set(AspExpression("b.")))
    val result = convert.prepare(t1, Set(Evaluation(t1, Set(a)))).get
    //TODO: discuss what's correct
    result.get should contain allOf(a, a(t1.timePoint.toString), Atom("b"))
  }
}
