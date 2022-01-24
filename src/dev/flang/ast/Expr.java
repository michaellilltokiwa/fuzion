/*

This file is part of the Fuzion language implementation.

The Fuzion language implementation is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published
by the Free Software Foundation, version 3 of the License.

The Fuzion language implementation is distributed in the hope that it will be
useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
License for more details.

You should have received a copy of the GNU General Public License along with The
Fuzion language implementation.  If not, see <https://www.gnu.org/licenses/>.

*/

/*-----------------------------------------------------------------------
 *
 * Tokiwa Software GmbH, Germany
 *
 * Source of class Expr
 *
 *---------------------------------------------------------------------*/

package dev.flang.ast;

import dev.flang.util.ANY;
import dev.flang.util.Errors;
import dev.flang.util.HasSourcePosition;
import dev.flang.util.List;
import dev.flang.util.SourcePosition;


/**
 * Expr <description>
 *
 * @author Fridtjof Siebert (siebert@tokiwa.software)
 */
public abstract class Expr extends ANY implements Stmnt, HasSourcePosition
{

  /*----------------------------  constants  ----------------------------*/


  /**
   * Empty Expr list to be used for empty actual arguments lists.
   */
  public static final List<Expr> NO_EXPRS = new List<Expr>();


  /*-------------------------  static variables -------------------------*/


  /**
   * quick-and-dirty way to make unique names for statement result vars
   */
  static private long _id_ = 0;


  /*----------------------------  variables  ----------------------------*/


  /*--------------------------  constructors  ---------------------------*/


  /**
   * Constructor for an Expression.
   */
  public Expr()
  {
  }

  /*-----------------------------  methods  -----------------------------*/


  /**
   * Mark that this Expr is used as part of a call in the inherits clause of a
   * feature. In the inherits clause i in a feature declaration
   *
   *   g<A,B> {
   *     f<C,D> : i { e; }
   *  }
   *
   * the generics used in i are resolved against f, while the outer class for i
   * is g. In contrast, an expression e outside of an inherits clause, generics
   * are resolved against the outer class f.
   */
  void isInheritsCall()
  {
  }


  /**
   * type returns the type of this expression or Types.t_ERROR if the type is
   * still unknown, i.e., before or during type resolution.
   *
   * @return this Expr's type or t_ERROR in case it is not known yet.
   */
  public AbstractType type()
  {
    var result = typeOrNull();
    if (result == null)
      {
        result = Types.t_ERROR;
        // NYI: This should try to find the reason for the missing type and
        // print the problem
        AstErrors.failedToInferType(this);
      }
    return result;
  }


  /**
   * typeOrNull returns the type of this expression or null if the type is still
   * unknown, i.e., before or during type resolution.
   *
   * @return this Expr's type or null if not known.
   */
  public abstract AbstractType typeOrNull();


  /**
   * The source code position of this expression that produces the result value
   * of this Expression. This is usually equal to this Expression's position,
   * unless we have a block of the form
   *
   *   {
   *     x;
   *     y
   *   }
   *
   * where this is the position of y.
   */
  SourcePosition posOfLast()
  {
    return pos();
  }


  /**
   * Load all features that are called by this expression.
   *
   * @param res this is called during type resolution, res gives the resolution
   * instance.
   *
   * @param outer the class that contains this expression.
   */
  void loadCalledFeature(Resolution res, AbstractFeature outer)
  {
    if (Errors.count() == 0)
      {
        // NYI: is this an error?
        // throw new UnsupportedOperationException(""+getClass()+".loadCalledFeature() at " + pos);
      }
  }


  /**
   * visit all the features, expressions, statements within this feature.
   *
   * @param v the visitor instance that defines an action to be performed on
   * visited objects.
   *
   * @param outer the feature surrounding this expression.
   *
   * @return this or an alternative Expr if the action performed during the
   * visit replaces this by the alternative.
   */
  public abstract Expr visit(FeatureVisitor v, AbstractFeature outer);


  /**
   * Convert this Expression into an assignment to the given field.  In case
   * this is a statment with several branches such as an "if" or a "match"
   * statement, add corresponding assignments in each branch and convert this
   * into a statement that does not produce a value.
   *
   * @param res this is called during type inference, res gives the resolution
   * instance.
   *
   * @param outer the feature that contains this expression
   *
   * @param r the field this should be assigned to.
   *
   * @return the Stmnt this Expr is to be replaced with, typically an Assign
   * that performs the assignment to r.
   */
  Stmnt assignToField(Resolution res, AbstractFeature outer, Feature r)
  {
    return new Assign(res, pos(), r, this, outer);
  }


  /**
   * During type inference: Inform this expression that it is used in an
   * environment that expects the given type.  In particular, if this
   * expression's result is assigned to a field, this will be called with the
   * type of the field.
   *
   * @param res this is called during type inference, res gives the resolution
   * instance.
   *
   * @param outer the feature that contains this expression
   *
   * @param t the expected type.
   *
   * @return either this or a new Expr that replaces thiz and produces the
   * result. In particular, if the result is assigned to a temporary field, this
   * will be replaced by the statement that reads the field.
   */
  public Expr propagateExpectedType(Resolution res, AbstractFeature outer, AbstractType t)
  {
    return this;
  }


  protected Expr addFieldForResult(Resolution res, AbstractFeature outer, AbstractType t)
  {
    var result = this;
    if (t.compareTo(Types.resolved.t_void) != 0)
      {
        var pos = pos();
        Feature r = new Feature(res,
                                pos,
                                Consts.VISIBILITY_INVISIBLE,
                                t,
                                "#stmtResult" + (_id_++),
                                outer);
        r.scheduleForResolution(res);
        res.resolveTypes();
        result = new Block(pos, pos, new List<>(assignToField(res, outer, r),
                                                    new Call(pos, new Current(pos, outer.thisType()), r).resolveTypes(res, outer)));
      }
    return result;
  }


  /**
   * Does this statement consist of nothing but declarations? I.e., it has no
   * code that actually would be executed at runtime.
   */
  public boolean containsOnlyDeclarations()
  {
    return false;
  }


  /**
   * Is this Expr a call to an outer ref?
   */
  public boolean isCallToOuterRef()
  {
    return false;
  }


  /**
   * Check if this value might need boxing, unboxing or tagging and wrap this
   * into Box()/Tag() if this is the case.
   *
   * @param frmlT the formal type this value is assigned to
   *
   * @return this or an instance of Box wrapping this.
   */
  Expr box(AbstractType frmlT)
  {
    if (PRECONDITIONS) require
      (frmlT != null);

    var result = this;
    var t = type();

    if (t.compareTo(Types.resolved.t_void) != 0)
      {
        if ((!t.isRef() || isCallToOuterRef()) &&
            (frmlT.isRef() ||
             (frmlT.isChoice() &&
              !frmlT.isAssignableFrom(t) &&
              frmlT.isAssignableFrom(t.asRef()))) ||
            frmlT.isGenericArgument())
          {
            result = new Box(result, frmlT);
            t = result.type();
          }
        if (frmlT.isChoice() && t.compareTo(frmlT) != 0 && frmlT.isAssignableFrom(t))
          {
            result = new Tag(result, frmlT);
          }
      }
    return result;
  }


  /**
   * Is this a compile-time constant?
   */
  boolean isCompileTimeConst()
  {
    return false;
  }


  /**
   * Get value of bool compile time constant.
   */
  boolean getCompileTimeConstBool()
  {
    if (PRECONDITIONS) require
      (isCompileTimeConst() && type().compareTo(Types.resolved.t_bool) == 0);

    throw new Error();
  }


  /**
   * Get value of i32 compile time constant.
   */
  int getCompileTimeConstI32()
  {
    if (PRECONDITIONS) require
      (isCompileTimeConst() && type().compareTo(Types.resolved.t_i32) == 0);

    throw new Error();
  }


  /**
   * Some Expressions do not produce a result, e.g., a Block that is empty or
   * whose last statement is not an expression that produces a result or an if
   * with one branch not producing a result.
   */
  boolean producesResult()
  {
    return true;
  }


}

/* end of file */
