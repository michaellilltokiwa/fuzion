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
 * Source of class Excecutor
 *
 *---------------------------------------------------------------------*/

package dev.flang.be.interpreter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import dev.flang.air.Clazz; // NYI: remove dependency! Use dev.flang.fuir instead.
import dev.flang.air.Clazzes; // NYI: remove dependency! Use dev.flang.fuir instead.

import dev.flang.fuir.FUIR;
import dev.flang.fuir.FUIR.ContractKind;
import dev.flang.fuir.analysis.AbstractInterpreter;
import dev.flang.fuir.analysis.AbstractInterpreter.ProcessStatement;

import dev.flang.util.Errors;
import dev.flang.util.FuzionOptions;
import dev.flang.util.List;
import dev.flang.util.Pair;


/**
 * The class Excecutor implements ProcessStatement of the AbstractInterpreter.
 * It does the actual execution of the code.
 */
public class Excecutor extends ProcessStatement<Value, Object>
{


  /*-----------------------------  static fields  -----------------------------*/


  /**
   * Universe instance
   */
  private static final Instance _universe = new Instance(Clazzes.universe.get());


  /**
   * The fuir to be used for executing the code.
   */
  private static FUIR _fuir;


  /**
   * The current options for compiling and running the application.
   */
  private static FuzionOptions _options_;



  /*-----------------------------  instance fields  -----------------------------*/



  /**
   * List that holds the args to be returned by args().
   */
  private final List<Value> _args;


  /**
   * The current instance to be returned by current().
   */
  private final Instance _cur;


  /**
   * The current outer to be returned by outer().
   */
  private final Value _outer;


  /*-----------------------------  constructors  -----------------------------*/


  /**
   * The constructor to initialize the Excecutor
   * at the start of the application.
   */
  public Excecutor(FUIR fuir, FuzionOptions opt)
  {
    _fuir = fuir;
    _options_ = opt;
    this._cur = _fuir.main()._idInFUIR == _fuir.clazzUniverse() ? _universe : new Instance(_fuir.main());
    this._outer = _universe;
    this._args = new List<>();
  }


  /**
   * The constructor to initalize the excecutor
   * with a custom current, outer and args.
   *
   * @param fuir
   * @param cur
   * @param outer
   * @param args
   */
  public Excecutor(Instance cur, Value outer, List<Value> args)
  {
    this._cur = cur;
    this._outer = outer;
    this._args = args;
  }



  /*-----------------------------  methods  -----------------------------*/



  /*
   * For obtaining the current FUIR by
   * accessing the private static field _fuir.
   */
  public FUIR fuir()
  {
    if (PRECONDITIONS) require
    (_fuir != null);

    return _fuir;
  }


  /*
   * For obtaining the current FuzionOptions by
   */
  public FuzionOptions options()
  {
    if (PRECONDITIONS) require
      (_options_ != null);

    return _options_;
  }


  @Override
  public Object sequence(List<Object> l)
  {
    return null;
  }

  @Override
  public Value unitValue()
  {
    return Value.EMPTY_VALUE;
  }

  @Override
  public Object statementHeader(int cl, int c, int i)
  {
    return null;
  }

  @Override
  public Object comment(String s)
  {
    return null;
  }

  @Override
  public Object nop()
  {
    return null;
  }

  @Override
  public Object assignStatic(int cl, boolean pre, int tc, int f, int rt, Value tvalue, Value val)
  {
    if (!(_fuir.clazzIsOuterRef(f) && _fuir.clazzIsUnitType(rt)))
      {
        var fc = _fuir.clazzForInterpreter(f);
        var thiz = fc;
        var staticClazz = _fuir.clazzForInterpreter(tc);
        Interpreter.setField(thiz, staticClazz, tvalue, val);
      }
    return null;
  }

  @Override
  public Object assign(int cl, boolean pre, int c, int i, Value tvalue, Value avalue)
  {
    // NYI: better check clazz containing field is universe
    if (tvalue == unitValue())
      {
        tvalue = _universe;
      }
    if (avalue != unitValue())
      {
        var ttcc = ttcc(cl, c, i, tvalue);
        var tt = ttcc.v0();
        var cc = ttcc.v1();
        var fc = _fuir.clazzForInterpreter(cc);
        Interpreter.setField(fc, _fuir.clazzForInterpreter(tt), tvalue, avalue);
      }
    return null;
  }

  @Override
  public Pair<Value, Object> call(int cl, boolean pre, int c, int i, Value tvalue, List<Value> args)
  {
    var cc0 = _fuir.accessedClazz(cl, c, i);
    var result = new Pair<>(unitValue(), nop());
    if (_fuir.clazzHasSideEffectOrIsNotUnitType(cc0))
      {
        var rt = _fuir.clazzResultClazz(cc0);
        var ttcc = ttcc(cl, c, i, tvalue);
        var tt = ttcc.v0();
        var cc = ttcc.v1();

        // NYI: abstract interpreter should probably not give us boxed values
        // in this case
        if(_fuir.clazzIsBoxed(tt) && !_fuir.clazzIsRef(_fuir.clazzOuterClazz(cc /* NYI should this be cc0? */)))
          {
            tt = ((Boxed)tvalue)._valueClazz._idInFUIR;
            tvalue = ((Boxed)tvalue)._contents;
          }

        result = switch (_fuir.clazzKind(cc))
          {
          case Routine :
            // NYI change call to pass in ai as in match statement?
            var cur = new Instance(_fuir.clazzForInterpreter(cc));

            callOnInstance(cc, cur, tvalue, args, true);
            callOnInstance(cc, cur, tvalue, args, false);

            Value rres = cur;
            var resf = _fuir.clazzResultField(cc);
            if (resf != -1)
              {
                var rfc = _fuir.clazzForInterpreter(resf);
                if (!AbstractInterpreter.clazzHasUnitValue(_fuir, rfc.resultClazz()._idInFUIR))
                  {
                    rres = Interpreter.getField(rfc, _fuir.clazzForInterpreter(cc), cur, false);
                  }
              }
            yield pair(rres);
          case Field :
            var fc = _fuir.clazzForInterpreter(cc);
            var fres = AbstractInterpreter.clazzHasUnitValue(_fuir, rt)
              ? pair(unitValue())
              : pair(Interpreter.getField(fc, _fuir.clazzForInterpreter(tt), tt == _fuir.clazzUniverse() ? _universe : tvalue, false));

            if (CHECKS)
              check(fres != null, AbstractInterpreter.clazzHasUnitValue(_fuir, rt) || fres.v0() != unitValue());

            yield fres;
          case Intrinsic :
            yield _fuir.clazzTypeParameterActualType(cc) != -1  /* type parameter is also of Kind Intrinsic, NYI: CLEANUP: should better have its own kind?  */
              ? pair(unitValue())
              : pair(Intrinsics.call(this, _fuir.clazzForInterpreter(cc)).call(new List<>(tvalue, args)));
          case Abstract:
            throw new Error("Calling abstract not possible: " + _fuir.codeAtAsString(cl, c, i));
          case Native:
            throw new Error("Calling native not yet supported in interpreter.");
          default:
            throw new RuntimeException("NYI");
          };
        }
    return result;
  }


  /**
   * Get the target clazz and the called clazz as a Pair of two Integers.
   *
   * @param tvalue the actual value of the target.
   */
  private Pair<Integer, Integer> ttcc(int cl, int c, int i, Value tvalue)
  {
    var ccs = _fuir.accessedClazzes(cl, c, i);
    var tt = ccs.length != 2 ? -1 : ccs[0];
    var cc = ccs.length != 2 ? -1 : ccs[1];

    if (cc == -1)
      {
        tt = ((ValueWithClazz)tvalue)._clazz._idInFUIR;

        for (var cci = 0; cci < ccs.length && cc==-1; cci += 2)
        {
          if (ccs[cci] == tt)
            {
              cc = ccs[cci+1];
            }
        }
      }

    if (POSTCONDITIONS) ensure
      (cc != -1);

    return new Pair<>(tt, cc);
  }


  /**
   * wrap `v` into a `Pair<>(v, null)`
   *
   * @param v
   * @return
   */
  static Pair<Value, Object> pair(Value v)
  {
    return new Pair<Value,Object>(v, null);
  }


  @Override
  public Pair<Value, Object> box(Value v, int vc, int rc)
  {
    var rcc = _fuir.clazzForInterpreter(rc);
    var vcc = _fuir.clazzForInterpreter(vc);
    return pair(new Boxed(rcc, vcc, v /* .cloneValue(vcc) */));
  }

  @Override
  public Pair<Value, Object> current(int cl, boolean pre)
  {
    return pair(_cur);
  }

  @Override
  public Pair<Value, Object> outer(int cl)
  {
    if (PRECONDITIONS) require
      (_fuir.clazzResultClazz(_fuir.clazzOuterRef(cl)) == _fuir.clazzOuterClazz(cl));

    return pair(_outer);
  }

  @Override
  public Value arg(int cl, int i)
  {
    return _args.get(i);
  }

  @Override
  public Pair<Value, Object> constData(int constCl, byte[] d)
  {
    // NYI: UNDERDEVELOPEMENT: cache?
    var val = switch (_fuir.getSpecialClazz(constCl))
      {
      case c_Const_String, c_String -> Interpreter
        .value(new String(Arrays.copyOfRange(d, 4, ByteBuffer.wrap(d).order(ByteOrder.LITTLE_ENDIAN).getInt() + 4), StandardCharsets.UTF_8));
      case c_bool -> { check(d.length == 1, d[0] == 0 || d[0] == 1); yield new boolValue(d[0] == 1); }
      case c_f32 -> new f32Value(ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN).getFloat());
      case c_f64 -> new f64Value(ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN).getDouble());
      case c_i16 -> new i16Value(ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN).getShort());
      case c_i32 -> new i32Value(ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN).getInt());
      case c_i64 -> new i64Value(ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN).getLong());
      case c_i8  -> new i8Value (ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN).get());
      case c_u16 -> new u16Value(ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN).getChar());
      case c_u32 -> new u32Value(ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN).getInt());
      case c_u64 -> new u64Value(ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN).getLong());
      case c_u8  -> new u8Value (ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN).get() & 0xff);
      default -> {
        if (_fuir.clazzIsArray(constCl))
          {
            var elementType = _fuir.inlineArrayElementClazz(constCl);

            var bb = ByteBuffer.wrap(d).order(ByteOrder.LITTLE_ENDIAN);
            var elCount = bb.getInt();

            var arrayData = ArrayData.alloc(elCount, _fuir, elementType);

            for (int idx = 0; idx < elCount; idx++)
              {
                var b = _fuir.deseralizeConst(elementType, bb);
                var c = constData(elementType, b).v0();
                switch (_fuir.getSpecialClazz(elementType))
                  {
                    case c_i8   : ((byte[])   (arrayData._array))[idx] = (byte)c.i8Value(); break;
                    case c_i16  : ((short[])  (arrayData._array))[idx] = (short)c.i16Value(); break;
                    case c_i32  : ((int[])    (arrayData._array))[idx] = c.i32Value(); break;
                    case c_i64  : ((long[])   (arrayData._array))[idx] = c.i64Value(); break;
                    case c_u8   : ((byte[])   (arrayData._array))[idx] = (byte)c.u8Value(); break;
                    case c_u16  : ((char[])   (arrayData._array))[idx] = (char)c.u16Value(); break;
                    case c_u32  : ((int[])    (arrayData._array))[idx] = c.u32Value(); break;
                    case c_u64  : ((long[])   (arrayData._array))[idx] = c.u64Value(); break;
                    case c_f32  : ((float[])  (arrayData._array))[idx] = c.f32Value(); break;
                    case c_f64  : ((double[]) (arrayData._array))[idx] = c.f64Value(); break;
                    case c_bool : ((boolean[])(arrayData._array))[idx] = c.boolValue(); break;
                    default     : ((Value[])  (arrayData._array))[idx] = c;
                  }
              }

            Instance result = new Instance(_fuir.clazzForInterpreter(constCl));
            var internalArray = _fuir.lookup_array_internal_array(constCl);
            var sysArray = _fuir.clazzResultClazz(internalArray);
            var saCl = _fuir.clazzForInterpreter(sysArray);
            Instance sa = new Instance(saCl);
            Interpreter.setField(_fuir.clazzForInterpreter(_fuir.lookup_fuzion_sys_internal_array_length(sysArray)), saCl,                               sa,     new i32Value(elCount));
            Interpreter.setField(_fuir.clazzForInterpreter(_fuir.lookup_fuzion_sys_internal_array_data(sysArray))  , saCl,                               sa,     arrayData);
            Interpreter.setField(_fuir.clazzForInterpreter(internalArray)                                          , _fuir.clazzForInterpreter(constCl), result, sa);
            yield result;
          }
        else if (!_fuir.clazzIsChoice(constCl))
          {
            var b = ByteBuffer.wrap(d);
            var result = new Instance(_fuir.clazzForInterpreter(constCl));
            for (int index = 0; index < _fuir.clazzArgCount(constCl); index++)
              {
                var fr = _fuir.clazzArgClazz(constCl, index);

                var bytes = _fuir.deseralizeConst(fr, b);
                var c = constData(fr, bytes).v0();
                var acl = _fuir.clazzForInterpreter(_fuir.clazzArg(constCl, index));
                Interpreter.setField(acl, _fuir.clazzForInterpreter(constCl), result, c);
              }

            yield result;
          }
        else
          {
            Errors.error("Unsupported constant.",
                         "Backend cannot handle constant of clazz '" + _fuir.clazzAsString(constCl) + "' ");
            yield null;
          }
      }
      };

    return pair(val);
  }

  @Override
  public Pair<Value, Object> match(AbstractInterpreter<Value, Object> ai, int cl, boolean pre, int c, int i,
    Value subv)
  {
    var staticSubjectClazz = subv instanceof boolValue ? Clazzes.bool.getIfCreated() : ((ValueWithClazz)subv)._clazz;

    if (CHECKS) check
      (staticSubjectClazz.isChoice());

    var tagAndChoiceElement = tagAndVal(staticSubjectClazz, subv);

    var cix = _fuir.matchCaseIndex(cl, c, i, tagAndChoiceElement.v0());

    var field = _fuir.matchCaseField(cl, c, i, cix);
    if (field != -1)
      {
        Interpreter.setField(
            _fuir.clazzForInterpreter(field),
            _cur._clazz,
            _cur,
            tagAndChoiceElement.v1());
      }
    return ai.process(cl, pre, _fuir.matchCaseCode(c, i, cix));
  }


  /**
   * @param staticSubjectClazz the clazz of the subject, a choice
   *
   * @param sub the subjects current value
   *
   * @return pair where first value is the tag, the second value the extracted value.
   */
  private Pair<Integer, Value> tagAndVal(Clazz staticSubjectClazz, Value sub)
  {
    if (PRECONDITIONS) require
      (staticSubjectClazz.isChoice());

    var tag = -1;
    Value val = null;
    if (staticSubjectClazz.isChoiceOfOnlyRefs())
      {
        val = Interpreter.getChoiceRefVal(staticSubjectClazz, staticSubjectClazz, sub);
        tag = ChoiceIdAsRef.tag(staticSubjectClazz, val);
      }
    else if (staticSubjectClazz == Clazzes.bool.getIfCreated())
      {
        tag = sub.boolValue() ? 1 : 0;
        val = sub;
      }
    else
      {
        tag = sub.tag();
      }
    if (val == null)
      {
        val = Interpreter.getChoiceVal(staticSubjectClazz, staticSubjectClazz, sub, tag);
      }

    if (POSTCONDITIONS) ensure
      (tag != -1 && val != null);

    return new Pair<Integer, Value>(tag, val);
  }

  @Override
  public Pair<Value, Object> tag(int cl, Value value, int newcl, int tagNum)
  {
    if (PRECONDITIONS) require
      (_fuir.clazzIsChoice(newcl));

    var tc = _fuir.clazzChoice(newcl, tagNum);

    return pair(Interpreter.tag(_fuir.clazzForInterpreter(newcl), _fuir.clazzForInterpreter(tc), value));
  }

  @Override
  public Pair<Value, Object> env(int ecl)
  {
    var result = FuzionThread.current()._effects.get(_fuir.clazzForInterpreter(ecl));
    if (result == null)
      {
        Errors.fatal("No effect installed: " + _fuir.clazzAsStringNew(ecl));
      }

    if (POSTCONDITIONS) ensure
      (result != unitValue());

    return pair(result);
  }

  @Override
  public Object contract(int cl, ContractKind ck, Value cc)
  {
    if (!cc.boolValue())
      {
        Errors.runTime(pos(),
                       (ck == ContractKind.Pre ? "Precondition" : "Postcondition") + " does not hold",
                       (ck == ContractKind.Pre ? "For" : "After") + " call to " + _fuir.clazzAsStringNew(cl) + "\n" + callStack(fuir()));
      }
    return null;
  }


  /**
   * callOnInstance assigns the arguments to the argument fields of a newly
   * created instance, calls the parents and then this feature.
   *
   * @param cur the newly created instance
   *
   * @param outer the target of the call
   *
   * @param args the arguments to be passed to this call.
   *
   * @param pre
   *
   * @return
   */
  Value callOnInstance(int cc, Instance cur, Value outer, List<Value> args, boolean pre)
  {
    FuzionThread.current()._callStackFrames.push(cc);
    FuzionThread.current()._callStack.push(site());

    new AbstractInterpreter<>(_fuir, new Excecutor(cur, outer, args))
      .process(cc, pre);

    FuzionThread.current()._callStack.pop();
    FuzionThread.current()._callStackFrames.pop();

    return null;
  }


  /**
   * Helper for callStack() to show one single frame
   *
   * @param frame the clazz of the entry to show
   *
   * @param call the call of the entry to show
   */
  private static void showFrame(FUIR fuir, StringBuilder sb, int frame, int callSite)
  {
    if (frame != -1)
      {
        sb.append(_fuir.clazzAsStringNew(frame)).append(": ");
      }
    sb.append(_fuir.siteAsPos(callSite).show()).append("\n");
  }


  /**
   * Helper for callStack() to show a repeated frame
   *
   * @param sb used to append the output
   *
   * @param repeat how often was the previous entry repeated? >= 0 where 0 means
   * it was not repeated, just appeared once, 1 means it was repeated once, so
   * appeared twice, etc.
   *
   * @param frame the clazz of the previous entry
   *
   * @param call the call of the previous entry
   */
  private static void showRepeat(FUIR fuir, StringBuilder sb, int repeat, int frame, int callSite)
  {
    if (repeat > 1)
      {
        sb.append(Errors.repeated(repeat)).append("\n\n");
      }
    else if (repeat > 0)
      {
        showFrame(fuir, sb, frame, callSite);
      }
  }


  /**
   * Current call stack as a string for debugging output.
   */
  public static String callStack(FUIR fuir)
  {
    StringBuilder sb = new StringBuilder("Call stack:\n");
    int lastFrame = -1;
    int lastCall = -1;
    int repeat = 0;
    var s = FuzionThread.current()._callStack;
    var sf = FuzionThread.current()._callStackFrames;
    for (var i = s.size()-1; i >= 0; i--)
      {
        int frame = i<sf.size() ? sf.get(i) : null;
        var call = s.get(i);
        if (frame == lastFrame && call == lastCall)
          {
            repeat++;
          }
        else
          {
            showRepeat(fuir, sb, repeat, lastFrame, lastCall);
            repeat = 0;
            showFrame(fuir, sb, frame, call);
            lastFrame = frame;
            lastCall = call;
          }
      }
    showRepeat(fuir, sb, repeat, lastFrame, lastCall);
    return sb.toString();
  }


}
