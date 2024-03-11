package dev.flang.be.interpreter2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import dev.flang.air.FeatureAndActuals;
import dev.flang.ast.AbstractCall;
import dev.flang.ast.Types;
import dev.flang.be.interpreter.Boxed;
import dev.flang.be.interpreter.DynamicBinding;
import dev.flang.be.interpreter.FuzionThread;
import dev.flang.be.interpreter.Instance;
import dev.flang.be.interpreter.Interpreter;
import dev.flang.be.interpreter.Intrinsics;
import dev.flang.be.interpreter.LValue;
import dev.flang.be.interpreter.Layout;
import dev.flang.be.interpreter.Value;
import dev.flang.be.interpreter.ValueWithClazz;
import dev.flang.be.interpreter.boolValue;
import dev.flang.be.interpreter.f32Value;
import dev.flang.be.interpreter.f64Value;
import dev.flang.be.interpreter.i16Value;
import dev.flang.be.interpreter.i32Value;
import dev.flang.be.interpreter.i64Value;
import dev.flang.be.interpreter.i8Value;
import dev.flang.be.interpreter.u16Value;
import dev.flang.be.interpreter.u32Value;
import dev.flang.be.interpreter.u64Value;
import dev.flang.be.interpreter.u8Value;
import dev.flang.fuir.FUIR;
import dev.flang.fuir.FUIR.ContractKind;
import dev.flang.fuir.analysis.AbstractInterpreter;
import dev.flang.fuir.analysis.AbstractInterpreter.ProcessStatement;
import dev.flang.util.Errors;
import dev.flang.util.List;
import dev.flang.util.Pair;

public class Processor extends ProcessStatement<Value, SideEffect>
{

  private final FUIR _fuir;
  private List<Value> curArgs;
  private Instance cur = Instance.universe;
  private Value outer = Instance.universe;

  public Processor(FUIR fuir)
  {
    this._fuir = fuir;
  }

  @Override
  public SideEffect sequence(List<SideEffect> l)
  {
    return SideEffect.NONE;
  }

  @Override
  public Value unitValue()
  {
    return Value.EMPTY_VALUE;
  }

  @Override
  public SideEffect statementHeader(int cl, int c, int i)
  {
    return SideEffect.NONE;
  }

  @Override
  public SideEffect comment(String s)
  {
    return SideEffect.NONE;
  }

  @Override
  public SideEffect nop()
  {
    return SideEffect.NONE;
  }

  @Override
  public Pair<Value, SideEffect> adrOf(Value v)
  {
    return value(v);
  }

  @Override
  public SideEffect assignStatic(int cl, boolean pre, int tc, int f, int rt, Value tvalue, Value val)
  {
    if (val != unitValue())
      {
        Interpreter.setField(_fuir.clazz(f).feature(), -1, _fuir.clazz(tc), tvalue, val);
      }
    return SideEffect.NONE;
  }

  @Override
  public SideEffect assign(int cl, boolean pre, int c, int i, Value tvalue, Value avalue)
  {
    if (avalue != unitValue())
      {
        var ccs = _fuir.accessedClazzes(cl, c, i);
        var tt = ccs[0];                   // target clazz we match against
        var cc = ccs[1];                   // called clazz in case of match

        Interpreter.setField(_fuir.clazz(cc).feature(), -1, _fuir.clazz(tt), tvalue, avalue);
      }
    return SideEffect.NONE;
  }

  @Override
  public Pair<Value, SideEffect> call(int cl, boolean pre, int c, int i, Value tvalue, List<Value> args)
  {
    curArgs = args;

    var ccs = _fuir.accessedClazzes(cl, c, i);
    var tt = -1;
    var cc = _fuir.accessedClazz(cl, c, i);
    for (var cci = 0; cci < ccs.length && tt==-1; cci += 2)
      {
        if (ccs[cci+1] == cc)
          {
            tt = ccs[cci];
          }
      }
    if (tt == -1)
      {
        var lv = ((LValue)tvalue);
        cc = ((ValueWithClazz)lv.container.refs[lv.offset])._clazz._idInFUIR;
        var db = (DynamicBinding) ((ValueWithClazz)tvalue)._clazz.resultClazz()._dynamicBinding;

        var calledFeature = _fuir.clazz(cc).feature();
        System.out.println(calledFeature);
        var f = new FeatureAndActuals(calledFeature, AbstractCall.NO_GENERICS /*staticClazz.actualGenerics(c.actualTypeParameters())*/, false);
        var outer = db.inner(f);
        var inner = db.inner(f);
        System.out.println(outer);
        System.out.println(inner);
      }

    switch (_fuir.clazzKind(cc))
      {
      case Routine :
        var oldCur = cur;
        var oldOuter = outer;
        cur = new Instance(_fuir.clazz(cc));
        outer = tvalue;
        var result = Interpreter2._ai.process(cc, pre);
        cur = oldCur;
        outer = oldOuter;
        return result;
      case Field :
        return tvalue == unitValue()
          ? value(unitValue())
          : value(tvalue.at(_fuir.clazz(cc), Layout.get(_fuir.clazz(tt)).offset(_fuir.clazz(cc))));
      case Intrinsic :
        if (_fuir.clazzTypeParameterActualType(cc) != -1)  /* type parameter is also of Kind Intrinsic, NYI: CLEANUP: should better have its own kind?  */
          {
            return value(unitValue());
          }
        else
          {
            return value(Intrinsics.call(null, _fuir.clazz(cc)).call(new List<>(tvalue, args)));
          }
      case Abstract:
        throw new Error("Calling abstract not possible: " + _fuir.codeAtAsString(cl, c, i));
      case Native:
        throw new Error("Calling native not supported in interpreter.");
      default:
        throw new RuntimeException("NYI");
      }
  }

  Pair<Value, SideEffect> value(Value v)
  {
    return new Pair<Value,SideEffect>(v, SideEffect.NONE);
  }

  @Override
  public Pair<Value, SideEffect> box(Value v, int vc, int rc)
  {
    var rcc = _fuir.clazz(rc);
    var vcc = _fuir.clazz(vc);
    return value(new Boxed(rcc, vcc, v /* .cloneValue(vcc) */));
  }

  @Override
  public Pair<Value, SideEffect> current(int cl, boolean pre)
  {
    return value(cur);
  }

  @Override
  public Pair<Value, SideEffect> outer(int cl)
  {
    if (PRECONDITIONS) require
      (_fuir.clazzResultClazz(_fuir.clazzOuterRef(cl)) == _fuir.clazzOuterClazz(cl));

    return value(outer);
  }

  @Override
  public Value arg(int cl, int i)
  {
    return curArgs.get(i);
  }

  @Override
  public Pair<Value, SideEffect> constData(int constCl, byte[] d)
  {
    // NYI cache?
    var val = switch (_fuir.getSpecialClazz(constCl))
      {
      case c_Const_String, c_String -> Interpreter
        .value(new String(Arrays.copyOfRange(d, 4, ByteBuffer.wrap(d).getInt() + 4), StandardCharsets.UTF_8));
      case c_bool -> new boolValue(d[0] != 0);
      case c_f32 -> new f32Value(ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN).getFloat());
      case c_f64 -> new f64Value(ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN).getDouble());
      case c_i16 -> new i16Value(ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN).getShort());
      case c_i32 -> new i32Value(ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN).getInt());
      case c_i64 -> new i64Value(ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN).getLong());
      case c_i8 -> new i8Value(ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN).get());
      case c_u16 -> new u16Value(ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN).getChar());
      case c_u32 -> new u32Value(ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN).getInt());
      case c_u64 -> new u64Value(ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN).getLong());
      case c_u8 -> new u8Value(ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN).get() & 0xff);
      default -> {
        if (_fuir.clazzIsArray(constCl))
          {
            var elementType = this._fuir.inlineArrayElementClazz(constCl);

            var bb = ByteBuffer.wrap(d);
            var elCount = bb.getInt();

            var result = Intrinsics.fuzionSysArrayAlloc(elCount, _fuir.clazz(constCl));

            for (int idx = 0; idx < elCount; idx++)
              {
                var b = _fuir.deseralizeConst(elementType, bb);
                var c = constData(elementType, b)._v0;
                if      (_fuir.clazz(elementType)._type.compareTo(Types.resolved.t_i8  ) == 0) { ((byte[])   (result._array))[idx] = (byte)c.i8Value(); }
                else if (_fuir.clazz(elementType)._type.compareTo(Types.resolved.t_i16 ) == 0) { ((short[])  (result._array))[idx] = (short)c.i16Value(); }
                else if (_fuir.clazz(elementType)._type.compareTo(Types.resolved.t_i32 ) == 0) { ((int[])    (result._array))[idx] = c.i32Value(); }
                else if (_fuir.clazz(elementType)._type.compareTo(Types.resolved.t_i64 ) == 0) { ((long[])   (result._array))[idx] = c.i64Value(); }
                else if (_fuir.clazz(elementType)._type.compareTo(Types.resolved.t_u8  ) == 0) { ((byte[])   (result._array))[idx] = (byte)c.u8Value(); }
                else if (_fuir.clazz(elementType)._type.compareTo(Types.resolved.t_u16 ) == 0) { ((char[])   (result._array))[idx] = (char)c.u16Value(); }
                else if (_fuir.clazz(elementType)._type.compareTo(Types.resolved.t_u32 ) == 0) { ((int[])    (result._array))[idx] = c.u32Value(); }
                else if (_fuir.clazz(elementType)._type.compareTo(Types.resolved.t_u64 ) == 0) { ((long[])   (result._array))[idx] = c.u64Value(); }
                else if (_fuir.clazz(elementType)._type.compareTo(Types.resolved.t_bool) == 0) { ((boolean[])(result._array))[idx] = c.boolValue(); }
                else                                                                           { ((Value[])  (result._array))[idx] = c; }
              }
            yield result;
          }
        else if (!_fuir.clazzIsChoice(constCl))
          {
            var b = ByteBuffer.wrap(d);
            var result = new Instance(_fuir.clazz(constCl));
            for (int index = 0; index < _fuir.clazzArgCount(constCl); index++)
              {
                var fr = _fuir.clazzArgClazz(constCl, index);

                var bytes = _fuir.deseralizeConst(fr, b);
                var c = constData(fr, bytes)._v0;
                Interpreter.setField(_fuir.clazz(_fuir.clazzArg(constCl, index)).feature(), -1, _fuir.clazz(constCl), result, c);
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

    return value(val);
  }

  @Override
  public Pair<Value, SideEffect> match(AbstractInterpreter<Value, SideEffect> ai, int cl, boolean pre, int c, int i,
    Value subv)
  {
    return ai.process(cl, pre, _fuir.matchCaseCode(c, i, ((LValue)subv).container.nonrefs[0]));
  }

  @Override
  public Pair<Value, SideEffect> tag(int cl, Value value, int newcl, int tagNum)
  {
    if (PRECONDITIONS) require
      (_fuir.clazzIsChoice(newcl));

    var tc = _fuir.clazzChoice(newcl, tagNum);
    return value(Interpreter.tag(_fuir.clazz(newcl), _fuir.clazz(tc), value));
  }

  @Override
  public Pair<Value, SideEffect> env(int ecl)
  {
    var result = FuzionThread.current()._effects.get(_fuir.clazz(ecl));
    check (result != null);
    return value(result);
  }

  @Override
  public SideEffect contract(int cl, ContractKind ck, Value cc)
  {
    if (!cc.boolValue())
      {
        Errors.fatal("CONTRACT FAILED: " + ck + " on call to '" + _fuir.clazzAsString(cl) + "'");
      }
    return SideEffect.NONE;
  }

}
