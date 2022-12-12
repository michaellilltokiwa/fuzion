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
 * Source of class IncompatibleResultTypeError
 *
 *---------------------------------------------------------------------*/

package dev.flang.ast;

import java.util.Map;
import java.util.stream.Collectors;

import dev.flang.util.ANY;
import dev.flang.util.List;
import dev.flang.util.SourcePosition;


/**
 * IncompatibleResultsOnBranches creates informative error messages for branching
 * statements like "if" or "match" in case the different branches produce
 * incompatible results.
 *
 * @author Fridtjof Siebert (siebert@tokiwa.software)
 */
public class IncompatibleResultsOnBranches extends ANY
{


  /*----------------------------  variables  ----------------------------*/

  /**
   * The different types in source code order.
   */
  private List<AbstractType> _types;

  /**
   * For each type, a list of expressions from different branches that produce
   * this type.
   */
  private Map<AbstractType, List<SourcePosition>> _positions;


  /*--------------------------  constructors  ---------------------------*/


  /**
   * Constructor to create error message for incompatible types in branches.
   *
   * @param pos the source position
   *
   * @param msg the main, one line error message
   *
   * @param it an iterator over the expressions that produce the results in the
   * different branches.  Should be in source-code order.
   */
  public IncompatibleResultsOnBranches(SourcePosition pos,
                                       String msg,
                                       List<Expr> expr)
  {
    _types = expr
      .stream()
      .map(b -> b.type())
      .collect(List.collector());
    if (CHECKS) check
      (_types.size() > 1);

    _positions =  expr.stream().collect(Collectors.groupingBy(
      Expr::type,
      Collectors.mapping(Expr::posOfLast, List.collector())));

    AstErrors.incompatibleResultsOnBranches(pos, msg, _types, _positions);
  }


}

/* end of file */
