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
 * Source of class Constant
 *
 *---------------------------------------------------------------------*/

package dev.flang.ast;

/**
 * AbstractConstant represents a constant in the source code such as '3.14',
 * '"Hello"'.  This class might be loaded from a library or parsed in sources.
 *
 * @author Fridtjof Siebert (siebert@tokiwa.software)
 */
public abstract class AbstractConstant extends Expr
{


  /*--------------------------  constructors  ---------------------------*/


  /**
   * Constructor for a Constant at the given source code position.
   */
  public AbstractConstant()
  {
  }


  /*-----------------------------  methods  -----------------------------*/


  /**
   * Serialized form of the data of this constant.
   */
  public abstract byte[] data();


  /**
   * visit all the expressions within this feature.
   *
   * @param v the visitor instance that defines an action to be performed on
   * visited objects.
   *
   * @param outer the feature surrounding this expression.
   *
   * @return this or an alternative Expr if the action performed during the
   * visit replaces this by the alternative.
   */
  @Override
  public Expr visit(FeatureVisitor v, AbstractFeature outer)
  {
    v.action(this);
    return this;
  }


  /**
   * Is this a compile-time constant?
   */
  @Override
  public boolean isCompileTimeConst()
  {
    return !type().isRef(); // String is ref but NYI
  }


  /**
   * This expression as a compile time constant.
   */
  @Override
  public AbstractConstant asCompileTimeConstant()
  {
    return this;
  }


}

/* end of file */
