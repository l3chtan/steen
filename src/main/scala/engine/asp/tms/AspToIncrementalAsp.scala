package engine.asp.tms

import core.asp.{AspProgram, _}
import core.lars._
import core._
import engine.asp.{PinnedProgramWithLars, PinnedRule, now}

/**
  * Created by FM on 08.06.16.
  *
  * Remove temporal information (the pinned part, so to speak) from intensional atoms.
  */
object PinnedAspToIncrementalAsp {

  def unpin(atom: AtomWithArgument): Atom = atom match {
    case p: PinnedAtAtom => unpin(p)
    case _ => atom
  }

  def unpin(pinned: PinnedTimeAtom): Atom = pinned.atom match {
    case p: PinnedAtom => p
    case _ => pinned.arguments match {
      case pinned.time :: Nil => pinned.time match {
        case t: TimeVariableWithOffset if t.variable == T.variable => pinned.atom
        case p: TimePoint => pinned.atom
        case _ => pinned
      }
      case _ => Atom(pinned.predicate, pinned.arguments filter (_ != pinned.time))
    }
  }

  def apply(rule: PinnedRule, atomsToUnpin: Set[ExtendedAtom]): AspRule[Atom] = {

    def unpinIfNeeded(pinned: AtomWithArgument) = atomsToUnpin.contains(pinned) match {
      case true => unpin(pinned)
      case false => pinned
    }

    AspRule(
      unpin(rule.head),
      (rule.pos filterNot (_.atom == now) map unpinIfNeeded) ++
        // @ with a concrete Timepoint (e.g. @_10 a)requires a now(t) => therefore we need to keep now when t is a concrete Timepoint
        (rule.pos collect { case p: PinnedTimeAtom if p.atom == now && p.time.isInstanceOf[TimePoint] => p }),
      rule.neg map unpinIfNeeded
    )
  }

  def apply(p: PinnedProgramWithLars): NormalProgram = {

    val windowAtoms = p.windowAtoms map (_.atom)
    val atAtoms = p.atAtoms map (_.atom)

    val timestampedAtoms = (windowAtoms ++ atAtoms) collect {
      case aa: AtomWithArgument => (aa.predicate, aa.arguments)
      case a: Atom => (a.predicate, Seq())
    }

    // get pinned window atoms (that is matching predicates and arguments, except the last argument which is the Time-Variable)
    val pinnedTimestampedAtoms = p.atoms filter (a => timestampedAtoms.contains((a.predicate, a.arguments.init)))
    val atomAtT: Set[AtomWithArgument] = pinnedTimestampedAtoms collect {
      case pinned: PinnedTimeAtom if pinned.time == core.lars.T => pinned
    }

    val atPredicates = atAtoms map (a => a.predicate)
    // for @ atoms we want to keep the timestamp
    val atomsAtTWithoutAt = atomAtT filterNot (a => atPredicates contains a.predicate)

    val atomsToKeepPinned = pinnedTimestampedAtoms diff atomsAtTWithoutAt

    // Unpin atoms which have a time-variable == T  and are not @-Atoms with time variables
    val atomsToUnpin = (p.atoms diff atomsToKeepPinned).toSet[ExtendedAtom]

    val semiPinnedRules = p.rules map (r => apply(r, atomsToUnpin))

    AspProgram(semiPinnedRules.toList)
  }
}