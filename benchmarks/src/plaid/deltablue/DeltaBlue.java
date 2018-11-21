package plaid.deltablue;

/*
 * CL_SUN_COPYRIGHT_JVM_BEGIN
 *   If you or the company you represent has a separate agreement with both
 *   CableLabs and Sun concerning the use of this code, your rights and
 *   obligations with respect to this code shall be as set forth therein. No
 *   license is granted hereunder for any other purpose.
 * CL_SUN_COPYRIGHT_JVM_END
*/

/*
 * @(#)DeltaBlue.java	1.6 06/10/10
 *
 * Copyright  1990-2006 Sun Microsystems, Inc. All Rights Reserved.  
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER  
 *   
 * This program is free software; you can redistribute it and/or  
 * modify it under the terms of the GNU General Public License version  
 * 2 only, as published by the Free Software Foundation.   
 *   
 * This program is distributed in the hope that it will be useful, but  
 * WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU  
 * General Public License version 2 for more details (a copy is  
 * included at /legal/license.txt).   
 *   
 * You should have received a copy of the GNU General Public License  
 * version 2 along with this work; if not, write to the Free Software  
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  
 * 02110-1301 USA   
 *   
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa  
 * Clara, CA 95054 or visit www.sun.com if you need additional  
 * information or have any questions. 
 *
 */
/*
  This is a Java implemention of the DeltaBlue algorithm described in:
    "The DeltaBlue Algorithm: An Incremental Constraint Hierarchy Solver"
    by Bjorn N. Freeman-Benson and John Maloney
    January 1990 Communications of the ACM,
    also available as University of Washington TR 89-08-06.
  This implementation by Mario Wolczko, Sun Microsystems, Sep 1996,
  based on the Smalltalk implementation by John Maloney.
*/

//package COM.sun.labs.kanban.DeltaBlue;

import java.util.ArrayList;

//import Benchmark;
/* 
Strengths are used to measure the relative importance of constraints.
New strengths may be inserted in the strength hierarchy without
disrupting current constraints.  Strengths cannot be created outside
this class, so pointer comparison can be used for value comparison.
*/
class Strength {
  private int strengthValue; // create here with Unique keyword
  private String name; // Create here with Unique keyword
  // Strength constants
  public final static Strength required       = new Strength(0, "required");
  public final static Strength strongPreferred= new Strength(1, "strongPreferred");
  public final static Strength preferred      = new Strength(2, "preferred");
  public final static Strength strongDefault  = new Strength(3, "strongDefault");
  public final static Strength normal	      = new Strength(4, "normal");
  public final static Strength weakDefault    = new Strength(5, "weakDefault");
  public final static Strength weakest	      = new Strength(6, "weakest");
  
// Full(this.StrengthValue),  Full(this.name) ->  Full(this.StrengthValue),  Full(this.name) , Unique(result)
  private Strength(int strengthValue, String name)
  {
    this.strengthValue = strengthValue;
    this.name = name;
    print();
  }
// Immutable(s1), Immutable(s2) -> Immutable(s2), Immutable(s2)
  public static boolean stronger(Strength s1, Strength s2)
  {
    return s1.strengthValue < s2.strengthValue;
  }
//Immutable(s1), Immutable(s2) -> Immutable(s2), Immutable(s2)
  public static boolean weaker(Strength s1, Strength s2)
  {
    return s1.strengthValue > s2.strengthValue;
  }
//Immutable(s1), Immutable(s2) -> Immutable(s2), Immutable(s2), Unique(result) 
  public static Strength weakestOf(Strength s1, Strength s2)
  {
    return weaker(s1, s2) ? s1 : s2;
  }
//Immutable(s1), Immutable(s2) -> Immutable(s2), Immutable(s2), Unique(result)
  public static Strength strongest(Strength s1, Strength s2)
  {
    return stronger(s1, s2) ? s1 : s2;
  }
 // Immutable(strengthValue), Immutable(weakest), Immutable(weakDefault), Immutable(normal), Immutable(strongDefault), Immutable(preferred), Immutable(strongPreferred)->  Immutable(strengthValue), Immutable(weakest), Immutable(weakDefault), Immutable(normal), Immutable(strongDefault), Immutable(preferred), Immutable(strongPreferred), Unique(result)
  // for iteration
  public Strength nextWeaker()
  {
    switch (strengthValue) {
    case 0: return weakest;
    case 1: return weakDefault;
    case 2: return normal;
    case 3: return strongDefault;
    case 4: return preferred;
    case 5: return strongPreferred;

    case 6: 
    default:
      System.err.println("Invalid call to nextStrength()!");
      System.exit(1);
      return null;
    }
  }
 //Immutable(strengthValue) -> Immutable(strengthValue)
  public void print()
  {
    System.out.println("strength[" + Integer.toString(strengthValue) + "]");
  }
}
//------------------------------ variables ------------------------------

// I represent a constrained variable. In addition to my value, I
// maintain the structure of the constraint graph, the current
// dataflow graph, and various parameters of interest to the DeltaBlue
// incremental constraint solver.

class Variable {

  public int value;               // my value; changed by constraints
  public ArrayList<Constraint> constraints;      // normal constraints that reference me
  public Constraint determinedBy; // the constraint that currently determines
                                  // my value (or null if there isn't one)
  public int mark;                // used by the planner to mark constraints
  public Strength walkStrength;   // my walkabout strength
  public boolean stay;            // true if I am a planning-time constant
  public String	name;             // a symbolic name for reporting purposes


 // none(value), none(constraints), none(determinedBy), none(mark), none(this.walkStrength), Immutable(walkStrength), none(stay), none(this.name) ->
  // Unique(value), Unique(constraints), Unique(determinedBy), Unique(mark), Unique(this.walkStrength), Immutable(walkStrength), Unique(stay), Unique(this.name), Unique(result) 
  private Variable(String name, int initialValue, Strength walkStrength,int nconstraints)
  {
    value = initialValue;
    constraints = new ArrayList<Constraint>(nconstraints);
    determinedBy = null;
    mark= 0;
    this.walkStrength= walkStrength;
    stay= true;
    this.name= name;
  }
//none(value), Immutable(Strength.weakest), none(this.name) -> Unique(value), Immutable(Strength.weakest), Unique(this.name), Unique(result)
  public Variable(String name, int value)
  {
    this(name, value, Strength.weakest, 2);
  }
  // none(this.name), Immutable(Strength.weakest)-> Unique(this.name), Immutable(Strength.weakest)
  public Variable(String name)
  {
    this(name, 0, Strength.weakest, 2);
  }

//Immutable(this.name), Immutable(this.value), Immutable(this.walkStrength) -> Immutable(this.name), Immutable(this.value), Immutable(this.walkStrength)
  public void print()
  {
    System.out.print(name + "(");
    this.walkStrength.print();
    System.out.print("," + value + ")");
  }
  // Add the given constraint to the set of all constraints that refer to me.
  //Unique(this.constraints), Immutable(c) -> Unique(this.contraints), Immutable(c)
  public void addConstraint(Constraint c)
  {
    constraints.add(c);
  }
  // Remove all traces of c from this variable.
// Unique(this.constraints), Unique(this.determinedBy)-> Unique(this.constraints), Unique(this.determinedBy)
 //or Unique(Variable v)
  public void removeConstraint(Constraint c)
  {
    constraints.remove(c);//full(this)
    if (determinedBy == c) determinedBy = null;//full(this) -> none(this)
  }

  // Attempt to assign the given value to me using the given strength.
  //Full(this) -> Full(this)
  public void setValue(int value, Strength strength)
  {
    EditConstraint e= new EditConstraint(this, strength);
    if (e.isSatisfied()) {
      this.value= value;
      DeltaBlue.planner.propagateFrom(this);
    }
    e.destroyConstraint();
  }
}
//------------------------ constraints ------------------------------------

// I am an abstract class representing a system-maintainable
// relationship (or "constraint") between a set of variables. I supply
// a strength instance variable; concrete subclasses provide a means
// of storing the constrained variables and other information required
// to represent a constraint.

abstract class Constraint {
  public Strength strength; // the strength of this constraint
  //no permissions required
  protected Constraint() {} // this has to be here because of
                            // Java's constructor idiocy
  //none(this.strength), Immutable(strength) -> Unique(this.strength), Immutable(strength), Unique(result)
  protected Constraint(Strength strength)
  {
    this.strength= strength;
  }

  // Answer true if this constraint is satisfied in the current solution.
  public abstract boolean isSatisfied();

  // Record the fact that I am unsatisfied.
  public abstract void markUnsatisfied();

  // Normal constraints are not input constraints. An input constraint
  // is one that depends on external state, such as the mouse, the
  // keyboard, a clock, or some arbitrary piece of imperative code.
  // no permission required and generated here
  public boolean isInput() { return false; }

  // Activate this constraint and attempt to satisfy it.
  
  //Immutable(this) -> Immutable(this)
  protected void addConstraint()
  {
    addToGraph();
    DeltaBlue.planner.incrementalAdd(this);
  }
  // Deactivate this constraint, remove it from the constraint graph,
  // possibly causing other constraints to be satisfied, and destroy it.
  
  // Unique(this) -> Unique(this)
  public void destroyConstraint()
  {
    if (isSatisfied()) DeltaBlue.planner.incrementalRemove(this);
    removeFromGraph();
  }

  // Add myself to the constraint graph.
  public abstract void addToGraph();

  // Remove myself from the constraint graph.
  public abstract void removeFromGraph();

  // Decide if I can be satisfied and record that decision. The output
  // of the choosen method must not have the given mark and must have
  // a walkabout strength less than that of this constraint.
  protected abstract void chooseMethod(int mark);

  // Set the mark of all input from the given mark.
  protected abstract void markInputs(int mark);

  // Assume that I am satisfied. Answer true if all my current inputs
  // are known. A variable is known if either a) it is 'stay' (i.e. it
  // is a constant at plan execution time), b) it has the given mark
  // (indicating that it has been computed by a constraint appearing
  // earlier in the plan), or c) it is not determined by any
  // constraint.
  public abstract boolean inputsKnown(int mark);

  // Answer my current output variable. Raise an error if I am not
  // currently satisfied.
  public abstract Variable output();

  // Attempt to find a way to enforce this constraint. If successful,
  // record the solution, perhaps modifying the current dataflow
  // graph. Answer the constraint that this constraint overrides, if
  // there is one, or nil, if there isn't.
  // Assume: I am not already satisfied.
 
  //Immutable(Strength.required), Unique(this), Immutable(strength) -> Unique(this),Immutable(Strength.required), Immutable(strength), Unique(result)
  public Constraint satisfy(int mark)
  {
    chooseMethod(mark);// Immutable(mark)
    if (!isSatisfied()) {
      if (strength == Strength.required) {
    	  	DeltaBlue.error("Could not satisfy a required constraint");
      }
      return null;
    }
    // constraint can be satisfied
    // mark inputs to allow cycle detection in addPropagate
    markInputs(mark);
    Variable out = output();
    Constraint overridden= out.determinedBy;
    if (overridden != null) overridden.markUnsatisfied();
    out.determinedBy= this;
    
    if (!DeltaBlue.planner.addPropagate(out.determinedBy, mark)) {
      System.out.println("Cycle encountered");
      return null;
    }
    out.mark= mark;
    return overridden;
  }

  // Enforce this constraint. Assume that it is satisfied.
  public abstract void execute();

  // Calculate the walkabout strength, the stay flag, and, if it is
  // 'stay', the value for the current output of this
  // constraint. Assume this constraint is satisfied.
  public abstract void recalculate();

  protected abstract void printInputs();

  // none
  protected void printOutput() { output().print(); }

 // none
  public void print()
  {
    int i, outIndex;

    if (!isSatisfied()) {
      System.out.print("Unsatisfied");
    } else {
      System.out.print("Satisfied(");
      printInputs();
      System.out.print(" -> ");
      printOutput();
      System.out.print(")");
    }
    System.out.print("\n");
  }
}
//-------------unary constraints-------------------------------------------

// I am an abstract superclass for constraints having a single
// possible output variable.
//
abstract class UnaryConstraint extends Constraint {

  protected Variable myOutput; // possible output variable
  protected boolean satisfied; // true if I am currently satisfied

  //Immutable(v), Imm(strength),none(myOutput), none(satisfied)->Immutable(v), Imm(strength),Unique(myOutput), Unique(satisfied), Unique(result)
  protected UnaryConstraint(Variable v, Strength strength)
  {
    super(strength);
    myOutput= v;
    satisfied= false;
    addConstraint();
  }

  // Answer true if this constraint is satisfied in the current solution.
  // Immutable(satisfied) -> Immutable(satisfied)
  public boolean isSatisfied() { return satisfied; }

  // Record the fact that I am unsatisfied.
//Full(satisfied) -> Full(satisfied)
  public void markUnsatisfied() { satisfied = false; }

  // Answer my current output variable.
  //Immutable(myOutput) ->  Immutable(myOutput), Unique(result)
  public Variable output() { return myOutput; }

  // Add myself to the constraint graph.
  
  //Immutable(this), Full(myOutput), Full(satisfied)-> Immutable(this), Full(myOutput), Full(satisfied)
  public void addToGraph()
  {
    myOutput.addConstraint(this);
    satisfied = false;
  }

  // Remove myself from the constraint graph.
  //Unique(myOutput), Immutable(this), Full(satisfied) -> Unique(myOutput), Immutable(this), Full(satisfied)
  public void removeFromGraph()
  {
    if (myOutput != null) myOutput.removeConstraint(this);
    satisfied = false;
  }

  // Decide if I can be satisfied and record that decision.
  //Full(satisfied), Immutable(myOutput), Immutable(strength) -> Full(satisfied), Immutable(myOutput), Immutable(strength)
  protected void chooseMethod(int mark)
  {
    satisfied=myOutput.mark != mark
               && Strength.stronger(strength, myOutput.walkStrength);
  }

  protected void markInputs(int mark) {}   // I have no inputs

  
  public boolean inputsKnown(int mark) { return true; }

  // Calculate the walkabout strength, the stay flag, and, if it is
  // 'stay', the value for the current output of this
  // constraint. Assume this constraint is satisfied."
  //Full(myOutput), Immutable(strength) -> Full(myOutput), Immutable(strength)
  public void recalculate()
  {
    myOutput.walkStrength= strength;
    myOutput.stay = !isInput();
    if (myOutput.stay) execute(); // stay optimization
  }
  protected void printInputs() {} // I have no inputs
}
// I am a unary input constraint used to mark a variable that the
// client wishes to change.
//
class EditConstraint extends UnaryConstraint {

	//v is a local reference variable here passed as parameter from caller
// Immutable(str), none(myOutput), none(satisfied) -> Immutable (strength), Unique(myOutput),Unique(satisfied), Unique(result)
  public EditConstraint(Variable v, Strength str) 
  { 
	  super(v, str); 
  }

  // I indicate that a variable is to be changed by imperative code.
  
  //no permission required here, no permission is generated, it is returning a literal value
  public boolean isInput() { 
	  return true; 
	  }
  // no permission required here
  public void execute() {} // Edit constraints do nothing.

}

// I mark variables that should, with some level of preference, stay
// the same. I have one method with zero inputs and one output, which
// does nothing. Planners may exploit the fact that, if I am
// satisfied, my output will not change during plan execution. This is
// called "stay optimization".
//
class StayConstraint extends UnaryConstraint {

  // Install a stay constraint with the given strength on the given variable.
	// v is a local reference variable here passed as parameter from a caller side
// Immutable(str),none(myOutput), none(satisfied) -> Immutable(str),Unique(myOutput), Unique(satisfied), Unique(result)
	public StayConstraint(Variable v, Strength str) { 
		super(v, str); 
	}

  public void execute() {} // Stay constraints do nothing.

}
//-------------binary constraints-------------------------------------------

// I am an abstract superclass for constraints having two possible
// output variables.
//
abstract class BinaryConstraint extends Constraint {

  protected Variable v1, v2; // possible output variables
  protected byte direction; // one of the following...
  protected static byte backward= -1;    // v1 is output
  protected static byte nodirection= 0;  // not satisfied
  protected static byte forward= 1;      // v2 is output

  protected BinaryConstraint() {} // this has to be here because of
                                  // Java's constructor idiocy.
// var1 and var2 are local ref variable passed from caller
  //Immutable(strength), none(v1), none(v2), none(direction), Immutable(nodirection) -> Unique(v1), Unique(v2),Unique(direction),Immutable(nodirection),  
  protected BinaryConstraint(Variable var1, Variable var2, Strength strength) {
    super(strength);
    v1= var1;
    v2= var2;
    direction= nodirection;
    addConstraint();
  }
//Remove myself from the constraint graph.
 //Unique(v1), Unique(v2), Immutable(this), Full(direction), Immutable(nodirection) ->  Unique(v1), Unique(v2), Immutable(this), Full(direction), Immutable(nodirection) ->
 public void removeFromGraph()
 {
   if (v1 != null) v1.removeConstraint(this);
   if (v2 != null) v2.removeConstraint(this);
   direction= nodirection;
 }
  // Answer true if this constraint is satisfied in the current solution.
  //Immutable(direction), Immutable(nodirection) ->Immutable(direction), Immutable(nodirection) 
  public boolean isSatisfied() { return direction != nodirection; }

  // Add myself to the constraint graph.
  //Full(v1), Full(v2),Full(direction), Immutable(nodirection), Immutable(this) -> Full(v1), Full(v2),Full(direction), Immutable(nodirection), Immutable(this) 
  public void addToGraph()
  {
    v1.addConstraint(this);
    v2.addConstraint(this);
    direction = nodirection;
  }
  

  // Decide if I can be satisfied and which way I should flow based on
  // the relative strength of the variables I relate, and record that decision.
  ///Immutable(v1), Immutable(v2), Immutable(strength), Full(direction),Immutable(nodirection),Immutable(backward) -> Immutable(v1), Immutable(v2),Immutable(strength), Full(direction),Immutable(nodirection), Immutable(nodirection),Immutable(backward) 
  protected void chooseMethod(int mark)
  {
    if (v1.mark == mark) 
      direction=
	v2.mark != mark && Strength.stronger(strength, v2.walkStrength)
	  ? forward : nodirection;
    if (v2.mark == mark) 
      direction=
	v1.mark != mark && Strength.stronger(strength, v1.walkStrength)
	  ? backward : nodirection;
    // If we get here, neither variable is marked, so we have a choice.
    if (Strength.weaker(v1.walkStrength, v2.walkStrength))
      direction=
	Strength.stronger(strength, v1.walkStrength) ? backward : nodirection;
    else
      direction=
	Strength.stronger(strength, v2.walkStrength) ? forward : nodirection;
  }

  // Record the fact that I am unsatisfied.
  // Full(direction), Immutable(nodirection) -> Full(direction), Immutable(nodirection)
  public void markUnsatisfied() { direction= nodirection; }

  // Mark the input variable with the given mark.
  //Full(v1), Full(v2),Immutable(direction), Immutable(forward) -> Full(v1), Full(v2), Immutable(direction), Immutable(forward), Unique(result)
  protected void markInputs(int mark)
  {
    input().mark = mark;
  }
  //Immutable(v1), Immutable(v2)->Immutable(v1), Immutable(v2)
  public boolean inputsKnown(int mark)
  {
    Variable i = input();
    return i.mark == mark || i.stay || i.determinedBy == null;
  }
  
  // Answer my current output variable.
//Immutable(v1), Immutable(v2),Immutable(direction), Immutable(forward) -> Immutable(v1), Immutable(v2),Immutable(direction), Immutable(forward), Unique(result)
  public Variable output() { return direction==forward ? v2 : v1; }

  // Answer my current input variable
//Immutable(v1), Immutable(v2),Immutable(direction), Immutable(forward)->Immutable(v1), Immutable(v2),Immutable(direction), Immutable(forward), Unique(result)
  public Variable input() { return direction== forward ? v1 : v2; }

  // Calculate the walkabout strength, the stay flag, and, if it is
  // 'stay', the value for the current output of this
  // constraint. Assume this constraint is satisfied.
  //
  //Full(v1), Full(v2), Immutable(strength) ->Full(out), Full(in), Immutable(strength) 
  public void recalculate()
  {
    Variable in = input(), out = output();
    out.walkStrength = Strength.weakestOf(strength, in.walkStrength);
    out.stay = in.stay;
    if (out.stay) execute();
  }

  //Immutable(v1), immutable(v2),
  protected void printInputs()
  {
    input().print();
  }

}
// I constrain two variables to have the same value: "v1 = v2".
class EqualityConstraint extends BinaryConstraint {

  // Install a constraint with the given strength equating the given variables.
	
//Immutable(strength), none(v1), none(v2), none(direction), Immutable(nodirection) -> Unique(v1), Unique(v2), Unique(strength), Unique(direction),Immutable(nodirection),   
  public EqualityConstraint(Variable var1, Variable var2, Strength strength)
  {
    super(var1, var2, strength);
  }
  // Enforce this constraint. Assume that it is satisfied.
  //Full(v1), Full(v2)-> Full(v1), Full(v2)
  public void execute() {
    output().value= input().value;
  }

}
// I relate two variables by the linear scaling relationship: "v2 =
// (v1 * scale) + offset". Either v1 or v2 may be changed to maintain
// this relationship but the scale factor and offset are considered
// read-only.
//
class ScaleConstraint extends BinaryConstraint {

  protected Variable scale; // scale factor input variable
  protected Variable offset; // offset input variable

  // Install a scale constraint with the given strength on the given variables.
  //none(this.strength), none(v1), none(v2), none (direction), none(this,scale), none(this.offset), Immutable(Strength.required) - > 
 
 // -> Unique(this.strength), Unique(v1), Unique(v2), Unique(direction), Unique(this,scale), Unique(this.offset), Immutable(Strength.required), Unique(result)  
  public ScaleConstraint(Variable src, Variable scale, Variable offset,
		         Variable dest, Strength strength){
    // Curse this wretched language for insisting that constructor invocation, must be the first thing in a method..because of that, we must copy the code from the inherited constructors.
    this.strength= strength;
    v1= src;
    v2= dest;
    direction= nodirection;
    this.scale= scale;
    this.offset= offset;
    addConstraint();
  }
  // Add myself to the constraint graph.
  //Full(scale), Immutable(this), Full(offset) -> Full(scale), Immutable(this), Full(offset) 
  public void addToGraph()
  {
    super.addToGraph();
    scale.addConstraint(this);
    offset.addConstraint(this);
  }
  // Remove myself from the constraint graph.
  //Unique(scale), Unique(offset), Immutable(this)- >  Unique(scale), Unique(offset), Immutable(this)
  public void removeFromGraph()
  {
    super.removeFromGraph();
    if (scale != null) scale.removeConstraint(this);
    if (offset != null) offset.removeConstraint(this);
  }
  // Mark the inputs from the given mark.
  //Full(scale), Full(offset) -> Full(scale), Full(offset)
  protected void markInputs(int mark)
  {
     super.markInputs(mark);
	 offset.mark = mark;
     scale.mark = offset.mark;
  }

  // Enforce this constraint. Assume that it is satisfied.
  //Imm(direction), Imm(forward), Full(v2), Imm(v1), Imm(scale), Imm(offset) -> Imm(direction), Imm(forward), Full(v2), Imm(v1), Imm(scale), Imm(offset)
  
  /*@Perm(requires="full(this) in alive",
		  347 ensures="full(this) in alive")*/
  public void execute()
  {
    if (direction == forward) 
      v2.value= v1.value * scale.value + offset.value;
    else
      v1.value= (v2.value - offset.value) / scale.value;
  }

  // Calculate the walkabout strength, the stay flag, and, if it is
  // 'stay', the value for the current output of this constraint. Assume this constraint is satisfied.
  //Full(v1), Full(v2), Imm(scale), Imm(offset), Immutable(strength) -> Full(v1), Full(v2), Imm(scale), Imm(offset), Immutable(strength) 
  
  public void recalculate()
  {
    Variable in= input(), out= output();
    out.walkStrength = Strength.weakestOf(strength, in.walkStrength);
    out.stay= in.stay && scale.stay && offset.stay;
    if (out.stay) execute(); // stay optimization
  }
}
// A Plan is an ordered list of constraints to be executed in sequence
// to resatisfy all currently satisfiable constraints in the face of
// one or more changing inputs.

class Plan {

  private ArrayList<Constraint> v;
  
  //none(v) -> Unique(v)
  public Plan() { 
	  v = new ArrayList<Constraint>(); 
	  }
  //Full(v), Immutable(c) -> Full(v), Immutable(c)
  public void addConstraint(Constraint c) { 
	  v.add(c); 
	  }
  //Immutable (v) -> Immutable(v), Unique(result)
  public int size() { return v.size(); 
  }
//Immutable(v) -> Immutable(v), Unique(result)
  public Constraint constraintAt(int index) {
    return v.get(index); 
    }
// Full(v) -> Full(v)  
  public void execute()
  {
    for (int i= 0; i < size(); ++i) {
      Constraint c = constraintAt(i);
      c.execute();
    }
  }

}
// ------------------------------------------------------------
// The DeltaBlue planner

class Planner {

  int currentMark= 0;

  //none(currentMark) -> Unique(currentMark)
  public Planner()
  {
    currentMark= 0;
  }
  // Select a previously unused mark value.
  // Full(currentMark) -> Full(currentMark)
  private int newMark() { return ++currentMark; }
  
  // Attempt to satisfy the given constraint and, if successful,
  // incrementally update the dataflow graph.  Details: If satifying
  // the constraint is successful, it may override a weaker constraint
  // on its output. The algorithm attempts to resatisfy that
  // constraint using some other method. This process is repeated
  // until either a) it reaches a variable that was not previously
  // determined by any constraint or b) it reaches a constraint that
  // is too weak to be satisfied using any of its methods. The
  // variables of constraints that have been processed are marked with
  // a unique mark value so that we know where we've been. This allows
  // the algorithm to avoid getting into an infinite loop even if the
  // constraint graph has an inadvertent cycle.
  
  //Full(c) -> Full(c)
  public void incrementalAdd(Constraint c)
  {
    int mark = newMark();
    Constraint overridden = c.satisfy(mark);
    while (overridden != null) {
      overridden = overridden.satisfy(mark);
    }
  }
  // Entry point for retracting a constraint. Remove the given
  // constraint and incrementally update the dataflow graph.
  // Details: Retracting the given constraint may allow some currently
  // unsatisfiable downstream constraint to be satisfied. We therefore collect
  // a list of unsatisfied downstream constraints and attempt to
  // satisfy each one in turn. This list is traversed by constraint
  // strength, strongest first, as a heuristic for avoiding
  // unnecessarily adding and then overriding weak constraints.
  // Assume: c is satisfied.
  
  //Unique(c), Immutable(Strength.weakest), Immutable(Strength.required) -> Unique(c), Immutable(Strength.weakest), Immutable(Strength.required)
  public void incrementalRemove(Constraint c)
  {
    Variable out= c.output();
    c.markUnsatisfied();
    c.removeFromGraph();
    ArrayList<Constraint> unsatisfied= removePropagateFrom(out);
    Strength strength = Strength.required;
    do {
      for (int i= 0; i < unsatisfied.size(); ++i) {
	Constraint u= unsatisfied.get(i);
	if (u.strength == strength)
	  incrementalAdd(u);
      }
      strength = strength.nextWeaker();
    } while (strength != Strength.weakest);
  }

  // Recompute the walkabout strengths and stay flags of all variables
  // downstream of the given constraint and recompute the actual
  // values of all variables whose stay flag is true. If a cycle is
  // detected, remove the given constraint and answer
  // false. Otherwise, answer true.
  // Details: Cycles are detected when a marked variable is
  // encountered downstream of the given constraint. The sender is
  // assumed to have marked the inputs of the given constraint with
  // the given mark. Thus, encountering a marked node downstream of
  // the output constraint means that there is a path from the
  // constraint's output to one of its inputs.
  
  //Unique(c) -> Unique(c)
  public boolean addPropagate(Constraint c, int mark)
  {
    ArrayList<Constraint> todo= new ArrayList<Constraint>();
    todo.add(c);
    while (!todo.isEmpty()) {
      Constraint d= todo.get(0);
      todo.remove(0);
      if (d.output().mark == mark) {
	incrementalRemove(c);
	return false;
      }
      d.recalculate();
      addConstraintsConsumingTo(d.output(), todo);
    }
    return true;
  }
  // The given variable has changed. Propagate new values downstream.
  // Full(v) -> Full(v)
  public void propagateFrom(Variable v)
  {
    ArrayList<Constraint> todo= new ArrayList<Constraint>();
    addConstraintsConsumingTo(v, todo);
    while (!todo.isEmpty()) {
      Constraint c = todo.get(0);
      todo.remove(0);
      c.execute();
      addConstraintsConsumingTo(c.output(), todo);
    }
  }

  // Update the walkabout strengths and stay flags of all variables
  // downstream of the given constraint. Answer a collection of
  // unsatisfied constraints sorted in order of decreasing strength.
  
  //Full(out), Immutable(Strength.weakest) -> Full(out), , Immutable(Strength.weakest), Unique(result)
  protected ArrayList<Constraint> removePropagateFrom(Variable out)
  {
    out.determinedBy= null;
    out.walkStrength= Strength.weakest;
    out.stay= true;
    ArrayList<Constraint> unsatisfied= new ArrayList<Constraint>();
    ArrayList<Variable> todo= new ArrayList<Variable>();
    todo.add(out);
    while (!todo.isEmpty()) {
      Variable v = todo.get(0);
      todo.remove(0);
      for (int i= 0; i < v.constraints.size(); ++i) {
	Constraint c = v.constraints.get(i);
	if (!c.isSatisfied())
	  unsatisfied.add(c);
      }
      Constraint determiningC= v.determinedBy;
      for (int i= 0; i < v.constraints.size(); ++i) {
	Constraint nextC= v.constraints.get(i);
	if (nextC != determiningC && nextC.isSatisfied()) {
	  nextC.recalculate();
	  todo.add(nextC.output());
	}
      }
    }
    return unsatisfied;
  }

  // Extract a plan for resatisfaction starting from the outputs of
  // the given constraints, usually a set of input constraints.
  
  // none(constraint) -> Unique(result)
  protected Plan extractPlanFromConstraints(ArrayList<Constraint> constraints)
  {
    ArrayList<Constraint> sources = new ArrayList<Constraint>();
    for (int i= 0; i < constraints.size(); ++i) {
      Constraint c = constraints.get(i);
      if (c.isInput() && c.isSatisfied())
    	  sources.add(c);
    }
    return makePlan(sources);
  }

  // Extract a plan for resatisfaction starting from the given source
  // constraints, usually a set of input constraints. This method
  // assumes that stay optimization is desired; the plan will contain
  // only constraints whose output variables are not stay. Constraints
  // that do no computation, such as stay and edit constraints, are
  // not included in the plan.
  // Details: The outputs of a constraint are marked when it is added
  // to the plan under construction. A constraint may be appended to
  // the plan when all its input variables are known. A variable is
  // known if either a) the variable is marked (indicating that has
  // been computed by a constraint appearing earlier in the plan), b)
  // the variable is 'stay' (i.e. it is a constant at plan execution
  // time), or c) the variable is not determined by any
  // constraint. The last provision is for past states of history
  // variables, which are not stay but which are also not computed by
  // any constraint.
  // Assume: sources are all satisfied.

  // none(sources) -> Unique(result)
  protected Plan makePlan(ArrayList<Constraint> sources)
  {
    int mark= newMark();
    Plan plan = new Plan();
    ArrayList<Constraint> todo = sources;
    while (!todo.isEmpty()) {
      Constraint c= todo.get(0);
      todo.remove(0);
      if (c.output().mark != mark && c.inputsKnown(mark)) {
	// not in plan already and eligible for inclusion
	plan.addConstraint(c);
	c.output().mark= mark;
	addConstraintsConsumingTo(c.output(), todo);
      }
    }
    return plan;
  }
// Full(v) -> none(coll) -> Full(v)
  protected void addConstraintsConsumingTo(Variable v, ArrayList<Constraint> coll)
  {
    Constraint determiningC= v.determinedBy;
    ArrayList<Constraint> cc= v.constraints;
    for (int i= 0; i < cc.size(); ++i) {
      Constraint c = cc.get(i);
      if (c != determiningC && c.isSatisfied())
	coll.add(c);
    }
  }
}

//------------------------------------------------------------

public class DeltaBlue /* implements Benchmark */ {

  private long total_ms;
  public static Planner planner;
  public int totalcontrains;

  //none(planner), none(total) -> Unique(planner), Unique(total), Unique(result)
  public DeltaBlue(){
	  planner = new Planner();
	  totalcontrains = 100;
	  total_ms = 0;
  }
  // Immutable(total_ms)  -> Immutable(total_ms), Unique(result)
  public long getRunTime() { 
	  return this.total_ms; 
	  }

  //  This is the standard DeltaBlue benchmark. A long chain of
  //  equality constraints is constructed with a stay constraint on
  //  one end. An edit constraint is then added to the opposite end
  //  and the time is measured for adding and removing this
  //  constraint, and extracting and executing a constraint
  //  satisfaction plan. There are two cases. In case 1, the added
  //  constraint is stronger than the stay constraint and values must
  //  propagate down the entire length of the chain. In case 2, the
  //  added constraint is weaker than the stay constraint so it cannot
  //  be accomodated. The cost in this case is, of course, very
  //  low. Typical situations lie somewhere between these two
  //  extremes.
  //

  //Immutable(planner), immutable(total), Imm(Strength.required), Imm(Strength.strongDefault), Imm(Strength.preferred) -> Imutable(planner), Immutable(total),  Imm(Strength.required), Imm(Strength.strongDefault), Imm(Strength.preferred)
  private void chainTest()
  {
       Variable prev= null, first= null, last= null;
       int total = this.totalcontrains;
    // Build chain of n equality constraints
    for (int i= 0; i <= total; i++) {
      String name= "v" + Integer.toString(i);
      Variable v = new Variable(name);
      if (prev != null)
    	  new EqualityConstraint(prev, v, Strength.required);
      if (i == 0) first = v;
      if (i == total) last = v;
      prev = v;
    }
    
    new StayConstraint(last, Strength.strongDefault);
    
    Constraint editC = new EditConstraint(first, Strength.preferred);
    
    ArrayList<Constraint> editV= new ArrayList<Constraint>();
    editV.add(editC);
    
    Plan plan = planner.extractPlanFromConstraints(editV);
    
    for (int i= 0; i < 100; i++) {
      first.value = i;
      plan.execute();
      if (last.value != i)
    	  	error("Chain test failed!");
    	}
      editC.destroyConstraint();
  }
  // This test constructs a two sets of variables related to each
  // other by a simple linear transformation (scale and offset). The
  // time is measured to change a variable on either side of the
  // mapping and to change the scale and offset factors.
  
  // Immutable(planner), Immutable(total), Imm(Strength.normal), Imm(Strength.required) -> Immutable(planner), Imm(Strength.normal), Imm(Strength.normal), Imm(Strength.required)  
  private void projectionTest(int total)
  {
    Variable scale = new Variable("scale", 10);
    Variable offset = new Variable("offset", 1000);
    Variable src = null, dst= null;

    ArrayList<Variable> dests= new ArrayList<Variable>();

    for (int i= 0; i < total; ++i) {
      src = new Variable("src" + Integer.toString(i), i);
      dst = new Variable("dst" + Integer.toString(i), i);
      dests.add(dst);
      new StayConstraint(src, Strength.normal);
      new ScaleConstraint(src, scale, offset, dst, Strength.required);
    }

    change(src, 17);
    if (dst.value != 1170) error("Projection test 1 failed!");

    change(dst, 1050);
    if (src.value != 5) error("Projection test 2 failed!");

    change(scale, 5);
    for (int i= 0; i < total - 1; ++i) {
      if ((dests.get(i)).value != i * 5 + 1000)
	error("Projection test 3 failed!");
    }
    change(offset, 2000);
    for (int i= 0; i < total - 1; ++i) {
      if ((dests.get(i)).value != i * 5 + 2000)
	error("Projection test 4 failed!");
    }
  }
  //Immutable(planner), Immutable(strength.preferred)->  Immutable(strength.preferred),Immutable(planner)
  private void change(Variable var, int newValue)
  {
    EditConstraint editC = new EditConstraint(var, Strength.preferred);
    ArrayList<Constraint> editV= new ArrayList<Constraint>();
    editV.add(editC);
    Plan plan = planner.extractPlanFromConstraints(editV);
    for (int i= 0; i < 10; i++) {
      var.value = newValue;
      plan.execute();
    }
    editC.destroyConstraint();
  }
  //no permissions required
  public static void error(String s)
  {
    System.err.println(s);
    System.exit(1);
  }
//Immutable(this.total) -> Immutable(this.total)
 public void inst_main(int n)
 {
   int iterations = n;
   String options = "";

   /*if (args != null && args.length > 0)
     iterations = Integer.parseInt(args[0]);

   if (args != null && args.length > 1)
     options = args[1];*/

   long start = System.nanoTime();// do -nothin
   
   for (int j = 0; j < iterations; ++j) {
     this.chainTest();
     projectionTest(this.totalcontrains);
   }
   long end = System.nanoTime();
	long elapsedTime = end - start;
	double ms = (double)elapsedTime / 1000000.0;
	System.out.println(" Milli Seconds Time = "+ms);
 }
  public static void main(String[] args) {
	   DeltaBlue db = new DeltaBlue();// unique db
	   db.inst_main(1000);
	   
  }
  }
/*
 * 
 * Class Name = Strength
Class Name = DeltaBlue
Method Name = main
Vertex Name = deltaBlue.Strength.required, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Strength.strongPreferred, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Strength.preferred, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Strength.strongDefault, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Strength.normal, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Strength.weakDefault, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Strength.weakest, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.backward, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.nodirection, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.forward, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.DeltaBlue.planner, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.DeltaBlue.planner.planner, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.DeltaBlue.totalcontrains, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.DeltaBlue.total_ms, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.DeltaBlue.planner.currentMark, Post Permissions = unique, Pre-Permissions =none
Vertex Name = db.totalcontrains, Post Permissions = none, Pre-Permissions =none
Vertex Name = db.total_ms, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.Strength.planner, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.Strength.value, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.value, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.constraints, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.mark, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.walkStrength, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.stay, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.name, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.weakest, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.BinaryConstraint.v1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.BinaryConstraint.v2, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.BinaryConstraint.direction, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.BinaryConstraint.nodirection, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.UnaryConstraint.planner, Post Permissions = none, Pre-Permissions =none
Vertex Name = c.strength, Post Permissions = none, Pre-Permissions =none
Vertex Name = c.required, Post Permissions = none, Pre-Permissions =none
Vertex Name = c.determinedBy, Post Permissions = none, Pre-Permissions =none
Vertex Name = c.planner, Post Permissions = unique, Pre-Permissions =none
Vertex Name = c.mark, Post Permissions = unique, Pre-Permissions =none
Vertex Name = planner.mark, Post Permissions = none, Pre-Permissions =none
Vertex Name = planner.required, Post Permissions = none, Pre-Permissions =none
Vertex Name = planner.strength, Post Permissions = none, Pre-Permissions =none
Vertex Name = planner.weakest, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.stay, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = unique, Pre-Permissions =none
Vertex Name = strength.strengthValue, Post Permissions = none, Pre-Permissions =none
Vertex Name = strength.weakest, Post Permissions = none, Pre-Permissions =none
Vertex Name = strength.weakDefault, Post Permissions = none, Pre-Permissions =none
Vertex Name = strength.normal, Post Permissions = none, Pre-Permissions =none
Vertex Name = strength.strongDefault, Post Permissions = none, Pre-Permissions =none
Vertex Name = strength.preferred, Post Permissions = none, Pre-Permissions =none
Vertex Name = strength.strongPreferred, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.UnaryConstraint.myOutput, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.UnaryConstraint.satisfied, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.mark, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.Plan.v, Post Permissions = unique, Pre-Permissions =none
Vertex Name = plan.v, Post Permissions = unique, Pre-Permissions =none
Vertex Name = e.planner, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.v1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.v2, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.direction, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.scale, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.offset, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.DeltaBlue.preferred, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.DeltaBlue.value, Post Permissions = unique, Pre-Permissions =none
Method Name = Strength
Vertex Name = deltaBlue.Strength.strengthValue, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Strength.name, Post Permissions = unique, Pre-Permissions =none
Vertex Name = walkStrength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Method Name = print
Vertex Name = walkStrength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Method Name = stronger
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.walkStrength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Method Name = weaker
Vertex Name = deltaBlue.Strength.walkStrength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.walkStrength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Method Name = weakestOf
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.walkStrength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.walkStrength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.strengthValue, Post Permissions = share, Pre-Permissions =share
Method Name = strongest
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.walkStrength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Method Name = nextWeaker
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Class Name = Variable
Method Name = Variable
Vertex Name = deltaBlue.Variable.value, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.constraints, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.mark, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.stay, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.name, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = print
Vertex Name = deltaBlue.Variable.name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.walkStrength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.value, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = walkStrength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Method Name = addConstraint
Vertex Name = myOutput.constraints, Post Permissions = share, Pre-Permissions =share
Method Name = removeConstraint
Vertex Name = myOutput.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = myOutput.determinedBy, Post Permissions = unique, Pre-Permissions =none
Method Name = setValue
Vertex Name = deltaBlue.Strength.value, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Strength.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.UnaryConstraint.myOutput, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.satisfied, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = e.satisfied, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = e.planner, Post Permissions = pure, Pre-Permissions =pure
Method Name = EditConstraint
Vertex Name = deltaBlue.Strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.UnaryConstraint.myOutput, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.satisfied, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = UnaryConstraint
Vertex Name = deltaBlue.UnaryConstraint.myOutput, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.satisfied, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = Constraint
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = share, Pre-Permissions =share
Method Name = addConstraint
Vertex Name = deltaBlue.UnaryConstraint.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = addToGraph
Method Name = incrementalAdd
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = newMark
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Method Name = satisfy
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = chooseMethod
Method Name = isSatisfied
Method Name = error
Method Name = markInputs
Method Name = output
Method Name = markUnsatisfied
Method Name = addPropagate
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = share, Pre-Permissions =share
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Method Name = incrementalRemove
Vertex Name = planner.required, Post Permissions = share, Pre-Permissions =share
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Method Name = removeFromGraph
Method Name = removePropagateFrom
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Method Name = recalculate
Method Name = addConstraintsConsumingTo
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = pure, Pre-Permissions =pure
Method Name = isSatisfied
Vertex Name = e.satisfied, Post Permissions = pure, Pre-Permissions =pure
Method Name = propagateFrom
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = pure, Pre-Permissions =pure
Method Name = execute
Method Name = destroyConstraint
Vertex Name = e.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Class Name = Constraint
Method Name = Constraint
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = share, Pre-Permissions =share
Method Name = isSatisfied
Method Name = markUnsatisfied
Method Name = isInput
Method Name = addConstraint
Vertex Name = deltaBlue.UnaryConstraint.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = destroyConstraint
Vertex Name = e.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = addToGraph
Method Name = removeFromGraph
Method Name = chooseMethod
Method Name = markInputs
Method Name = inputsKnown
Method Name = output
Method Name = satisfy
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = execute
Method Name = recalculate
Method Name = printInputs
Method Name = printOutput
Vertex Name = deltaBlue.Variable.name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.walkStrength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.value, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = walkStrength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Method Name = print
Vertex Name = deltaBlue.Variable.name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.walkStrength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.value, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = walkStrength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Class Name = UnaryConstraint
Method Name = UnaryConstraint
Vertex Name = deltaBlue.UnaryConstraint.myOutput, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.satisfied, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = isSatisfied
Vertex Name = e.satisfied, Post Permissions = pure, Pre-Permissions =pure
Method Name = markUnsatisfied
Vertex Name = deltaBlue.UnaryConstraint.satisfied, Post Permissions = share, Pre-Permissions =share
Method Name = output
Vertex Name = deltaBlue.UnaryConstraint.myOutput, Post Permissions = pure, Pre-Permissions =pure
Method Name = addToGraph
Vertex Name = deltaBlue.UnaryConstraint.myOutput, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.UnaryConstraint.satisfied, Post Permissions = share, Pre-Permissions =share
Vertex Name = myOutput.constraints, Post Permissions = share, Pre-Permissions =share
Method Name = removeFromGraph
Vertex Name = deltaBlue.UnaryConstraint.myOutput, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.UnaryConstraint.satisfied, Post Permissions = share, Pre-Permissions =share
Vertex Name = myOutput.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = myOutput.determinedBy, Post Permissions = unique, Pre-Permissions =none
Method Name = chooseMethod
Vertex Name = deltaBlue.UnaryConstraint.satisfied, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.UnaryConstraint.myOutput, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.UnaryConstraint.walkStrength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.walkStrength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Method Name = markInputs
Method Name = inputsKnown
Method Name = recalculate
Vertex Name = deltaBlue.Variable.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.myOutput, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.stay, Post Permissions = share, Pre-Permissions =share
Method Name = printInputs
Class Name = EditConstraint
Method Name = EditConstraint
Vertex Name = deltaBlue.Strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.UnaryConstraint.myOutput, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.satisfied, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = isInput
Method Name = execute
Class Name = StayConstraint
Method Name = StayConstraint
Vertex Name = deltaBlue.Strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.UnaryConstraint.myOutput, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.satisfied, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = execute
Class Name = BinaryConstraint
Method Name = BinaryConstraint
Vertex Name = deltaBlue.BinaryConstraint.v1, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.BinaryConstraint.v2, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.BinaryConstraint.direction, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.BinaryConstraint.nodirection, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = isSatisfied
Vertex Name = deltaBlue.BinaryConstraint.direction, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.nodirection, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = addToGraph
Vertex Name = deltaBlue.BinaryConstraint.v1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.v2, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.direction, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.BinaryConstraint.nodirection, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = myOutput.constraints, Post Permissions = share, Pre-Permissions =share
Method Name = removeFromGraph
Vertex Name = deltaBlue.BinaryConstraint.v1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.v2, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.direction, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.BinaryConstraint.nodirection, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = myOutput.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = myOutput.determinedBy, Post Permissions = unique, Pre-Permissions =none
Method Name = chooseMethod
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.v1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.direction, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.v2, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.walkStrength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.forward, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Variable.nodirection, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Variable.backward, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.walkStrength, Post Permissions = pure, Pre-Permissions =pure
Method Name = markUnsatisfied
Vertex Name = deltaBlue.BinaryConstraint.direction, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.BinaryConstraint.nodirection, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = markInputs
Vertex Name = deltaBlue.Variable.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.BinaryConstraint.direction, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.forward, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.BinaryConstraint.v1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.v2, Post Permissions = pure, Pre-Permissions =pure
Method Name = input
Vertex Name = deltaBlue.BinaryConstraint.direction, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.forward, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.BinaryConstraint.v1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.v2, Post Permissions = pure, Pre-Permissions =pure
Method Name = inputsKnown
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.stay, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.determinedBy, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.direction, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.forward, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.BinaryConstraint.v1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.v2, Post Permissions = pure, Pre-Permissions =pure
Method Name = output
Vertex Name = deltaBlue.BinaryConstraint.direction, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.forward, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.BinaryConstraint.v2, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.v1, Post Permissions = share, Pre-Permissions =share
Method Name = recalculate
Vertex Name = deltaBlue.Variable.walkStrength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.BinaryConstraint.direction, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.forward, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.BinaryConstraint.v1, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.BinaryConstraint.v2, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.walkStrength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Method Name = printInputs
Vertex Name = deltaBlue.Variable.name, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.walkStrength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.value, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = walkStrength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.direction, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.forward, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.BinaryConstraint.v1, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.v2, Post Permissions = pure, Pre-Permissions =pure
Class Name = EqualityConstraint
Method Name = EqualityConstraint
Vertex Name = deltaBlue.Strength.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.v1, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.BinaryConstraint.v2, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.BinaryConstraint.direction, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.BinaryConstraint.nodirection, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = execute
Vertex Name = deltaBlue.Variable.value, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.BinaryConstraint.direction, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.forward, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.BinaryConstraint.v2, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.v1, Post Permissions = share, Pre-Permissions =share
Class Name = ScaleConstraint
Method Name = ScaleConstraint
Vertex Name = deltaBlue.Strength.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Constraint.v1, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Constraint.v2, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Constraint.direction, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.nodirection, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Constraint.scale, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Constraint.offset, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = addToGraph
Vertex Name = deltaBlue.ScaleConstraint.scale, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.ScaleConstraint.offset, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = myOutput.constraints, Post Permissions = share, Pre-Permissions =share
Method Name = removeFromGraph
Vertex Name = deltaBlue.ScaleConstraint.scale, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.ScaleConstraint.offset, Post Permissions = share, Pre-Permissions =share
Vertex Name = myOutput.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = myOutput.determinedBy, Post Permissions = unique, Pre-Permissions =none
Method Name = markInputs
Vertex Name = deltaBlue.Variable.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.scale, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.offset, Post Permissions = pure, Pre-Permissions =pure
Method Name = execute
Vertex Name = deltaBlue.BinaryConstraint.direction, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.forward, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.BinaryConstraint.value, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.BinaryConstraint.v2, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.v1, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.BinaryConstraint.scale, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.offset, Post Permissions = pure, Pre-Permissions =pure
Method Name = recalculate
Vertex Name = deltaBlue.Variable.walkStrength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.scale, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.offset, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.direction, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.forward, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.BinaryConstraint.v1, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.BinaryConstraint.v2, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.walkStrength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.strengthValue, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.BinaryConstraint.value, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.BinaryConstraint.scale, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.BinaryConstraint.offset, Post Permissions = pure, Pre-Permissions =pure
Class Name = Plan
Method Name = Plan
Vertex Name = deltaBlue.Plan.v, Post Permissions = unique, Pre-Permissions =none
Method Name = addConstraint
Vertex Name = plan.v, Post Permissions = share, Pre-Permissions =share
Method Name = size
Vertex Name = deltaBlue.Plan.v, Post Permissions = share, Pre-Permissions =share
Method Name = constraintAt
Vertex Name = deltaBlue.Plan.v, Post Permissions = share, Pre-Permissions =share
Method Name = execute
Vertex Name = deltaBlue.Plan.v, Post Permissions = share, Pre-Permissions =share
Class Name = Planner
Method Name = Planner
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.DeltaBlue.planner.currentMark, Post Permissions = unique, Pre-Permissions =none
Method Name = newMark
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Method Name = incrementalAdd
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = incrementalRemove
Vertex Name = planner.required, Post Permissions = share, Pre-Permissions =share
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Method Name = addPropagate
Vertex Name = deltaBlue.Variable.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = share, Pre-Permissions =share
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Method Name = propagateFrom
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = pure, Pre-Permissions =pure
Method Name = removePropagateFrom
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Method Name = extractPlanFromConstraints
Vertex Name = deltaBlue.Variable.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Plan.v, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = plan.v, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = pure, Pre-Permissions =pure
Method Name = makePlan
Vertex Name = deltaBlue.Variable.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Plan.v, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = plan.v, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = pure, Pre-Permissions =pure
Method Name = addConstraintsConsumingTo
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = pure, Pre-Permissions =pure
Method Name = DeltaBlue
Vertex Name = deltaBlue.DeltaBlue.planner, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.DeltaBlue.planner.planner, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.DeltaBlue.totalcontrains, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.DeltaBlue.total_ms, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.DeltaBlue.planner.currentMark, Post Permissions = unique, Pre-Permissions =none
Method Name = getRunTime
Vertex Name = deltaBlue.DeltaBlue.total_ms, Post Permissions = pure, Pre-Permissions =pure
Method Name = chainTest
Vertex Name = db.totalcontrains, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Strength.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.value, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.value, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.constraints, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.mark, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.stay, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.name, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.BinaryConstraint.v1, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.BinaryConstraint.v2, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.BinaryConstraint.direction, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.BinaryConstraint.nodirection, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.UnaryConstraint.myOutput, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.satisfied, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Plan.v, Post Permissions = unique, Pre-Permissions =none
Vertex Name = plan.v, Post Permissions = share, Pre-Permissions =share
Vertex Name = e.planner, Post Permissions = pure, Pre-Permissions =pure
Method Name = projectionTest
Vertex Name = db.totalcontrains, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Strength.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.value, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Variable.value, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.constraints, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.mark, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.stay, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.name, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.UnaryConstraint.myOutput, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.satisfied, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Constraint.v1, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Constraint.v2, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Constraint.direction, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.nodirection, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Constraint.scale, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Constraint.offset, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.DeltaBlue.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.DeltaBlue.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.DeltaBlue.value, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Plan.v, Post Permissions = unique, Pre-Permissions =none
Vertex Name = plan.v, Post Permissions = share, Pre-Permissions =share
Vertex Name = e.planner, Post Permissions = pure, Pre-Permissions =pure
Method Name = Variable
Vertex Name = deltaBlue.Variable.value, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.constraints, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.mark, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.stay, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.name, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Method Name = change
Vertex Name = deltaBlue.DeltaBlue.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.DeltaBlue.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.DeltaBlue.value, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.UnaryConstraint.myOutput, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.satisfied, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Plan.v, Post Permissions = unique, Pre-Permissions =none
Vertex Name = plan.v, Post Permissions = share, Pre-Permissions =share
Vertex Name = e.planner, Post Permissions = pure, Pre-Permissions =pure
Method Name = error
Method Name = inst_main
Vertex Name = db.totalcontrains, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = db.total_ms, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Strength.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.value, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.value, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.constraints, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.mark, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Variable.stay, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.name, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.BinaryConstraint.v1, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.BinaryConstraint.v2, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.BinaryConstraint.direction, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.BinaryConstraint.nodirection, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.determinedBy, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = c.planner, Post Permissions = share, Pre-Permissions =share
Vertex Name = c.mark, Post Permissions = share, Pre-Permissions =share
Vertex Name = planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.required, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.strength, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Planner.stay, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = share, Pre-Permissions =share
Vertex Name = strength.strengthValue, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.weakDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongDefault, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = strength.strongPreferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Strength.normal, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.UnaryConstraint.myOutput, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.UnaryConstraint.satisfied, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.mark, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Plan.v, Post Permissions = unique, Pre-Permissions =none
Vertex Name = plan.v, Post Permissions = share, Pre-Permissions =share
Vertex Name = e.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.Strength.weakest, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Constraint.v1, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Constraint.v2, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Constraint.direction, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.nodirection, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.Constraint.scale, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.Constraint.offset, Post Permissions = share, Pre-Permissions =share
Vertex Name = deltaBlue.DeltaBlue.preferred, Post Permissions = immutable, Pre-Permissions =immutable
Vertex Name = deltaBlue.DeltaBlue.planner, Post Permissions = pure, Pre-Permissions =pure
Vertex Name = deltaBlue.DeltaBlue.value, Post Permissions = share, Pre-Permissions =share
Method Name = main
Vertex Name = deltaBlue.Strength.required, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Strength.strongPreferred, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Strength.preferred, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Strength.strongDefault, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Strength.normal, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Strength.weakDefault, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Strength.weakest, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.backward, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.nodirection, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.forward, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.currentMark, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.DeltaBlue.planner, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.DeltaBlue.planner.planner, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.DeltaBlue.totalcontrains, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.DeltaBlue.total_ms, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.DeltaBlue.planner.currentMark, Post Permissions = unique, Pre-Permissions =none
Vertex Name = db.totalcontrains, Post Permissions = none, Pre-Permissions =none
Vertex Name = db.total_ms, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.Strength.planner, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.Strength.value, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.value, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.constraints, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.mark, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.walkStrength, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.stay, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.name, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Variable.weakest, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.BinaryConstraint.v1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.BinaryConstraint.v2, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.BinaryConstraint.direction, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.BinaryConstraint.nodirection, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.strength, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.UnaryConstraint.planner, Post Permissions = none, Pre-Permissions =none
Vertex Name = c.strength, Post Permissions = none, Pre-Permissions =none
Vertex Name = c.required, Post Permissions = none, Pre-Permissions =none
Vertex Name = c.determinedBy, Post Permissions = none, Pre-Permissions =none
Vertex Name = c.planner, Post Permissions = unique, Pre-Permissions =none
Vertex Name = c.mark, Post Permissions = unique, Pre-Permissions =none
Vertex Name = planner.mark, Post Permissions = none, Pre-Permissions =none
Vertex Name = planner.required, Post Permissions = none, Pre-Permissions =none
Vertex Name = planner.strength, Post Permissions = none, Pre-Permissions =none
Vertex Name = planner.weakest, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.determinedBy, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.walkStrength, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.weakest, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.stay, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.constraints, Post Permissions = unique, Pre-Permissions =none
Vertex Name = strength.strengthValue, Post Permissions = none, Pre-Permissions =none
Vertex Name = strength.weakest, Post Permissions = none, Pre-Permissions =none
Vertex Name = strength.weakDefault, Post Permissions = none, Pre-Permissions =none
Vertex Name = strength.normal, Post Permissions = none, Pre-Permissions =none
Vertex Name = strength.strongDefault, Post Permissions = none, Pre-Permissions =none
Vertex Name = strength.preferred, Post Permissions = none, Pre-Permissions =none
Vertex Name = strength.strongPreferred, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.UnaryConstraint.myOutput, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.UnaryConstraint.satisfied, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Planner.mark, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.Plan.v, Post Permissions = unique, Pre-Permissions =none
Vertex Name = plan.v, Post Permissions = unique, Pre-Permissions =none
Vertex Name = e.planner, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.v1, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.v2, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.direction, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.scale, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.Constraint.offset, Post Permissions = unique, Pre-Permissions =none
Vertex Name = deltaBlue.DeltaBlue.preferred, Post Permissions = none, Pre-Permissions =none
Vertex Name = deltaBlue.DeltaBlue.value, Post Permissions = unique, Pre-Permissions =none

//////////////////////////////////////////////////////////////////////////////////////////////

Class Name = Strength

//////////////////////////////////////////////////////


//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
Class Name = DeltaBlue
Method Name = main
Ref-Var= deltaBlue.Strength.required, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Strength.strongPreferred, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Strength.preferred, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Strength.strongDefault, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Strength.normal, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Strength.weakDefault, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Strength.weakest, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Constraint.backward, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Constraint.nodirection, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Constraint.forward, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Planner.currentMark, Pre-Permissions=none, Post Permissions=unique
Ref-Var= db.totalcontrains, Pre-Permissions=none, Post Permissions=none
Ref-Var= db.total_ms, Pre-Permissions=none, Post Permissions=none
Ref-Var= db.required, Pre-Permissions=none, Post Permissions=none
Ref-Var= db.strongDefault, Pre-Permissions=none, Post Permissions=none
Ref-Var= db.preferred, Pre-Permissions=none, Post Permissions=none
Ref-Var= db.planner, Pre-Permissions=none, Post Permissions=none
Ref-Var= db.value, Pre-Permissions=none, Post Permissions=none
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=none, Post Permissions=unique
Ref-Var= planner.mark, Pre-Permissions=none, Post Permissions=none
Ref-Var= planner.currentMark, Pre-Permissions=none, Post Permissions=unique
Ref-Var= plan.v, Pre-Permissions=none, Post Permissions=unique
Ref-Var= planner.determinedBy, Pre-Permissions=none, Post Permissions=unique
Ref-Var= planner.constraints, Pre-Permissions=none, Post Permissions=unique
Ref-Var= e.planner, Pre-Permissions=none, Post Permissions=none
Ref-Var= planner.required, Pre-Permissions=none, Post Permissions=none
Ref-Var= planner.strength, Pre-Permissions=none, Post Permissions=none
Ref-Var= planner.weakest, Pre-Permissions=none, Post Permissions=none
Ref-Var= planner.walkStrength, Pre-Permissions=none, Post Permissions=none
Ref-Var= planner.stay, Pre-Permissions=none, Post Permissions=none
Ref-Var= c.strength, Pre-Permissions=none, Post Permissions=none
Ref-Var= c.required, Pre-Permissions=none, Post Permissions=none
Ref-Var= c.determinedBy, Pre-Permissions=none, Post Permissions=none
Ref-Var= c.planner, Pre-Permissions=none, Post Permissions=unique
Ref-Var= c.mark, Pre-Permissions=none, Post Permissions=none
Ref-Var= strength.strengthValue, Pre-Permissions=none, Post Permissions=none
Ref-Var= strength.weakest, Pre-Permissions=none, Post Permissions=none
Ref-Var= strength.weakDefault, Pre-Permissions=none, Post Permissions=none
Ref-Var= strength.normal, Pre-Permissions=none, Post Permissions=none
Ref-Var= strength.strongDefault, Pre-Permissions=none, Post Permissions=none
Ref-Var= strength.preferred, Pre-Permissions=none, Post Permissions=none
Ref-Var= strength.strongPreferred, Pre-Permissions=none, Post Permissions=none
Ref-Var= db.normal, Pre-Permissions=none, Post Permissions=none
Ref-Var= deltaBlue.Variable.value, Pre-Permissions=none, Post Permissions=none
Method Name = Strength
Ref-Var= deltaBlue.Strength.strengthValue, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Strength.name, Pre-Permissions=none, Post Permissions=unique
Method Name = print
Ref-Var= deltaBlue.Strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Method Name = stronger
Ref-Var= deltaBlue.Constraint.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Method Name = weaker
Ref-Var= deltaBlue.Strength.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Method Name = weakestOf
Ref-Var= deltaBlue.Constraint.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Strength.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Strength.strengthValue, Pre-Permissions=share, Post Permissions=share
Method Name = strongest
Ref-Var= deltaBlue.Constraint.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Method Name = nextWeaker
Ref-Var= strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.normal, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.preferred, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongPreferred, Pre-Permissions=pure, Post Permissions=pure

//////////////////////////////////////////////////////


//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
Class Name = Variable
Method Name = Variable
Ref-Var= deltaBlue.Variable.value, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Variable.constraints, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Variable.determinedBy, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Variable.walkStrength, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.Variable.stay, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Variable.name, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Variable.weakest, Pre-Permissions=pure, Post Permissions=pure
Method Name = print
Ref-Var= deltaBlue.Variable.name, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.value, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Method Name = addConstraint
Ref-Var= myOutput.constraints, Pre-Permissions=share, Post Permissions=share
Method Name = removeConstraint
Ref-Var= myOutput.constraints, Pre-Permissions=share, Post Permissions=share
Ref-Var= myOutput.determinedBy, Pre-Permissions=none, Post Permissions=unique
Method Name = setValue
Ref-Var= deltaBlue.Variable.value, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.Variable.planner, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= e.satisfied, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.determinedBy, Pre-Permissions=none, Post Permissions=unique
Ref-Var= planner.constraints, Pre-Permissions=share, Post Permissions=share
Ref-Var= e.planner, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.stay, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.normal, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.preferred, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongPreferred, Pre-Permissions=pure, Post Permissions=pure
Method Name = isSatisfied
Ref-Var= e.satisfied, Pre-Permissions=pure, Post Permissions=pure
Method Name = propagateFrom
Ref-Var= planner.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=pure, Post Permissions=pure
Method Name = addConstraintsConsumingTo
Ref-Var= planner.determinedBy, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.constraints, Pre-Permissions=pure, Post Permissions=pure
Method Name = isSatisfied
Method Name = execute
Method Name = output
Method Name = destroyConstraint
Ref-Var= e.planner, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.determinedBy, Pre-Permissions=none, Post Permissions=unique
Ref-Var= planner.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.stay, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.normal, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.preferred, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongPreferred, Pre-Permissions=pure, Post Permissions=pure
Method Name = incrementalRemove
Ref-Var= planner.required, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.determinedBy, Pre-Permissions=none, Post Permissions=unique
Ref-Var= planner.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.stay, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.normal, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.preferred, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongPreferred, Pre-Permissions=pure, Post Permissions=pure
Method Name = markUnsatisfied
Method Name = removeFromGraph
Method Name = removePropagateFrom
Ref-Var= planner.determinedBy, Pre-Permissions=none, Post Permissions=unique
Ref-Var= planner.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.stay, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=share, Post Permissions=share
Method Name = recalculate
Method Name = incrementalAdd
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=pure, Post Permissions=pure
Method Name = newMark
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Method Name = satisfy
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=pure, Post Permissions=pure
Method Name = chooseMethod
Method Name = error
Method Name = markInputs
Method Name = addPropagate
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.required, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.determinedBy, Pre-Permissions=none, Post Permissions=unique
Ref-Var= planner.constraints, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.stay, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.normal, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.preferred, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongPreferred, Pre-Permissions=pure, Post Permissions=pure

//////////////////////////////////////////////////////


//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
Class Name = Constraint
Method Name = Constraint
Ref-Var= deltaBlue.Constraint.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Constraint.strength, Pre-Permissions=share, Post Permissions=share
Method Name = isSatisfied
Method Name = markUnsatisfied
Method Name = isInput
Method Name = addConstraint
Ref-Var= deltaBlue.Constraint.planner, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=pure, Post Permissions=pure
Method Name = addToGraph
Method Name = destroyConstraint
Ref-Var= e.planner, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.determinedBy, Pre-Permissions=none, Post Permissions=unique
Ref-Var= planner.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.stay, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.normal, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.preferred, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongPreferred, Pre-Permissions=pure, Post Permissions=pure
Method Name = removeFromGraph
Method Name = chooseMethod
Method Name = markInputs
Method Name = inputsKnown
Method Name = output
Method Name = satisfy
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=pure, Post Permissions=pure
Method Name = execute
Method Name = recalculate
Method Name = printInputs
Method Name = printOutput
Ref-Var= deltaBlue.Variable.name, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.value, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Method Name = print
Ref-Var= deltaBlue.Variable.name, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.value, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure

//////////////////////////////////////////////////////


//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
Class Name = UnaryConstraint
Method Name = UnaryConstraint
Ref-Var= deltaBlue.UnaryConstraint.myOutput, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.UnaryConstraint.myOutput, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.UnaryConstraint.satisfied, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Constraint.strength, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.Constraint.planner, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=pure, Post Permissions=pure
Method Name = isSatisfied
Ref-Var= e.satisfied, Pre-Permissions=pure, Post Permissions=pure
Method Name = markUnsatisfied
Ref-Var= deltaBlue.UnaryConstraint.satisfied, Pre-Permissions=share, Post Permissions=share
Method Name = output
Ref-Var= deltaBlue.UnaryConstraint.myOutput, Pre-Permissions=pure, Post Permissions=pure
Method Name = addToGraph
Ref-Var= deltaBlue.UnaryConstraint.myOutput, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.UnaryConstraint.satisfied, Pre-Permissions=share, Post Permissions=share
Ref-Var= myOutput.constraints, Pre-Permissions=share, Post Permissions=share
Method Name = removeFromGraph
Ref-Var= deltaBlue.UnaryConstraint.myOutput, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.UnaryConstraint.satisfied, Pre-Permissions=share, Post Permissions=share
Ref-Var= myOutput.constraints, Pre-Permissions=share, Post Permissions=share
Ref-Var= myOutput.determinedBy, Pre-Permissions=none, Post Permissions=unique
Method Name = chooseMethod
Ref-Var= deltaBlue.UnaryConstraint.satisfied, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.UnaryConstraint.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.UnaryConstraint.myOutput, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.UnaryConstraint.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.UnaryConstraint.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Constraint.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Method Name = markInputs
Method Name = inputsKnown
Method Name = recalculate
Ref-Var= deltaBlue.UnaryConstraint.walkStrength, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.UnaryConstraint.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.myOutput, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.Variable.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.stay, Pre-Permissions=share, Post Permissions=share
Method Name = printInputs

//////////////////////////////////////////////////////


//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
Class Name = EditConstraint
Method Name = EditConstraint
Ref-Var= deltaBlue.Variable.preferred, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.preferred, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.UnaryConstraint.myOutput, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.UnaryConstraint.satisfied, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Constraint.strength, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.Constraint.planner, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=pure, Post Permissions=pure
Method Name = isInput
Method Name = execute

//////////////////////////////////////////////////////


//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
Class Name = StayConstraint
Method Name = StayConstraint
Ref-Var= db.strongDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.normal, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.UnaryConstraint.myOutput, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.UnaryConstraint.satisfied, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Constraint.strength, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.Constraint.planner, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=pure, Post Permissions=pure
Method Name = execute

//////////////////////////////////////////////////////


//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
Class Name = BinaryConstraint
Method Name = BinaryConstraint
Ref-Var= deltaBlue.BinaryConstraint.v1, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.v1, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.BinaryConstraint.v2, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.BinaryConstraint.direction, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.BinaryConstraint.nodirection, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= deltaBlue.Constraint.strength, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.Constraint.planner, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=pure, Post Permissions=pure
Method Name = isSatisfied
Ref-Var= deltaBlue.BinaryConstraint.direction, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.nodirection, Pre-Permissions=immutable, Post Permissions=immutable
Method Name = addToGraph
Ref-Var= deltaBlue.BinaryConstraint.v1, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.v2, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.direction, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.BinaryConstraint.nodirection, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= myOutput.constraints, Pre-Permissions=share, Post Permissions=share
Method Name = removeFromGraph
Ref-Var= deltaBlue.BinaryConstraint.v1, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.v2, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.direction, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.BinaryConstraint.nodirection, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= myOutput.constraints, Pre-Permissions=share, Post Permissions=share
Ref-Var= myOutput.determinedBy, Pre-Permissions=none, Post Permissions=unique
Method Name = chooseMethod
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.v1, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.direction, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.Variable.v2, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.forward, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= deltaBlue.Variable.nodirection, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= deltaBlue.Variable.backward, Pre-Permissions=none, Post Permissions=none
Ref-Var= deltaBlue.Constraint.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Strength.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Method Name = markUnsatisfied
Ref-Var= deltaBlue.BinaryConstraint.direction, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.BinaryConstraint.nodirection, Pre-Permissions=immutable, Post Permissions=immutable
Method Name = markInputs
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.BinaryConstraint.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.direction, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.forward, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= deltaBlue.BinaryConstraint.v1, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.v2, Pre-Permissions=pure, Post Permissions=pure
Method Name = input
Ref-Var= deltaBlue.BinaryConstraint.direction, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.forward, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= deltaBlue.BinaryConstraint.v1, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.v2, Pre-Permissions=pure, Post Permissions=pure
Method Name = inputsKnown
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.stay, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.direction, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.forward, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= deltaBlue.BinaryConstraint.v1, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.v2, Pre-Permissions=pure, Post Permissions=pure
Method Name = output
Ref-Var= deltaBlue.BinaryConstraint.direction, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.forward, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= deltaBlue.BinaryConstraint.v2, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.v1, Pre-Permissions=share, Post Permissions=share
Method Name = recalculate
Ref-Var= deltaBlue.Variable.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.stay, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.Variable.stay, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.direction, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.forward, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= deltaBlue.BinaryConstraint.v1, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.BinaryConstraint.v2, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Constraint.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Strength.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Method Name = printInputs
Ref-Var= deltaBlue.Variable.name, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.value, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.direction, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.forward, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= deltaBlue.BinaryConstraint.v1, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.v2, Pre-Permissions=pure, Post Permissions=pure

//////////////////////////////////////////////////////


//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
Class Name = EqualityConstraint
Method Name = EqualityConstraint
Ref-Var= db.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.v1, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.BinaryConstraint.v2, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.BinaryConstraint.direction, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.BinaryConstraint.nodirection, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= deltaBlue.Constraint.strength, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.Constraint.planner, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=pure, Post Permissions=pure
Method Name = execute
Ref-Var= deltaBlue.Variable.value, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.BinaryConstraint.direction, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.forward, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= deltaBlue.BinaryConstraint.v2, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.v1, Pre-Permissions=share, Post Permissions=share

//////////////////////////////////////////////////////


//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
Class Name = ScaleConstraint
Method Name = ScaleConstraint
Ref-Var= db.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Constraint.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Constraint.strength, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.BinaryConstraint.v1, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Constraint.v1, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.BinaryConstraint.v2, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Constraint.v2, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.Constraint.direction, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Constraint.nodirection, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= deltaBlue.ScaleConstraint.scale, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Constraint.scale, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.ScaleConstraint.offset, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Constraint.offset, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.Constraint.planner, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=pure, Post Permissions=pure
Method Name = addToGraph
Ref-Var= deltaBlue.ScaleConstraint.scale, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.ScaleConstraint.offset, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= myOutput.constraints, Pre-Permissions=share, Post Permissions=share
Method Name = removeFromGraph
Ref-Var= deltaBlue.ScaleConstraint.scale, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.ScaleConstraint.offset, Pre-Permissions=share, Post Permissions=share
Ref-Var= myOutput.constraints, Pre-Permissions=share, Post Permissions=share
Ref-Var= myOutput.determinedBy, Pre-Permissions=none, Post Permissions=unique
Method Name = markInputs
Ref-Var= deltaBlue.ScaleConstraint.mark, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.scale, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.offset, Pre-Permissions=pure, Post Permissions=pure
Method Name = execute
Ref-Var= deltaBlue.BinaryConstraint.direction, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.forward, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= deltaBlue.Constraint.value, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.BinaryConstraint.value, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.BinaryConstraint.v2, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.v1, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.BinaryConstraint.scale, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.offset, Pre-Permissions=pure, Post Permissions=pure
Method Name = recalculate
Ref-Var= deltaBlue.Variable.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.stay, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.Variable.stay, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.scale, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.offset, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.direction, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.forward, Pre-Permissions=immutable, Post Permissions=immutable
Ref-Var= deltaBlue.BinaryConstraint.v1, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.BinaryConstraint.v2, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Constraint.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Strength.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Strength.strengthValue, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.Constraint.value, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.BinaryConstraint.value, Pre-Permissions=share, Post Permissions=share
Ref-Var= deltaBlue.BinaryConstraint.scale, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.BinaryConstraint.offset, Pre-Permissions=pure, Post Permissions=pure

//////////////////////////////////////////////////////


//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
Class Name = Plan
Method Name = Plan
Ref-Var= deltaBlue.Plan.v, Pre-Permissions=none, Post Permissions=unique
Method Name = addConstraint
Ref-Var= plan.v, Pre-Permissions=share, Post Permissions=share
Method Name = size
Ref-Var= plan.v, Pre-Permissions=share, Post Permissions=share
Method Name = constraintAt
Ref-Var= plan.v, Pre-Permissions=share, Post Permissions=share
Method Name = execute
Ref-Var= plan.v, Pre-Permissions=share, Post Permissions=share

//////////////////////////////////////////////////////


//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
Class Name = Planner
Method Name = Planner
Ref-Var= deltaBlue.Planner.currentMark, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.DeltaBlue.planner.currentMark, Pre-Permissions=none, Post Permissions=unique
Method Name = newMark
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Method Name = incrementalAdd
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=pure, Post Permissions=pure
Method Name = incrementalRemove
Ref-Var= planner.required, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.determinedBy, Pre-Permissions=none, Post Permissions=unique
Ref-Var= planner.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.stay, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.normal, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.preferred, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongPreferred, Pre-Permissions=pure, Post Permissions=pure
Method Name = addPropagate
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.required, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.determinedBy, Pre-Permissions=none, Post Permissions=unique
Ref-Var= planner.constraints, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.stay, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.normal, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.preferred, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongPreferred, Pre-Permissions=pure, Post Permissions=pure
Method Name = propagateFrom
Ref-Var= planner.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=pure, Post Permissions=pure
Method Name = removePropagateFrom
Ref-Var= planner.determinedBy, Pre-Permissions=none, Post Permissions=unique
Ref-Var= planner.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.stay, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=share, Post Permissions=share
Method Name = extractPlanFromConstraints
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= plan.v, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=pure, Post Permissions=pure
Method Name = makePlan
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= plan.v, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.constraints, Pre-Permissions=pure, Post Permissions=pure
Method Name = addConstraintsConsumingTo
Ref-Var= planner.determinedBy, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.constraints, Pre-Permissions=pure, Post Permissions=pure
Method Name = DeltaBlue
Ref-Var= deltaBlue.DeltaBlue.planner, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.DeltaBlue.planner.planner, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.DeltaBlue.totalcontrains, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.DeltaBlue.total_ms, Pre-Permissions=none, Post Permissions=unique
Method Name = getRunTime
Ref-Var= deltaBlue.DeltaBlue.total_ms, Pre-Permissions=pure, Post Permissions=pure
Method Name = chainTest
Ref-Var= db.totalcontrains, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.strongDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.preferred, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.planner, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.value, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= plan.v, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.determinedBy, Pre-Permissions=none, Post Permissions=unique
Ref-Var= planner.constraints, Pre-Permissions=share, Post Permissions=share
Ref-Var= e.planner, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.stay, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.normal, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.preferred, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongPreferred, Pre-Permissions=pure, Post Permissions=pure
Method Name = projectionTest
Ref-Var= db.totalcontrains, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.normal, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.value, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.value, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.preferred, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.planner, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= plan.v, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.determinedBy, Pre-Permissions=none, Post Permissions=unique
Ref-Var= planner.constraints, Pre-Permissions=share, Post Permissions=share
Ref-Var= e.planner, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.stay, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.normal, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.preferred, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongPreferred, Pre-Permissions=pure, Post Permissions=pure
Method Name = change
Ref-Var= db.preferred, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.planner, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.value, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= plan.v, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.determinedBy, Pre-Permissions=none, Post Permissions=unique
Ref-Var= planner.constraints, Pre-Permissions=share, Post Permissions=share
Ref-Var= e.planner, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.stay, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.normal, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.preferred, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongPreferred, Pre-Permissions=pure, Post Permissions=pure
Method Name = error
Method Name = inst_main
Ref-Var= db.totalcontrains, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.total_ms, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.strongDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.preferred, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.planner, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.value, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.currentMark, Pre-Permissions=share, Post Permissions=share
Ref-Var= plan.v, Pre-Permissions=share, Post Permissions=share
Ref-Var= planner.determinedBy, Pre-Permissions=none, Post Permissions=unique
Ref-Var= planner.constraints, Pre-Permissions=share, Post Permissions=share
Ref-Var= e.planner, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.walkStrength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= planner.stay, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.strength, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.required, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.determinedBy, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= c.planner, Pre-Permissions=share, Post Permissions=share
Ref-Var= c.mark, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strengthValue, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakest, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.weakDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.normal, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongDefault, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.preferred, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= strength.strongPreferred, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= db.normal, Pre-Permissions=pure, Post Permissions=pure
Ref-Var= deltaBlue.Variable.value, Pre-Permissions=pure, Post Permissions=pure
Method Name = main
Ref-Var= deltaBlue.Strength.required, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Strength.strongPreferred, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Strength.preferred, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Strength.strongDefault, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Strength.normal, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Strength.weakDefault, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Strength.weakest, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Constraint.backward, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Constraint.nodirection, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Constraint.forward, Pre-Permissions=none, Post Permissions=unique
Ref-Var= deltaBlue.Planner.currentMark, Pre-Permissions=none, Post Permissions=unique
Ref-Var= db.totalcontrains, Pre-Permissions=none, Post Permissions=none
Ref-Var= db.total_ms, Pre-Permissions=none, Post Permissions=none
Ref-Var= db.required, Pre-Permissions=none, Post Permissions=none
Ref-Var= db.strongDefault, Pre-Permissions=none, Post Permissions=none
Ref-Var= db.preferred, Pre-Permissions=none, Post Permissions=none
Ref-Var= db.planner, Pre-Permissions=none, Post Permissions=none
Ref-Var= db.value, Pre-Permissions=none, Post Permissions=none
Ref-Var= deltaBlue.Variable.mark, Pre-Permissions=none, Post Permissions=unique
Ref-Var= planner.mark, Pre-Permissions=none, Post Permissions=none
Ref-Var= planner.currentMark, Pre-Permissions=none, Post Permissions=unique
Ref-Var= plan.v, Pre-Permissions=none, Post Permissions=unique
Ref-Var= planner.determinedBy, Pre-Permissions=none, Post Permissions=unique
Ref-Var= planner.constraints, Pre-Permissions=none, Post Permissions=unique
Ref-Var= e.planner, Pre-Permissions=none, Post Permissions=none
Ref-Var= planner.required, Pre-Permissions=none, Post Permissions=none
Ref-Var= planner.strength, Pre-Permissions=none, Post Permissions=none
Ref-Var= planner.weakest, Pre-Permissions=none, Post Permissions=none
Ref-Var= planner.walkStrength, Pre-Permissions=none, Post Permissions=none
Ref-Var= planner.stay, Pre-Permissions=none, Post Permissions=none
Ref-Var= c.strength, Pre-Permissions=none, Post Permissions=none
Ref-Var= c.required, Pre-Permissions=none, Post Permissions=none
Ref-Var= c.determinedBy, Pre-Permissions=none, Post Permissions=none
Ref-Var= c.planner, Pre-Permissions=none, Post Permissions=unique
Ref-Var= c.mark, Pre-Permissions=none, Post Permissions=none
Ref-Var= strength.strengthValue, Pre-Permissions=none, Post Permissions=none
Ref-Var= strength.weakest, Pre-Permissions=none, Post Permissions=none
Ref-Var= strength.weakDefault, Pre-Permissions=none, Post Permissions=none
Ref-Var= strength.normal, Pre-Permissions=none, Post Permissions=none
Ref-Var= strength.strongDefault, Pre-Permissions=none, Post Permissions=none
Ref-Var= strength.preferred, Pre-Permissions=none, Post Permissions=none
Ref-Var= strength.strongPreferred, Pre-Permissions=none, Post Permissions=none
Ref-Var= db.normal, Pre-Permissions=none, Post Permissions=none
Ref-Var= deltaBlue.Variable.value, Pre-Permissions=none, Post Permissions=none*/
