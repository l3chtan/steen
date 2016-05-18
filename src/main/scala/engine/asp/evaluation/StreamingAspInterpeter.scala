package engine.asp.evaluation

import clingo.ClingoConversion
import core.Model
import core.asp.AspProgram
import core.lars.TimePoint
import engine.asp.{AspPullEvaluationEngine, AspPushEvaluationEngine, EvaluationMode, UseFuture}
import engine.{Result, Stream}


trait StreamingAspInterpeter {
  // TODO: pass timepoint as arguments to streaming interperter? needed for TMS
  def apply(timePoint: TimePoint, pinnedAtoms: Set[PinnedAspRule]): Option[Model]
}

//{
//
//  def prepare(pinnedAtoms: Set[PinnedAspRule]):
//}

object StreamingAspInterpeter {

  def select(program: AspProgram, interpretationMode: InterpretationMode): StreamingAspInterpeter = interpretationMode match {
    case Clingo => StreamingClingoInterpreter(ClingoConversion(program))
  }
}

trait InterpretationMode

object Clingo extends InterpretationMode

object TMS extends InterpretationMode
