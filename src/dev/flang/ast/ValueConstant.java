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
 * Source of class ValueConstant
 *
 *---------------------------------------------------------------------*/

package dev.flang.ast;

import java.io.ByteArrayOutputStream;

import dev.flang.util.List;
import dev.flang.util.SourcePosition;


/**
 * ValueConstant.
 */
public class ValueConstant extends Constant
{

  private final AbstractType _type;
  private final byte[] data;

  /*----------------------------  variables  ----------------------------*/


  /*--------------------------  constructors  ---------------------------*/


  /**
   * Constructor for a ValueConstant at the given source code position.
   *
   * @param pos the sourcecode position, used for error messages.
   */
  public ValueConstant(SourcePosition pos, List<AbstractConstant> ac, AbstractType t)
  {
    super(pos);

    if (PRECONDITIONS) require
      (!t.isGenericArgument(),
       !t.isThisType(),
       !t.isChoice(),
       !t.isRef());


    this._type = t;

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (AbstractConstant c : ac) {
      out.write(c.data(), 0, c.data().length);
    }
    this.data = out.toByteArray();
  }


  /*-----------------------------  methods  -----------------------------*/


  @Override
  public byte[] data()
  {
    return data;
  }


  public AbstractType typeIfKnown()
  {
    return removeThisTypes(_type);
  }


  private AbstractType removeThisTypes(AbstractType t)
  {
    return t == null
      ? null
      : t.applyToGenericsAndOuter(x -> { return new ResolvedNormalType(x.generics(), x.unresolvedGenerics(), removeThisTypes(x.outer()), x.featureOfType(), UnresolvedType.RefOrVal.Value); });
  }


  @Override
  public Expr visit(FeatureVisitor v, AbstractFeature outer)
  {
    return this;
  }

}

/* end of file */
