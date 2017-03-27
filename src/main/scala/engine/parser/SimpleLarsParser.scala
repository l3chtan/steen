package engine.parser

import core.lars._
import engine.parser.wrapper._

import scala.util.parsing.combinator.JavaTokenParsers
/**
   * Created by et on 16.03.17.
   */
class SimpleLarsParser extends JavaTokenParsers {

  /* Possibly remove comments from the input beforehand. See utils.Tokenizer for that. */

  override val skipWhitespace = false

  def program: Parser[ProgramWrapper] = rep(comment) ~> rep(importN) ~ rep(comment) ~ rule ~ rep(rule) <~ rep(comment) ^^ {
    case imp ~_ ~ r ~ lr => ProgramWrapper(imp,List(r)++lr)
  }

  def importN: Parser[ImportWrapper] = "import"~>space ~ str ~ opt("("~>optSpace ~ str ~ optSpace <~ ")") ~ space ~ "as" ~ space ~ str ~ newline <~ rep(newline) ^^ {
    case _ ~ str1 ~ None ~ _ ~ _ ~ _ ~ str2 ~ _ => {
      ImportWrapper(str1,None,str2)
    }
    case _ ~ str1 ~ params ~ _ ~ _ ~ _ ~ str2 ~ _ => {
      val parameter = params.get._2 + params.get._1._1 + params.get._1._2
      ImportWrapper(str1,Some(parameter.trim),str2)
    }
  }

  def rule: Parser[RuleWrapper] = rep(comment) ~> ruleBase ~ "." <~ rep(comment) ~> rep(newline) ^^ {case r ~ _ => r}

  def ruleBase: Parser[RuleWrapper] = (opt(head) ~ optSpace ~ ":-" ~ optSpace ~ body ^^ {case h ~ _ ~ _ ~ _ ~ b => RuleWrapper(h,Some(b))}
    | head ^^ (h => RuleWrapper(Some(h), None)))

  def head: Parser[AtomTrait] = atAtom | atom

  def body: Parser[BodyWrapper] = repsep(bodyElement,",") ^^ BodyWrapper

  def bodyElement: Parser[BodyTrait] = wAtom | head | operation

  def atom: Parser[AtomWrapper] = opt(neg) ~ optSpace ~ predicate ~ opt("(" ~> repsep(upperChar,",") <~ ")") ^^ {
    case not ~ _ ~ pred ~ params => AtomWrapper(not, pred, params.get)
  }

  def predicate: Parser[String] = lowChar ~ opt(str) ^^ { case l ~ r => ""+l+r }

  def atAtom: Parser[AtAtomWrapper] = atom ~ space ~ opt(neg) ~ "at" ~ space ~ (number|(upperChar ~ rep(str))) ^^ {
    case atom ~ _ ~ not ~ _ ~ _ ~ time => AtAtomWrapper(not,atom,time.toString)
  }

  def wAtom: Parser[WAtomWrapper] = atom ~ opt(space ~> "always" <~ optSpace) ~ opt(space ~ "in" ~ space) ~ window ^^ {
    case atom ~ None ~ _ ~ win => WAtomWrapper(atom,Some(Diamond),win)
    case atom ~ _ ~ _ ~ win => WAtomWrapper(atom,Some(Box),win)
  } | atAtom ~ opt(space ~ "in" ~ space) ~ window ^^ {
    case atAtom ~ _ ~ win => WAtomWrapper(atAtom,None,win)
  }

  //TODO can we abstract this and plug in?
  def window: Parser[WindowWrapper] = "[" ~> str ~ opt(space ~> param ~ opt("," ~> param ~ opt("," ~> param))) <~ "]" ^^ {
    case wType ~ None                                   => WindowWrapper(wType)
    case wType ~ params if params.get._2.isEmpty        => WindowWrapper(wType,Some(params.get._1))
    case wType ~ params if params.get._2.get._2.isEmpty => WindowWrapper(wType,Some(params.get._1),Some(params.get._2.get._1))
    case wType ~ params                                 => WindowWrapper(wType,Some(params.get._1),Some(params.get._2.get._1),Some(params.get._2.get._2.get))
  }

  def operand: Parser[OperandWrapper] = optSpace ~> (upperChar ^^ { o: Char => OperandWrapper(o) }
                                                    | number ^^ { o: Double => OperandWrapper(o) }) <~ optSpace

  def arithmetic: Parser[String] = "+"|"-"|"/"|"*"

  def compare: Parser[String] = "="|">="|"<="|"!="|"<"|">"

  //TODO do not allow arbitrary 'calculations', only single 'assignments' (like T = A + B, A + B = T) and (binary) relations
  def arithOperation: Parser[List[(String, OperandWrapper)]] = operand ~ rep(arithmetic ~ operand) ^^ {
    case o ~ l => List(("", o)) ++ l.flatten
  }

  def operation: Parser[OperationWrapper] = arithOperation ~ compare ~ arithOperation ^^ {
    case l1 ~ op ~ l2 => OperationWrapper(l1,op,l2)
  }

  def operator: Parser[String] = arithmetic | compare

  def param: Parser[ParamWrapper] = optSpace ~> number ~ opt(space ~> str) ^^ {
    case num ~ str => ParamWrapper(num,str)
  }

  def neg: Parser[Any] = optSpace ~ "not" ~ space

  def number: Parser[Double] = floatingPointNumber ^^ (_.toDouble)

  def digit: Parser[Int] = """[0-9]""".r ^^ (_.toInt)

  def newline: Parser[String] = "\n" | "\r"

  def str: Parser[String] = char ~ rep(char|digit) ^^ {
    case c ~ str => c.toString+str.toString()
  }

  def char: Parser[Char] = lowChar | upperChar

  def lowChar: Parser[Char] = """[a-z]""".r ^^ (_.head)

  def upperChar: Parser[Char] = """[A-Z]""".r ^^ (_.head)

  def comment: Parser[Any] = lineComment | blockComment

  def lineComment: Parser[Any] = ("//" | "%") ~ optSpace ~ repsep(str,space) ~ newline

  def blockComment: Parser[Any] = ("/*" ~ optSpace ~ repsep(str,space) ~ "*/" | "%*" ~ optSpace ~ repsep(str,space) ~ "*%") ~ rep(newline)

  def optSpace: Parser[String] = rep(" ") ^^ (_.toString)

  def space: Parser[String] = "( )+".r
}