package skeptik

import skeptik.proofs._
import skeptik.judgments._
import skeptik.expressions._

// A collection of functions to analyse proofs and differences between proofs.
object help {

  def PNCToMap(pnc: ProofNodeCollection[SequentProof]) =
    pnc.foldLeft(Map[Sequent, List[(Sequent,Sequent)]]()) { (map,p) => p match {
      case CutIC(left,right,_,_) => map + (p.conclusion -> ((left.conclusion,right.conclusion)::(map.getOrElse(p.conclusion,Nil))))
      case _ => map
    }
  }

  def diffMap[A,B,C](ma: Map[A,B], mb: Map[A,C]) = {
    val keys = (ma.keySet) union (mb.keySet)
    keys.foldLeft(Map[A,(Option[B],Option[C])]()) { (map,k) =>
      (ma contains k, mb contains k) match {
        case (true,true) if (ma(k) != mb(k)) => map + (k -> (Some(ma(k)),Some(mb(k))))
        case (true,false) => map + (k -> (Some(ma(k)),None))
        case (false,true) => map + (k -> (None,Some(mb(k))))
        case _ => map
      }
    }
  }

  def convertToSequent(clause: proofs.oldResolution.resolution.Clause) = {
    var ant: List[E] = Nil
    var suc: List[E] = Nil
    clause.foreach { l => if (l.polarity) ant = Var(l.atom.toString,o)::ant else suc = Var(l.atom.toString,o)::suc }
    Sequent(ant,suc)
  }

  def convertToSequentProof(p: proofs.oldResolution.resolution.Proof) = {
    val toSequent = scala.collection.mutable.HashMap[proofs.oldResolution.resolution.Proof,SequentProof]()
    def recursive(p: proofs.oldResolution.resolution.Proof):SequentProof = if (toSequent contains p) toSequent(p) else {
      val seq = p match {
        case proofs.oldResolution.resolution.Resolvent(left,right) => CutIC(recursive(left), recursive(right))
        case proofs.oldResolution.resolution.Input(clause) => Axiom(convertToSequent(clause))
      }
      toSequent.update(p, seq)
      seq
    }
    recursive(p)
  }

  def printDigraph[A](filename: String, in: Map[A,List[(A,A)]]) = {
    val out = new java.io.PrintStream(filename)
    var next = 0
    val map = scala.collection.mutable.HashMap[A,String]()
    def nodeString(node: A) =
      if (map contains node) map(node) else {
        val ret = "n" + next
        map.update(node, ret)
        next = next + 1
        ret
      }
    out.println("digraph proof {")
    in.keys.foreach { k =>
      in(k).foreach { v =>
        out.println("  " + nodeString(k) + " -> " + nodeString(v._1) + ";")
        out.println("  " + nodeString(k) + " -> " + nodeString(v._2) + ";")
      }
    }
    map.foreach { t => out.println("  " + t._2 + " [label=\"" + t._1 + "\"];") }
    out.println("}")
    out.close()
  }

}