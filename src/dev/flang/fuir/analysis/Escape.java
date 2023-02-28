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
 * Source of class Escape
 *
 *---------------------------------------------------------------------*/

package dev.flang.fuir.analysis;

import java.util.Stack;
import java.util.TreeMap;

import dev.flang.fuir.FUIR;

import dev.flang.ir.IR;

import dev.flang.util.ANY;
import dev.flang.util.Errors;


/**
 * Escape performs escape analysis of instances allocated within
 * clazzes or passes as arguments in a call to a clazz.
 *
 * @author Fridtjof Siebert (siebert@tokiwa.software)
 */
public class Escape extends ANY
{


  /*-----------------------------  classes  -----------------------------*/


  /*----------------------------  constants  ----------------------------*/


  /**
   * property-controlled flag to enable debug output.
   *
   * To enable debugging, use fz with
   *
   *   FUZION_JAVA_OPTIONS=-Ddev.flang.fuir.analysis.Escape.DEBUG=true
   */
  static final boolean DEBUG =
    System.getProperty("dev.flang.fuir.analysis.Escape.DEBUG",
                       "false").equals("true");



  /*----------------------------  variables  ----------------------------*/


  /**
   * The intermediate code we are analyzing.
   */
  public final FUIR _fuir;


  /**
   * Cached results of doesCurEscape() for non-constructors.
   */
  private final TreeMap<Integer, Boolean> _doesEscapeCache = new TreeMap<>();


  /*---------------------------  constructors  ---------------------------*/


  /**
   * Create Escape for given intermediate code.
   *
   * @param fuir the intermediate code.
   */
  public Escape(FUIR fuir)
  {
    _fuir = fuir;
    Errors.showAndExit();
  }


  /*-----------------------------  methods  -----------------------------*/



  /**
   * Check if on a call to clazz 'cl', the current instance ('cl.this') escapes
   * and lives longer than this call.
   *
   * @param cl a Routine
   */
  public boolean doesCurEscape(int cl)
  {
    if (PRECONDITIONS) require
      ( _fuir.clazzKind(cl) == IR.FeatureKind.Routine
     || _fuir.clazzKind(cl) == IR.FeatureKind.Intrinsic);

    return _fuir.clazzResultField(cl) == -1 // a constructor, i.e., current instance is returned!
      || doesCurEscapeCached(cl);
  }


  /**
   * For non-constructor routine cl, perform escape analysis and cache the
   * result.
   *
   * @param cl a non-constructor routine.
   */
  private boolean doesCurEscapeCached(int cl)
  {
    if (PRECONDITIONS) require
      (_fuir.clazzKind(cl) == IR.FeatureKind.Routine,
       _fuir.clazzResultField(cl) != -1);

    var result = _doesEscapeCache.get(cl);
    if (result == null)
      {
        result = doesCurEscape(cl, new Stack<Boolean>(), _fuir.clazzCode(cl));
        _doesEscapeCache.put(cl, result);
      }
    return result;
  }


  /**
   * Perform escape analysis for current instance in given code block.
   *
   * @param cl a Routine
   *
   * @param stack an originally empty stack that contains true for all current
   * instances on the stack, false for any other instance.
   *
   * @param c the code block to analyze.
   *
   * @return true iff the current instance cannot be proven not to escape, i.e.,
   * we have to assume that it may escape.
   */
  private boolean doesCurEscape(int cl, Stack<Boolean> stack, int c)
  {
    var gotVoid = false;
    for (int i = 0; !gotVoid && _fuir.withinCode(c, i); i = i + _fuir.codeSizeAt(c, i))
      {
        var s = _fuir.codeAt(c, i);
        if (DEBUG)
          {
            System.out.println("ESCAPE: process "+_fuir.clazzAsString(cl)+"."+c+"."+i+":\t"+_fuir.codeAtAsString(cl, c, i)+" stack is "+stack);
          }
        switch (s)
          {
          case AdrOf:
            {
              stack.push(stack.pop());
              break;
            }
          case Assign:
            {
              if (_fuir.accessedClazz(cl, c, i) != -1)  // field we are assigning to may be unused, i.e., -1
                {
                  var tc = _fuir.accessTargetClazz(cl, c, i);
                  if (_fuir.hasData(tc))
                    {
                      var target = stack.pop();
                    }
                  var cc0 = _fuir.accessedClazz  (cl, c, i);
                  var rt = _fuir.clazzResultClazz(cc0);
                  if (_fuir.hasData(rt))
                    {
                      var value  = stack.pop();
                      if (value)
                        { // if value is current and stored in a field, we consider
                          // it has escaped since we do not follow that field:
                          return true;
                        }
                    }
                }
              break;
            }
          case Box:
            {
              var vc = _fuir.boxValueClazz(cl, c, i);
              var rc = _fuir.boxResultClazz(cl, c, i);
              if (_fuir.hasData(vc))
                {
                  stack.pop();
                }
              if (_fuir.hasData(rc))
                {
                  stack.push(false);
                }
              break;
            }
          case Unbox:
            {
              var refc = _fuir.unboxOuterRefClazz(cl, c, i);
              var resc = _fuir.unboxResultClazz(cl, c, i);
              var v = false;
              if (_fuir.hasData(refc))
                {
                  v = stack.pop();
                }
              if (_fuir.hasData(resc))
                {
                  stack.push(v);
                }
              break;
            }
          case Call:
            {
              var tc = _fuir.accessTargetClazz(cl, c, i);
              var cc0 = _fuir.accessedClazz  (cl, c, i);
              var rt = _fuir.clazzResultClazz(cc0);
              // NYI: for dynamic all, check all called methods!
              if (_fuir.hasPrecondition(cc0))
                {
                  // NYI: check if current escapes via pre-condition
                  //
                  // ol.add(call(tc, cc0, (Stack<CExpr>) stack.clone(), true, cl, c, i));
                }
              if (!_fuir.callPreconditionOnly(cl, c, i))
                {
                  //                  result = call(tc, cc0, stack, false, cl, c, i);
                  var argCount = _fuir.clazzArgCount(cc0);
                  while (argCount > 0)
                    {
                      var ac = _fuir.clazzArgClazz(cc0, argCount-1);
                      if (_fuir.hasData(ac))
                        {
                          if (stack.pop())
                            {
                              // NYI: better check if this argument escapes from
                              // any of the actual called clazzes when passed as
                              // an arg.
                              return true;
                            }
                        }
                      argCount = argCount - 1;
                    }
                  if (tc != -1)
                    {
                      var or = _fuir.clazzOuterRef(cc0);
                      if (_fuir.hasData(tc))
                        {
                          if (stack.pop() && _fuir.clazzKind(cc0) != IR.FeatureKind.Field)
                            {
                              // NYI: better check if target escapes from any of
                              // the actual called clazzes when passed as target.
                              return true;
                            }
                        }
                    }
                  gotVoid = _fuir.clazzIsVoidType(rt);
                  if (_fuir.hasData(rt))
                    {
                      stack.push(false);
                    }
                }
              break;
            }
          case Comment:
            {
              break;
            }
          case Current:
            {
              stack.push(true);
              break;
            }
          case Const:
            {
              var constCl = _fuir.constClazz(c, i);
              if (_fuir.hasData(constCl))
                {
                  stack.push(false);
                }
              break;
            }
          case Match:
            {
              for (var mc = 0; mc < _fuir.matchCaseCount(c, i); mc++)
                {
                  if (doesCurEscape(cl, (Stack<Boolean>) stack.clone(), _fuir.matchCaseCode(c, i, mc)))
                    {
                      return true;
                    }
                }
              break;
            }
          case Tag:
            {
              var vc = _fuir.tagValueClazz(cl, c, i);  // static clazz of value
              var nc = _fuir.tagNewClazz  (cl, c, i);  // static clazz of result
              var isCurrent = false;
              if (_fuir.hasData(vc))
                {
                  isCurrent = stack.pop();
                }
              if (_fuir.hasData(nc))
                {
                  stack.push(isCurrent);
                }
              break;
            }
          case Env:
            {
              stack.push(false);
              break;
            }
          case Dup:
            {
              var v = stack.pop();
              stack.push(v);
              stack.push(v);
              break;
            }
          case Pop:
            { // Handled within Call
              break;
            }
          default:
            {
              Errors.fatal("C backend does not handle statements of type " + s);
            }
          }
      }
    return false;
  }


}

/* end of file */
