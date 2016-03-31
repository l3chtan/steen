package jtms.asp.examples

import core.{Atom, Fact, Program, Rule}
import jtms.tmn.examples.TweetyBehavior
import jtms.{AnswerUpdateNetwork, in}
import org.scalatest.FlatSpec

/**
  * Created by FM on 12.02.16.
  */
class Deletion extends FlatSpec {

  val a = Atom("a")
  val b = Atom("b")
  val c = Atom("c")
  
  val none = Set[Atom]()

  "A model with only one rule" should "have no rules and atoms after deletion" in {
    val r0 = Rule(a,none,none)

    val net = new AnswerUpdateNetwork()
    net.add(r0)

    assume(net.getModel == Some(Set(a)))
    assume(net.status(a) == in)

    net.remove(r0)

    assert(net.atoms.isEmpty)
    assert(net.status.isEmpty)

    assert(net.rules.isEmpty)
    assert(net.getModel == None)

    assert(net.cons.isEmpty)
    assert(net.supp.isEmpty)
  }

  "Removing the rule 'a :-c' in a program ('a :- c','a :- c, b')" should "still have cons(c) = a " in {
    val r1 = Rule(a,Set(c),none)
    val r2 = Rule(a,Set(c, b))

    val program = Program(r1, r2)

    val net = AnswerUpdateNetwork(program)

    assume(net.cons(c) == Set(a))

    net.remove(r1)

    assert(net.cons(c) == Set(a))
  }

  "A stable TMN with 2 atoms and two rules" should "have an empty model after deletion of a supporting Premise" in {
    // arrange
    val r0 = Rule.pos(a).head(b)
    val r1 = Rule(a,none,none)

    val net = new AnswerUpdateNetwork()

    net.add(r0)
    net.add(r1)

    assume(net.getModel.get == Set(a, b))

    // act
    net.remove(r1)

    // assert
    assert(net.getModel.isEmpty)

    assert(net.rules == List(r0))
    assert(net.supp(a).isEmpty)
    assert(net.suppRule(a) == None)
    assert(net.suppRule(b) == None)
    assert(net.cons(a) == Set(b))
    assert(net.atoms == Set(a, b))
    assert(net.status.keys == Set(a, b))
  }

  it should "have the Model A after deletion of a rule" in {
    // arrange
    val r1 = Rule(b,a)
    val r2 = Fact(a)

    val net = new AnswerUpdateNetwork()

    net.add(r1)
    net.add(r2)

    assume(net.getModel.get == Set(a, b))

    // act
    net.remove(r1)

    // assert
    assert(net.getModel.get == Set(a))

    assert(net.rules == List(r2))
    assert(net.supp(a) == Set())
    assert(net.suppRule(a) == Some(r2))
    assert(net.cons(a) == Set())

    assert(net.atoms == Set(a))
    assert(net.status.keys == Set(a))
  }

  "A TMN with three atoms" should "have only Model A after deleting a rule" in {
    val r0 = Rule.pos(a).head(b)
    val r1 = Fact(a)
    val r2 = Rule.pos(b).head(c)

    val net = new AnswerUpdateNetwork()

    net.add(r0)
    net.add(r1)
    net.add(r2)

    assume(net.getModel.get == Set(a, b, c))

    net.remove(r0)

    assert(net.getModel.get == Set(a))

    assume(net.rules.toSet == Set(r1, r2))
    assert(net.supp(c) == Set(b))
    assert(net.cons(a) == Set())
    assert(net.suppRule(c) == None)

    assert(net.atoms== Set(a, b, c))
    assert(net.status.keys == Set(a, b, c))
  }

  "A TMN with three atoms and a redundant rule" should "have Model A,C after deleting a rule supporting B" in {
    val r0 = Rule.pos(a).head(b)
    val r1 = Fact(a)
    val r2 = Rule.pos(b).head(c)
    val r3 = Rule.pos(a).head(c)

    val net = new AnswerUpdateNetwork()

    net.add(r0)
    net.add(r1)
    net.add(r2)
    net.add(r3)

    assume(net.getModel.get == Set(a, b, c))
    assume(net.suppRule(c) == Some(r2))
    assume(net.supp(c) == Set(b))
    assume(net.cons(a) == Set(b, c))
    assume(net.cons(b) == Set(c))

    net.remove(r0)

    assert(net.getModel.get == Set(a, c))
    assert(net.rules.toSet == Set(r1, r2, r3), "the SJ for C should change")
    info("the SJ for C should change")
    assert(net.suppRule(c) == Some(r3))
    info("the supp for C should change")
    assert(net.supp(c) == Set(a))
    info("the cons for A should change")
    assert(net.cons(a) == Set(c))
    assert(net.cons(b) == Set(c))
  }

  "Removing an additional rule form the JTMS 5 sample" should "result in the original model" in {
    // arrange
    val setup = new JTMS_5_ASP
    val net = setup.net

    assume(net.getModel.get == Set(setup.a, setup.c, setup.d, setup.e, setup.f))

    // act
    net.remove(setup.j0)

    // assert
    assert(net.getModel.get == Set(setup.e, setup.b, setup.d))
  }

  "Removing the Penguin premise from the Tweety sample" should "result in the Model V, F" in {
    // arrange
    class Tweety extends FlatSpec with TweetyBehavior

    val setup = new Tweety

    val net = AnswerUpdateNetwork(setup.program)

    net.add(setup.j5)

    assume(net.getModel.get == Set(setup.F_not, setup.V, setup.P))

    // act
    net.remove(setup.j5)

    // assert
    assert(net.getModel.get == Set(setup.V, setup.F))
  }

  "Removing a rule from a TMN where backtracking occurred" should "result in the original model" in {
    // arrange
    class JTMS_21ASP extends JTMSSpecASP with JTMS_21Behavior_ASP
    val setup = new JTMS_21ASP
    val net = AnswerUpdateNetwork(setup.p)

    assume(net.getModel.get == Set(setup.a, setup.c, setup.d, setup.f, setup.e))

    // act
    net.remove(setup.j7)

    // assert
    //assert(net.getModel == setup.net.model) TODO??
    assert(net.getModel.get == Set(setup.e, setup.b, setup.d))
  }

  "Removing a exclusion rule for A in the library sample" should "result in the initial model" in {
    val setup = new LibraryAtomValidationASP
    val net = setup.Network

    net.add(setup.r10)

    assume(net.getModel.get == Set(setup.not_a, setup.h, setup.p, setup.v))

    // act
    net.remove(setup.r10)

    // assert
    assert(net.getModel.get == Set(setup.a, setup.p, setup.v))
  }

}
