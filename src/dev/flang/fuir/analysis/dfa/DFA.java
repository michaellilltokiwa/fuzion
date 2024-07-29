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
 * Source of class DFA
 *
 *---------------------------------------------------------------------*/

package dev.flang.fuir.analysis.dfa;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.function.Supplier;

import dev.flang.fuir.FUIR;
import dev.flang.fuir.FUIR.SpecialClazzes;
import dev.flang.fuir.analysis.AbstractInterpreter;

import static dev.flang.ir.IR.NO_SITE;

import dev.flang.util.ANY;
import dev.flang.util.Errors;
import dev.flang.util.FuzionConstants;

import static dev.flang.util.FuzionConstants.EFFECT_ABORTABLE_NAME;
import dev.flang.util.FuzionOptions;
import dev.flang.util.List;
import dev.flang.util.Pair;
import dev.flang.util.SourcePosition;


/**
 * DFA creates a data flow analysis based on the FUIR representation of a Fuzion
 * application.
 *
 * @author Fridtjof Siebert (siebert@tokiwa.software)
 */
public class DFA extends ANY
{


  /*----------------------------  interfaces  ---------------------------*/


  /**
   * Functional interface to create intrinsics.
   */
  @FunctionalInterface
  interface IntrinsicDFA
  {
    Val analyze(Call c);
  }


  /*-----------------------------  classes  -----------------------------*/


  /**
   * Dummy unit type as type parameter for AbstractInterpreter.ProcessExpression.
   */
  static class Unit
  {
  }


  /**
   * Statement processor used with AbstractInterpreter to perform DFA analysis
   */
  class Analyze extends AbstractInterpreter.ProcessExpression<Val,Unit>
  {


    /**
     * The Call we are analyzing.
     */
    final Call _call;


    /**
     * Create processor for an abstract interpreter doing DFA analysis of the
     * given call's code.
     */
    Analyze(Call call)
    {
      _call = call;
    }



    /**
     * Join a List of RESULT from subsequent statements into a compound
     * statement.  For a code generator, this could, e.g., join statements "a :=
     * 3;" and "b(x);" into a block "{ a := 3; b(x); }".
     */
    @Override
    public Unit sequence(List<Unit> l)
    {
      return _unit_;
    }


    /*
     * Produce the unit type value.  This is used as a placeholder
     * for the universe instance as well as for the instance 'unit'.
     */
    @Override
    public Val unitValue()
    {
      return Value.UNIT;
    }


    /**
     * Called before each statement is processed. May be used to, e.g., produce
     * tracing code for debugging or a comment.
     */
    @Override
    public Unit expressionHeader(int s)
    {
      if (_reportResults && _options.verbose(9))
        {
          say("DFA for "+_fuir.siteAsString(s)+" at "+s+": "+_fuir.codeAtAsString(s));
        }
      return _unit_;
    }


    /**
     * A comment, adds human readable information
     */
    @Override
    public Unit comment(String s)
    {
      return _unit_;
    }


    /**
     * no operation, like comment, but without giving any comment.
     */
    @Override
    public Unit nop()
    {
      return _unit_;
    }


    /**
     * Perform an assignment val to field f in instance rt
     *
     * @param s site of the expression causing this assignment
     *
     * @param tc clazz id of the target instance
     *
     * @param f clazz id of the assigned field
     *
     * @param rt clazz is of the field type
     *
     * @param tvalue the target instance
     *
     * @param val the new value to be assigned to the field.
     *
     * @return resulting code of this assignment.
     */
    @Override
    public Unit assignStatic(int s, int tc, int f, int rt, Val tvalue, Val val)
    {
      tvalue.value().setField(DFA.this, f, val.value());
      return _unit_;
    }


    /**
     * Perform an assignment of avalue to a field in tvalue. The type of tvalue
     * might be dynamic (a reference). See FUIR.access*().
     *
     * @param s site of assignment
     *
     * @param tvalue the target instance
     *
     * @param avalue the new value to be assigned to the field.
     */
    @Override
    public Unit assign(int s, Val tvalue, Val avalue)
    {
      var res = access(s, tvalue, new List<>(avalue));
      return _unit_;
    }


    /**
     * In an access, check if the target of the access is a boxed value. If so,
     * unbox it.
     *
     * @param tvalue the target value of an access
     *
     * @param tt the type of the target value
     *
     * @param cc the called clazz
     *
     * @return tvalue in case tvalue does not need unboxing, or the unboxed
     * value if tt is boxed and the outer clazz of cc is a value type.
     */
    private Val unboxTarget(Val tvalue, int tt, int cc)
    {
      var cco = _fuir.clazzOuterClazz(cc);
      return _fuir.clazzIsRef(tt) && !_fuir.clazzIsRef(cco) ? tvalue.value().unbox(cco)
                                                            : tvalue;
    }


    /**
     * Perform a call of a feature with target instance tvalue with given
     * arguments.  The type of tvalue might be dynamic (a reference). See
     * FUIR.access*().
     *
     * Result.v0() may be null to indicate that code generation should stop here
     * (due to an error or tail recursion optimization).
     */
    @Override
    public Pair<Val, Unit> call(int s, Val tvalue, List<Val> args)
    {
      var res = access(s, tvalue, args);
      DFA.this.site(s).recordResult(res == null);
      return new Pair<>(res, _unit_);
    }


    /**
     * Analyze an access (call or write) of a feature.
     *
     * @param s site of access, must be ExprKind.Assign or ExprKind.Call
     *
     * @param tvalue the target of this call, Value.UNIT if none.
     *
     * @param args the arguments of this call, or, in case of an assignment, a
     * list of one element containing value to be assigned.
     *
     * @return result value of the access
     */
    Val access(int s, Val tvalue, List<Val> args)
    {
      var tc = _fuir.accessTargetClazz(s);
      var cc0 = _fuir.accessedClazz  (s);
      var ccs = _fuir.accessedClazzes(s);
      var resf = new Val[] { null };
      tvalue.value().forAll(t ->
        {
          if (CHECKS) check
            (t != Value.UNIT || AbstractInterpreter.clazzHasUnitValue(_fuir, tc));
          var t_cl = t == Value.UNIT ? tc : t._clazz;
          var found = false;
          for (var cci = 0; cci < ccs.length; cci += 2)
            {
              var tt = ccs[cci  ];
              var cc = ccs[cci+1];
              if (t_cl == tt ||
                  t != Value.UNDEFINED && _fuir.clazzAsValue(t._clazz) == tt)
                {
                  found = true;
                  var r = access0(s, t, args, cc, tvalue);
                  if (r != null)
                    {
                      resf[0] = resf[0] == null ? r : resf[0].joinVal(DFA.this, r);
                    }
                }
            }
          if (!found)
            {
              var instantiatedAt = _calls.keySet().stream()
                .filter(c -> c._cc == t_cl && c._site != NO_SITE)
                .map(c -> c._site)
                .findAny()
                .orElse(NO_SITE);
              _fuir.recordAbstractMissing(t_cl, cc0, instantiatedAt);
            }
        });
      var res = resf[0];
      if (res != null &&
          tvalue instanceof EmbeddedValue &&
          !_fuir.clazzIsRef(tc) &&
          _fuir.clazzKind(cc0) == FUIR.FeatureKind.Field)
        { // an embedded field in a value instance, so keep tvalue's
          // embedding. For chained embedded fields in value instances like
          // `t.f.g.h`, the embedding remains `t` for `f`, `g` and `h`.
          res = tvalue.rewrap(DFA.this, x -> resf[0].value());
        }
      return res;
    }


    /**
     * Helper routine for access (above) to perform a static access (cal or write).
     */
    Val access0(int s, Val tvalue, List<Val> args, int cc, Val original_tvalue /* NYI: ugly */)
    {
      var cs = DFA.this.site(s);
      cs._accesses.add(cc);
      var isCall = _fuir.codeAt(s) == FUIR.ExprKind.Call;
      Val r;
      if (isCall)
        {
          r = call0(s, tvalue, args, cc, original_tvalue);
        }
      else
        {
          if (!_fuir.clazzIsUnitType(_fuir.clazzResultClazz(cc)))
            {
              if (_reportResults && _options.verbose(9))
                {
                  say("DFA for "+_fuir.siteAsString(s) + ": "+_fuir.codeAtAsString(s)+": " +
                                     tvalue + ".set("+_fuir.clazzAsString(cc)+") := " + args.get(0));
                }
              var v = args.get(0);
              tvalue.value().setField(DFA.this, cc, v.value());
              tempEscapes(s, v, cc);
            }
          r = Value.UNIT;
        }
      return r;
    }


    /**
     * Helper for call to handle non-dynamic call to cc
     *
     * @param s site of call
     *
     * @param tvalue
     *
     * @param args
     *
     * @param cc clazz that is called
     *
     * @return result values of the call
     */
    Val call0(int s, Val tvalue, List<Val> args, int cc, Val original_tvalue)
    {
      // in case we access the value in a boxed target, unbox it first:
      tvalue = unboxTarget(tvalue, _fuir.accessTargetClazz(s), cc);
      Val res = null;
      switch (_fuir.clazzKind(cc))
        {
        case Abstract :
          Errors.error("Call to abstract feature encountered.",
                       "Found call to  " + _fuir.clazzAsString(cc));
        case Routine  :
        case Intrinsic:
        case Native   :
          {
            if (_fuir.clazzNeedsCode(cc))
              {
                var ca = newCall(cc, s, tvalue.value(), args, _call._env, _call);
                res = ca.result();
                if (res != null && res != Value.UNIT && !_fuir.clazzIsRef(_fuir.clazzResultClazz(cc)))
                  {
                    res = new EmbeddedValue(s, res.value());
                  }
                // check if target value of new call ca causes current _call's instance to escape.
                var or = _fuir.clazzOuterRef(cc);
                if (original_tvalue instanceof EmbeddedValue ev && ev._instance == _call._instance &&
                    _escapes.containsKey(ca._cc) &&
                    (or != -1) &&
                    _fuir.clazzFieldIsAdrOfValue(or)    // outer ref is adr, otherwise target is passed by value (primitive type like u32)
                    )
                  {
                    _call.escapes(s);
                  }
                tempEscapes(s, original_tvalue, _fuir.clazzOuterRef(cc));
                if (_reportResults && _options.verbose(9))
                  {
                    say("DFA for " +_fuir.siteAsString(s) + ": "+_fuir.codeAtAsString(s)+": " + ca);
                  }
              }
            break;
          }
        case Field:
          {
            res = tvalue.value().callField(DFA.this, cc, s, _call);
            if (_reportResults && _options.verbose(9))
              {
                say("DFA for "+_fuir.siteAsString(s) + ": "+_fuir.codeAtAsString(s)+": " +
                                   tvalue + ".get(" + _fuir.clazzAsString(cc) + ") => " + res);
              }
            break;
          }
        default:       throw new Error("This should not happen: Unknown feature kind: " + _fuir.clazzKind(cc));
        }
      return res;
    }


    /**
     * For a given value v of value type vc create a boxed ref value of type rc.
     */
    @Override
    public Pair<Val, Unit> box(int s, Val val, int vc, int rc)
    {
      var boxed = val.value().box(DFA.this, vc, rc, _call);
      return new Pair<>(boxed, _unit_);
    }


    /**
     * Get the current instance
     */
    @Override
    public Pair<Val, Unit> current(int s)
    {
      return new Pair<>(_call._instance, _unit_);
    }


    /**
     * Get the outer instance
     */
    @Override
    public Pair<Val, Unit> outer(int s)
    {
      return new Pair<>(_call._target, _unit_);
    }

    /**
     * Get the argument #i
     */
    @Override
    public Val arg(int s, int i)
    {
      return _call._args.get(i);
    }


    /**
     * Get a constant value of type constCl with given byte data d.
     */
    @Override
    public Pair<Val, Unit> constData(int s, int constCl, byte[] d)
    {
      var o = _unit_;
      var r = switch (_fuir.getSpecialClazz(constCl))
        {
        case c_bool -> d[0] == 1 ? _true : _false;
        case c_i8   ,
             c_i16  ,
             c_i32  ,
             c_i64  ,
             c_u8   ,
             c_u16  ,
             c_u32  ,
             c_u64  ,
             c_f32  ,
             c_f64  -> new NumericValue(DFA.this, constCl, ByteBuffer.wrap(d).position(4).order(ByteOrder.LITTLE_ENDIAN));
        case c_Const_String, c_String -> newConstString(Arrays.copyOfRange(d, 4, ByteBuffer.wrap(d).order(ByteOrder.LITTLE_ENDIAN).getInt()+4), _call);
        default ->
          {
            if (!_fuir.clazzIsChoice(constCl))
              {
                yield _fuir.clazzIsArray(constCl)
                  ? newArrayConst(s, constCl, _call, ByteBuffer.wrap(d).order(ByteOrder.LITTLE_ENDIAN))
                  : newValueConst(s, constCl, _call, ByteBuffer.wrap(d).order(ByteOrder.LITTLE_ENDIAN));
              }
            else
              {
                Errors.error("Unsupported constant in DFA analysis.",
                             "DFA cannot handle constant of clazz '" + _fuir.clazzAsString(constCl) + "' ");
                yield null;
              }
          }
        };
      return new Pair<>(r, o);
    }


    /**
     * deserialize value constant of type `constCl` from `b`
     *
     * @param s the site of the constant
     *
     * @param constCl the constants clazz, e.g. `(tuple u32 codepoint)`
     *
     * @param context for debugging: Reason that causes this const string to be
     * part of the analysis.
     *
     * @param b the serialized data to be used when creating this constant
     *
     * @return an instance of `constCl` with fields initialized using the data from `b`.
     */
    private Value newValueConst(int s, int constCl, Context context, ByteBuffer b)
    {
      var result = newInstance(constCl, NO_SITE, context);
      var args = new List<Val>();
      for (int index = 0; index < _fuir.clazzArgCount(constCl); index++)
        {
          var f = _fuir.clazzArg(constCl, index);
          var fr = _fuir.clazzArgClazz(constCl, index);
          var bytes = _fuir.deseralizeConst(fr, b);
          var arg = constData(s, fr, bytes).v0().value();
          args.add(arg);
          result.setField(DFA.this, f, arg);
        }

      // register calls for constant creation even though
      // not every backend actually performs these calls.
      newCall(constCl, NO_SITE, _universe, args, null /* new environment */, context);

      return result;
    }


    /**
     * deserialize array constant of type `constCl` from `d`
     *
     * @param s the site of the constant
     *
     * @param constCl the constants clazz, e.g. `array (tuple i32 codepoint)`
     *
     * @param context for debugging: Reason that causes this const string to be
     * part of the analysis.
     *
     * @param d the serialized data to be used when creating this constant
     *
     * @return an instance of `constCl` with fields initialized using the data from `d`.
     */
    private Value newArrayConst(int s, int constCl, Call context, ByteBuffer d)
    {
      var result = newInstance(constCl, NO_SITE, context);
      var sa = _fuir.clazzField(constCl, 0);
      var sa0 = newInstance(_fuir.clazzResultClazz(sa), NO_SITE, context);

      var elementClazz = _fuir.inlineArrayElementClazz(constCl);
      var data = _fuir.clazzField(_fuir.clazzResultClazz(sa), 0);
      var lengthField = _fuir.clazzField(_fuir.clazzResultClazz(sa), 1);

      var elCount = d.getInt();

      // joining elements before instantiating sysarray
      // because setEl always triggers a DFA changed.
      Value elements = null;
      for (int idx = 0; idx < elCount; idx++)
        {
          var b = _fuir.deseralizeConst(elementClazz, d);
          elements = elements == null
            ? constData(s, elementClazz, b).v0().value()
            : elements.join(constData(s, elementClazz, b).v0().value());
        }
      SysArray sysArray = elCount == 0 ? new SysArray(DFA.this, new byte[0], elementClazz) :  new SysArray(DFA.this, elements);

      sa0.setField(DFA.this, data, sysArray);
      sa0.setField(DFA.this, lengthField, new NumericValue(DFA.this, _fuir.clazzResultClazz(lengthField), elCount));
      result.setField(DFA.this, sa, sa0);
      return result;
    }


    /**
     * Perform a match on value subv.
     */
    @Override
    public Pair<Val, Unit> match(int s, AbstractInterpreter<Val,Unit> ai, Val subv)
    {
      Val r = null; // result value null <=> does not return.  Will be set to Value.UNIT if returning case was found.
      for (var mc = 0; mc < _fuir.matchCaseCount(s); mc++)
        {
          // array to permit modification in lambda
          var takenA    = new boolean[] { false };
          var field = _fuir.matchCaseField(s, mc);
          for (var t : _fuir.matchCaseTags(s, mc))
            {
              subv.value().forAll(v -> {
                  if (v.value() instanceof TaggedValue tv)
                    {
                      if (tv._tag == t)
                        {
                          var untagged = tv._original;
                          takenA[0] = true;
                          if (field != -1)
                            {
                              _call._instance.setField(DFA.this, field, untagged);
                            }
                        }
                    }
                  else
                    {
                      throw new Error("DFA encountered Unexpected value in match: " + v.getClass() + " '" + v + "' " +
                                      " for match of type " + _fuir.clazzAsString(_fuir.matchStaticSubject(s)));
                    }
                });

            }
          var taken = takenA[0];
          if (_reportResults && _options.verbose(9))
            {
              say("DFA for " + _fuir.siteAsString(s) + ": "+_fuir.codeAtAsString(s)+": "+subv+" case "+mc+": "+
                                 (taken ? "taken" : "not taken"));
            }

          if (taken)
            {
              var resv = ai.processCode(_fuir.matchCaseCode(s, mc));
              if (resv.v0() != null)
                { // if at least one case returns (i.e., result is not null), this match returns.
                  r = Value.UNIT;
                }
            }
        }
      DFA.this.site(s).recordResult(r == null);
      return new Pair<>(r, _unit_);
    }


    /**
     * Create a tagged value of type newcl from an untagged value.
     */
    @Override
    public Pair<Val, Unit> tag(int s, Val value, int newcl, int tagNum)
    {
      Val res = value.value().tag(_call._dfa, newcl, tagNum);
      return new Pair<>(res, _unit_);
    }


    /**
     * Access the effect of type ecl that is installed in the environment.
     */
    @Override
    public Pair<Val, Unit> env(int s, int ecl)
    {
      return new Pair<>(_call.getEffectForce(s, ecl), _unit_);
    }

  }


  /*----------------------------  constants  ----------------------------*/


  /**
   * For debugging: dump stack when _changed is set, for debugging when fix point
   * is not reached.
   *
   * To enable, use fz with
   *
   *   FUZION_JAVA_OPTIONS=-Ddev.flang.fuir.analysis.dfa.DFA.SHOW_STACK_ON_CHANGE=true
   */
  static final boolean SHOW_STACK_ON_CHANGE =
    Boolean.getBoolean("dev.flang.fuir.analysis.dfa.DFA.SHOW_STACK_ON_CHANGE");


  /**
   * singleton instance of Unit.
   */
  static Unit _unit_ = new Unit();


  /**
   * DFA's intrinsics.
   */
  static TreeMap<String, IntrinsicDFA> _intrinsics_ = new TreeMap<>();


  /**
   * Set of intrinsics that are found to be used by the DFA.
   */
  static Set<String> _usedIntrinsics_ = new TreeSet<>();


  /**
   * Maximum recursive analysis of newly created Calls, see `analyzeNewCall` for
   * details.
   */
  private static int MAX_NEW_CALL_RECURSION = 10;


  /*-------------------------  static methods  --------------------------*/


  /**
   * Helper method to add intrinsic to _intrinsics_.
   */
  private static void put(String n, IntrinsicDFA c)
  {
    _intrinsics_.put(n, (call) -> {
      _usedIntrinsics_.add(n);
      return c.analyze(call);
    });
  }


  /**
   * Get the names of all intrinsics supported by this backend.
   */
  public static Set<String> supportedIntrinsics()
  {
    return _intrinsics_.keySet();
  }


  /*----------------------------  variables  ----------------------------*/


  /**
   * Options provided to the 'fz' command.
   */
  public final FuzionOptions _options;


  /**
   * The intermediate code we are analyzing.
   */
  public final FUIR _fuir;


  /**
   * Special values of clazz 'bool' for 'true', 'false' and arbitrary bool
   * values.
   */
  final Value _true, _false, _bool;


  /**
   * Special value for universe.
   */
  final Value _universe;


  /**
   * Instances created during DFA analysis.
   */
  TreeMap<Value, Value> _instances = new TreeMap<>(Value.COMPARATOR);


  /**
   * Calls created during DFA analysis.
   */
  TreeMap<Call, Call> _calls = new TreeMap<>();


  /**
   * Sites created during DFA analysis.
   */
  TreeMap<Site, Site> _sites = new TreeMap<>();


  /**
   * Calls created during current sub-iteration of the DFA analysis.  These will
   * be analyzed at the end of the current iteration since they most likely add
   * new information.
   */
  TreeSet<Call> _newCalls = new TreeSet<>();


  /**
   * Current number of recursive analysis of newly created Calls, see `analyzeNewCall` for
   * details.
   */
  private int _newCallRecursiveAnalyzeCalls = 0;


  /**
   * Clazz ids for clazzes for of newly created calls for which recursive analysis is performed,
   * see `analyzeNewCall` for details.
   */
  private int[] _newCallRecursiveAnalyzeClazzes = new int[MAX_NEW_CALL_RECURSION];


  /**
   * Envs created during DFA analysis.
   */
  TreeMap<Env, Env> _envs = new TreeMap<>();


  /**
   * All fields that are ever written.  These will be needed even if they are
   * never read unless the assignments are actually removed (which is currently
   * not the case).
   */
  TreeSet<Integer> _writtenFields = new TreeSet<>();


  /**
   * All fields that are ever read.
   */
  TreeSet<Integer> _readFields = new TreeSet<>();


  /**
   * Map from type to corresponding default effects.
   *
   * NYI: this might need to be thread-local and not global!
   */
  public final TreeMap<Integer, Value> _defaultEffects = new TreeMap<>();


  /**
   * Map from effect-type to corresponding call that uses this effect.
   *
   * NYI: this might need to be thread-local and not global!
   */
  public final TreeMap<Integer, Call> _defaultEffectContexts = new TreeMap<>();


  /**
   * Flag to detect changes during current iteration of the fix-point algorithm.
   * If this remains false during one iteration we have reached a fix-point.
   */
  private boolean _changed = false;


  /**
   * For debugging: lazy creation of a message why _changed was set to true.
   */
  private Supplier<String> _changedSetBy;


  /**
   * Set of effects that are missing, excluding default effects.
   */
  TreeSet<Integer> _missingEffects = new TreeSet<>();


  /**
   * Flag to control output of errors.  This is set to true after a fix point
   * has been reached to report errors that should disappear when fix point is
   * reached (like vars are initialized).
   */
  boolean _reportResults = false;


  /*---------------------------  constructors  ---------------------------*/


  /**
   * Create DFA for given intermediate code.
   *
   * @param options the options to specify values for intrinsics like 'debug',
   * 'safety'.
   *
   * @param fuir the intermediate code.
   */
  public DFA(FuzionOptions options, FUIR fuir)
  {
    _options = options;
    _fuir = fuir;
    var bool = fuir.clazz(FUIR.SpecialClazzes.c_bool);
    _true  = new TaggedValue(this, bool, Value.UNIT, 1);
    _false = new TaggedValue(this, bool, Value.UNIT, 0);
    _bool  = _true.join(_false);
    _universe = newInstance(_fuir.clazzUniverse(), NO_SITE, null);
    Errors.showAndExit();
  }


  /*-----------------------------  methods  -----------------------------*/


  /**
   * Create a new Instance of FUIR using the information collected during this
   * DFA analysis. In particular, Let 'clazzNeedsCode' return false for
   * routines that were found never to be called.
   */
  public FUIR new_fuir()
  {
    dfa();
    var called = new TreeSet<Integer>();
    for (var c : _calls.values())
      {
        called.add(c._cc);
      }
    _options.timer("dfa");
    return new FUIR(_fuir)
      {
        public boolean clazzNeedsCode(int cl)
        {
          return super.clazzNeedsCode(cl) &&
            switch (_fuir.clazzKind(cl))
            {
            case Routine, Intrinsic,
                 Native             -> called.contains(cl);
            case Field              -> isBuiltInNumeric(_fuir.clazzOuterClazz(cl)) ||
                                       _readFields.contains(cl) ||
                                       // main result field
                                       _fuir.clazzResultField(_fuir.mainClazzId()) == cl;
            case Abstract           -> true;
            case Choice             -> true;
            };
        }


        /**
         * Determine the lifetime of the instance of a call to clazz cl.
         *
         * @param cl a clazz id of any kind
         *
         * @return A conservative estimate of the lifespan of cl's instance.
         * Undefined if a call to cl does not create an instance, Call if it is
         * guaranteed that the instance is inaccessible after the call returned.
         */
        public LifeTime lifeTime(int cl)
        {
          return
            (clazzKind(cl) != FeatureKind.Routine)
                ? super.lifeTime(cl)
                : currentEscapes(cl) ? LifeTime.Unknown :
                                       LifeTime.Call;
        }


        /**
         * For a call to cl, does the instance of cl escape the call?
         *
         * @param cl a call's inner clazz
         *
         * @return true iff the instance of the call must be allocated on the
         * heap.
         */
        private boolean currentEscapes(int cl)
        {
          return _escapes.containsKey(cl);
        }


        /**
         * For a call in cl in code block c at index i, does the result escape
         * the current clazz stack frame (such that it cannot be stored in a
         * local var in the stack frame of cl)
         *
         * @param cl the outer clazz of the call
         *
         * @param s site of call
         *
         * @return true iff the result of the call must be cloned on the heap.
         */
        public boolean doesResultEscape(int s)
        {
          return _escapesCode.contains(s);
        }


        @Override
        public int[] accessedClazzes(int s)
        {
          var ccs = super.accessedClazzes(s);
          var cs = site(s);
          var nr = new int[ccs.length];
          int j = 0;
          for (var cci = 0; cci < ccs.length; cci += 2)
            {
              var tt = ccs[cci+0];
              var cc = ccs[cci+1];
              if (cs._accesses.contains(cc))
                {
                  nr[j++] = tt;
                  nr[j++] = cc;
                }
            }
          return java.util.Arrays.copyOfRange(nr, 0, j);
        }


        @Override
        public boolean isIntrinsicUsed(String name)
        {
          return super.isIntrinsicUsed(name) && _usedIntrinsics_.contains(name);
        }


        @Override
        public boolean alwaysResultsInVoid(int s)
        {
          if (s < 0)
            {
              return false;
            }
          else
            {
              var code = _fuir.codeAt(s);
              return (code == ExprKind.Call || code == ExprKind.Match) && site(s).alwaysResultsInVoid() || super.alwaysResultsInVoid(s);
            }
        }

    };
  }


  /**
   * Perform DFA analysis
   */
  public void dfa()
  {
    var cl = _fuir.mainClazzId();

    newCall(cl,
            NO_SITE,
            Value.UNIT,
            new List<>(),
            null /* env */,
            Context._MAIN_ENTRY_POINT_);

    findFixPoint();
    _fuir.reportAbstractMissing();
    Errors.showAndExit();
  }


  /**
   * Iteratively perform data flow analysis until a fix point is reached.
   */
  void findFixPoint()
  {
    int cnt = 0;
    do
      {
        cnt++;
        if (_options.verbose(2))
          {
            _options.verbosePrintln(2,
                                    "DFA iteration #"+cnt+": --------------------------------------------------" +
                                    (_options.verbose(3) ? "calls:"+_calls.size() + ",instances:" + _instances.size() + ",envs:" + _envs.size() + "; " + _changedSetBy.get()
                                                         : ""                                                                  ));
          }
        _changed = false;
        _changedSetBy = () -> "*** change not set ***";
        iteration();
      }
    while (_changed && (true || cnt < 100) || false && (cnt < 50));
    if (_options.verbose(4))
      {
        _options.verbosePrintln(4, "DFA done:");
        _options.verbosePrintln(4, "Instances: " + _instances.values());
        _options.verbosePrintln(4, "Calls: ");
        for (var c : _calls.values())
          {
            _options.verbosePrintln(4, "  call: " + c);
          }
      }
    _reportResults = true;
    iteration();
  }


  /**
   * Set flag _changed to record the fact that the current iteration has not
   * reached a fix point yet.
   *
   * @param by in case _changed was not set yet, by is used to produce a message
   * why we have not reached a fix point yet.
   */
  void wasChanged(Supplier<String> by)
  {
    if (!_changed)
      {
        if (SHOW_STACK_ON_CHANGE)
          {
            var msg = by.get();
            say(msg);
            Thread.dumpStack();
          }
        _changedSetBy = by;
        _changed = true;
      }
  }


  /**
   * Perform one iteration of the analysis.
   */
  void iteration()
  {
    var vs = _calls.values();
    do
      {
        var s = vs.toArray(new Call[vs.size()]);
        _newCalls = new TreeSet<>();
        for (var c : s)
          {
            if (_reportResults && _options.verbose(4))
              {
                say(("----------------"+c+
                                    "----------------------------------------------------------------------------------------------------")
                                   .substring(0,100));

                var sb = new StringBuilder();
                var ignore = c.showWhy(sb);
                say(sb);
              }
            analyze(c);
          }
        vs = _newCalls;
      }
    while (!vs.isEmpty());
  }


  /**
   * Analyze code for given call
   *
   * @param c the call
   */
  void analyze(Call c)
  {
    if (_fuir.clazzKind(c._cc) == FUIR.FeatureKind.Routine)
      {
        var i = c._instance;
        check
          (c._args.size() == _fuir.clazzArgCount(c._cc));
        for (var a = 0; a < c._args.size(); a++)
          {
            var af = _fuir.clazzArg(c._cc, a);
            var aa = c._args.get(a);
            i.setField(this, af, aa.value());
          }

        // copy outer ref argument to outer ref field:
        var or = _fuir.clazzOuterRef(c._cc);
        if (or != -1)
          {
            i.setField(this, or, c._target);
          }

        var ai = new AbstractInterpreter<Val,Unit>(_fuir, new Analyze(c));
        var r = ai.processClazz(c._cc);
        if (r.v0() != null)
          {
            c.returns();
          }
      }
  }


  /**
   * Flag to detect and stop (endless) recursion within NYIintrinsicMissing.
   */
  static boolean _recursion_in_NYIintrinsicMissing = false;


  /**
   * Report that intrinsic 'cl' is missing and return Value.UNDEFINED.
   */
  static Value NYIintrinsicMissing(Call cl)
  {
    if (true || cl._dfa._reportResults)
      {
        var name = cl._dfa._fuir.clazzOriginalName(cl._cc);

        // NYI: Proper error handling.
        Errors.error("NYI: Support for intrinsic '" + name + "' missing");

        // cl.showWhy(sb) may try to print result values that depend on
        // intrinsics, so we risk running into an endless recursion here:
        if (!_recursion_in_NYIintrinsicMissing)
          {
            _recursion_in_NYIintrinsicMissing = true;
            var sb = new StringBuilder();
            var ignore = cl.showWhy(sb);
            say_err(sb);
            _recursion_in_NYIintrinsicMissing = false;
          }
      }
    return Value.UNDEFINED;
  }


  /**
   * Map of clazzes whose instance may escape the call to the clazz's routine.
   *
   * key = clazz
   * value = site where clazz was found to escape
   */
  TreeMap<Integer, Integer> _escapes = new TreeMap<>();


  /**
   * Set of sites of calls whose result value may escape the caller's
   * context (since a pointer to that value may be passed to a call).
   */
  TreeSet<Integer> _escapesCode = new TreeSet<>();


  /**
   * Record that the given clazz escape the call to the routine.  If it
   * does escape, the instance cannot be allocated on the stack.
   *
   * @param cc the clazz to check
   *
   * @param s
   *
   * @param call
   */
  void escapes(int cc, int s, Call call)
  {
    if (s != -1 && _fuir.clazzAsString(cc).matches(".*#loop[0-9]+") /* NYI: REALLY BAD HACK: do not use RegEx */
        && (_fuir.clazzLoopAllowEscape() == -1 || call.getEffectCheck(_fuir.clazzLoopAllowEscape()) == null)
       )
      {
        DfaErrors.loopInstanceEscapes(_fuir.declarationPos(cc),  _fuir.sitePos(s), escapeRoute(s));
      }
    if (!_escapes.containsKey(cc))
      {
        _escapes.put(cc, s);
        wasChanged(() -> "Escapes: " + _fuir.clazzAsString(cc));
      }
  }


  /*
   * The recorded escape route of s.
   * To be used by the error message.
   */
  private List<String> escapeRoute(int s)
  {
    var ac = _fuir.accessedClazz(s);
    SourcePosition pos = _fuir.sitePos(s);
    // NYI: UNDER DEVELOPMENT: skip pre_and_call
    return _escapes.get(ac) == -1
      ? new List<>(
          pos + System.lineSeparator() + pos.showInSource(),
          _fuir.declarationPos(ac) + System.lineSeparator() + _fuir.declarationPos(ac).showInSource()
        )
      : new List<>(
          pos + System.lineSeparator() + pos.showInSource(),
          escapeRoute(_escapes.get(ac))
        );
  }


  /**
   * Record that a temporary value whose address is taken may live longer than
   * than the current call, so we cannot store it in the current stack frame.
   *
   * @param s site of the call or assignment we are analysing
   *
   * @param v value we are taking an address of
   *
   * @param adrField field the address of `v` is assigned to.
   *
   */
  void tempEscapes(int s, Val v, int adrField)
  {
    if (v instanceof EmbeddedValue ev &&
        adrField != -1 &&
        !_fuir.clazzIsRef(_fuir.clazzResultClazz(adrField)) &&
        _fuir.clazzFieldIsAdrOfValue(adrField)
        && ev._site != -1
        )
      {
        var cp = ev._site;
        if (!_escapesCode.contains(cp))
          {
            _escapesCode.add(cp);
            wasChanged(() -> "code escapes: "+_fuir.codeAtAsString(s));
          }
      }
  }


  /**
   * Helper routine for intrinsics that set the elements of an array given as an argument
   *
   * @param cl the intrinsic call
   *
   * @param argnum the argument that is an internal array
   *
   * @param intrinsicName name of the intrinsic, just of error handling
   *
   * @param elementClazz the element type of the array.
   */
  static void setArrayElementsToAnything(Call cl, int argnum, String intrinsicName, FUIR.SpecialClazzes elementClazz)
  {
    var array = cl._args.get(argnum);
    if (array instanceof SysArray sa)
      {
        sa.setel(new NumericValue(cl._dfa, cl._dfa._fuir.clazz(FUIR.SpecialClazzes.c_i32)),
                 new NumericValue(cl._dfa, cl._dfa._fuir.clazz(elementClazz)));
      }
    else
      {
        Errors.fatal("DFA internal error: intrinsic '" + intrinsicName+ ": Expected class SysArray, found " + array.getClass() + " " + array);
      }
  }


  /**
   * Helper routine to call setArrayElementsToAnything with specific clazz.
   *
   * @param cl the intrinsic call
   *
   * @param argnum the argument that is an internal array
   *
   * @param intrinsicName name of the intrinsic, just of error handling
   */
  static void setArrayU8ElementsToAnything (Call cl, int argnum, String intrinsicName) { setArrayElementsToAnything(cl, argnum, intrinsicName, FUIR.SpecialClazzes.c_u8 ); }
  static void setArrayI32ElementsToAnything(Call cl, int argnum, String intrinsicName) { setArrayElementsToAnything(cl, argnum, intrinsicName, FUIR.SpecialClazzes.c_i32); }
  static void setArrayI64ElementsToAnything(Call cl, int argnum, String intrinsicName) { setArrayElementsToAnything(cl, argnum, intrinsicName, FUIR.SpecialClazzes.c_i64); }


  static
  {
    put("Type.name"                      , cl -> cl._dfa.newConstString(cl._dfa._fuir.clazzTypeName(cl._dfa._fuir.clazzOuterClazz(cl._cc)), cl) );

    put("concur.atomic.compare_and_swap0",  cl ->
        {
          var v = cl._dfa._fuir.lookupAtomicValue(cl._dfa._fuir.clazzOuterClazz(cl._cc));

          if (CHECKS) check
            (cl._dfa._fuir.clazzNeedsCode(v));

          var atomic    = cl._target;
          var expected  = cl._args.get(0);
          var new_value = cl._args.get(1).value();
          var res = atomic.callField(cl._dfa, v, cl.site(), cl);

          // NYI: we could make compare_and_swap more accurate and call setField only if res contains expected, need bit-wise comparison
          atomic.setField(cl._dfa, v, new_value);
          return res;
        });

    put("concur.atomic.compare_and_set0",  cl ->
        {
          var v = cl._dfa._fuir.lookupAtomicValue(cl._dfa._fuir.clazzOuterClazz(cl._cc));

          if (CHECKS) check
            (cl._dfa._fuir.clazzNeedsCode(v));

          var atomic    = cl._target;
          var expected  = cl._args.get(0);
          var new_value = cl._args.get(1).value();
          var res = atomic.callField(cl._dfa, v, cl.site(), cl);

          // NYI: we could make compare_and_set more accurate and call setField only if res contains expected, need bit-wise comparison
          atomic.setField(cl._dfa, v, new_value);
          return cl._dfa._bool;
        });

    put("concur.atomic.racy_accesses_supported",  cl ->
        {
          // NYI: racy_accesses_supported could return true or false depending on the backend's behaviour.
          return cl._dfa._bool;
        });

    put("concur.atomic.read0",  cl ->
        {
          var v = cl._dfa._fuir.lookupAtomicValue(cl._dfa._fuir.clazzOuterClazz(cl._cc));

          if (CHECKS) check
            (cl._dfa._fuir.clazzNeedsCode(v));

          var atomic = cl._target;
          return atomic.callField(cl._dfa, v, cl.site(), cl);
        });

    put("concur.atomic.write0", cl ->
        {
          var v = cl._dfa._fuir.lookupAtomicValue(cl._dfa._fuir.clazzOuterClazz(cl._cc));

          if (CHECKS) check
            (cl._dfa._fuir.clazzNeedsCode(v));

          var atomic    = cl._target;
          var new_value = cl._args.get(0).value();
          atomic.setField(cl._dfa, v, new_value);
          return Value.UNIT;
        });

    put("concur.util.loadFence", cl ->
        {
          return Value.UNIT;
        });

    put("concur.util.storeFence", cl ->
        {
          return Value.UNIT;
        });

    put("safety"                         , cl -> cl._dfa._options.fuzionSafety() ? cl._dfa._true : cl._dfa._false );
    put("debug"                          , cl -> cl._dfa._options.fuzionDebug()  ? cl._dfa._true : cl._dfa._false );
    put("debug_level"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc), cl._dfa._options.fuzionDebugLevel()) );

    put("fuzion.sys.args.count"          , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("fuzion.sys.args.get"            , cl -> cl._dfa.newConstString(null, cl) );
    put("fuzion.std.exit"                , cl -> null );
    put("fuzion.sys.fileio.read"         , cl ->
        {
          setArrayU8ElementsToAnything(cl, 1, "fuzion.sys.fileio.read");
          return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc));
        });
    put("fuzion.sys.fileio.write"        , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("fuzion.sys.fileio.delete"       , cl -> cl._dfa._bool );
    put("fuzion.sys.fileio.move"         , cl -> cl._dfa._bool );
    put("fuzion.sys.fileio.create_dir"   , cl -> cl._dfa._bool );
    put("fuzion.sys.fileio.open"         , cl ->
        {
          setArrayI64ElementsToAnything(cl, 1, "fuzion.sys.fileio.open");
          return Value.UNIT;
        });
    put("fuzion.sys.fileio.close"        , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("fuzion.sys.fileio.stats"        , cl -> { setArrayI64ElementsToAnything(cl, 1, "fuzion.sys.fileio.stats"   ); return cl._dfa._bool; });
    put("fuzion.sys.fileio.lstats"       , cl -> { setArrayI64ElementsToAnything(cl, 1, "fuzion.sys.fileio.lstats"  ); return cl._dfa._bool; });
    put("fuzion.sys.fileio.seek"         , cl -> { setArrayI64ElementsToAnything(cl, 2, "fuzion.sys.fileio.seek"    ); return Value.UNIT; });
    put("fuzion.sys.fileio.file_position", cl -> { setArrayI64ElementsToAnything(cl, 1, "fuzion.sys.fileio.position"); return Value.UNIT; });
    put("fuzion.sys.fileio.mmap"         , cl ->
        {
          setArrayI32ElementsToAnything(cl, 3, "fuzion.sys.fileio.mmap");
          return new SysArray(cl._dfa, new NumericValue(cl._dfa, cl._dfa._fuir.clazz(FUIR.SpecialClazzes.c_u8))); // NYI: length wrong, get from arg
        });
    put("fuzion.sys.fileio.munmap"       , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("fuzion.sys.fileio.mapped_buffer_get", cl ->
        {
          var array = cl._args.get(0).value();
          var index = cl._args.get(1).value();
          if (array instanceof SysArray sa)
            {
              return sa.get(index);
            }
          else
            {
              throw new Error("intrinsic fuzion.sys.internal_array.gel: Expected class SysArray, found "+array.getClass()+" "+array);
            }
        });
    put("fuzion.sys.fileio.mapped_buffer_set", cl ->
        {
          var array = cl._args.get(0).value();
          var index = cl._args.get(1).value();
          var value = cl._args.get(2).value();
          if (array instanceof SysArray sa)
            {
              sa.setel(index, value);
              return Value.UNIT;
            }
          else
            {
              throw new Error("intrinsic fuzion.sys.internal_array.setel: Expected class SysArray, found "+array.getClass()+" "+array);
            }
        });

    put("fuzion.sys.fileio.flush"        , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("fuzion.sys.fatal_fault0"        , cl-> null                                                              );
    put("fuzion.sys.stdin.stdin0"        , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("fuzion.sys.out.stdout"          , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("fuzion.sys.err.stderr"          , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );

    put("fuzion.sys.fileio.open_dir"     , cl -> { setArrayI64ElementsToAnything(cl, 1, "fuzion.sys.fileio.open_dir"); return Value.UNIT; } );
    put("fuzion.sys.fileio.read_dir"     , cl -> cl._dfa.newConstString(null, cl) );
    put("fuzion.sys.fileio.read_dir_has_next", cl -> cl._dfa._bool );
    put("fuzion.sys.fileio.close_dir"    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("i8.prefix -°"                   , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i16.prefix -°"                  , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i32.prefix -°"                  , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i64.prefix -°"                  , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i8.infix -°"                    , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i16.infix -°"                   , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i32.infix -°"                   , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i64.infix -°"                   , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i8.infix +°"                    , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i16.infix +°"                   , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i32.infix +°"                   , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i64.infix +°"                   , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i8.infix *°"                    , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i16.infix *°"                   , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i32.infix *°"                   , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i64.infix *°"                   , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i8.div"                         , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i16.div"                        , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i32.div"                        , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i64.div"                        , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i8.mod"                         , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i16.mod"                        , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i32.mod"                        , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i64.mod"                        , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i8.infix <<"                    , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i16.infix <<"                   , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i32.infix <<"                   , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i64.infix <<"                   , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i8.infix >>"                    , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i16.infix >>"                   , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i32.infix >>"                   , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i64.infix >>"                   , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i8.infix &"                     , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i16.infix &"                    , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i32.infix &"                    , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i64.infix &"                    , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i8.infix |"                     , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i16.infix |"                    , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i32.infix |"                    , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i64.infix |"                    , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i8.infix ^"                     , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i16.infix ^"                    , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i32.infix ^"                    , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );
    put("i64.infix ^"                    , cl -> { return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)); } );

    put("i8.type.equality"               , cl -> cl._dfa._bool );
    put("i16.type.equality"              , cl -> cl._dfa._bool );
    put("i32.type.equality"              , cl -> cl._dfa._bool );
    put("i64.type.equality"              , cl -> cl._dfa._bool );
    put("i8.type.lteq"                   , cl -> cl._dfa._bool );
    put("i16.type.lteq"                  , cl -> cl._dfa._bool );
    put("i32.type.lteq"                  , cl -> cl._dfa._bool );
    put("i64.type.lteq"                  , cl -> cl._dfa._bool );

    put("u8.prefix -°"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u16.prefix -°"                  , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u32.prefix -°"                  , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u64.prefix -°"                  , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u8.infix -°"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u16.infix -°"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u32.infix -°"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u64.infix -°"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u8.infix +°"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u16.infix +°"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u32.infix +°"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u64.infix +°"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u8.infix *°"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u16.infix *°"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u32.infix *°"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u64.infix *°"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u8.div"                         , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u16.div"                        , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u32.div"                        , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u64.div"                        , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u8.mod"                         , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u16.mod"                        , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u32.mod"                        , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u64.mod"                        , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u8.infix <<"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u16.infix <<"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u32.infix <<"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u64.infix <<"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u8.infix >>"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u16.infix >>"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u32.infix >>"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u64.infix >>"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u8.infix &"                     , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u16.infix &"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u32.infix &"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u64.infix &"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u8.infix |"                     , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u16.infix |"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u32.infix |"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u64.infix |"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u8.infix ^"                     , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u16.infix ^"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u32.infix ^"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u64.infix ^"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );

    put("u8.type.equality"               , cl -> cl._dfa._bool );
    put("u16.type.equality"              , cl -> cl._dfa._bool );
    put("u32.type.equality"              , cl -> cl._dfa._bool );
    put("u64.type.equality"              , cl -> cl._dfa._bool );
    put("u8.type.lteq"                   , cl -> cl._dfa._bool );
    put("u16.type.lteq"                  , cl -> cl._dfa._bool );
    put("u32.type.lteq"                  , cl -> cl._dfa._bool );
    put("u64.type.lteq"                  , cl -> cl._dfa._bool );

    put("i8.as_i32"                      , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("i16.as_i32"                     , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("i32.as_i64"                     , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("i32.as_f64"                     , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("i64.as_f64"                     , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u8.as_i32"                      , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u16.as_i32"                     , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u32.as_i64"                     , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u32.as_f64"                     , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u64.as_f64"                     , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("i8.cast_to_u8"                  , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("i16.cast_to_u16"                , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("i32.cast_to_u32"                , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("i64.cast_to_u64"                , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u8.cast_to_i8"                  , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u16.cast_to_i16"                , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u32.cast_to_i32"                , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u32.cast_to_f32"                , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u64.cast_to_i64"                , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u64.cast_to_f64"                , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u16.low8bits"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u32.low8bits"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u64.low8bits"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u32.low16bits"                  , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u64.low16bits"                  , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("u64.low32bits"                  , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );

    put("f32.prefix -"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.prefix -"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.infix +"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.infix +"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.infix -"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.infix -"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.infix *"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.infix *"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.infix /"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.infix /"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.infix %"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.infix %"                    , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.infix **"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.infix **"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.infix ="                    , cl -> cl._dfa._bool );
    put("f64.infix ="                    , cl -> cl._dfa._bool );
    put("f32.infix <="                   , cl -> cl._dfa._bool );
    put("f64.infix <="                   , cl -> cl._dfa._bool );
    put("f32.infix >="                   , cl -> cl._dfa._bool );
    put("f64.infix >="                   , cl -> cl._dfa._bool );
    put("f32.infix <"                    , cl -> cl._dfa._bool );
    put("f64.infix <"                    , cl -> cl._dfa._bool );
    put("f32.infix >"                    , cl -> cl._dfa._bool );
    put("f64.infix >"                    , cl -> cl._dfa._bool );
    put("f32.as_f64"                     , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.as_f32"                     , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.as_i64_lax"                 , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.cast_to_u32"                , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.cast_to_u64"                , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );

    put("f32.type.min_exp"               , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.type.max_exp"               , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.type.min_positive"          , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.type.max"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.type.epsilon"               , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.type.is_NaN"                , cl -> cl._dfa._bool );
    put("f64.type.is_NaN"                , cl -> cl._dfa._bool );
    put("f64.type.min_exp"               , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.type.max_exp"               , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.type.min_positive"          , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.type.max"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.type.epsilon"               , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.type.square_root"           , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.type.square_root"           , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.type.exp"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.type.exp"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.type.log"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.type.log"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.type.sin"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.type.sin"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.type.cos"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.type.cos"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.type.tan"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.type.tan"                   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.type.asin"                  , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.type.asin"                  , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.type.acos"                  , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.type.acos"                  , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.type.atan"                  , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.type.atan"                  , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.type.sinh"                  , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.type.sinh"                  , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.type.cosh"                  , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.type.cosh"                  , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f32.type.tanh"                  , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("f64.type.tanh"                  , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );

    put("fuzion.sys.internal_array_init.alloc", cl -> new SysArray(cl._dfa, new byte[0], -1)); // NYI: get length from args
    put("fuzion.sys.internal_array.setel", cl ->
        {
          var array = cl._args.get(0).value();
          var index = cl._args.get(1).value();
          var value = cl._args.get(2).value();
          if (array instanceof SysArray sa)
            {
              sa.setel(index, value);
              return Value.UNIT;
            }
          else
            {
              throw new Error("intrinsic fuzion.sys.internal_array.setel: Expected class SysArray, found "+array.getClass()+" "+array);
            }
        });
    put("fuzion.sys.internal_array.get"  , cl ->
        {
          var array = cl._args.get(0).value();
          var index = cl._args.get(1).value();
          if (array instanceof SysArray sa)
            {
              return sa.get(index);
            }
          else
            {
              throw new Error("intrinsic fuzion.sys.internal_array.gel: Expected class SysArray, found "+array.getClass()+" "+array);
            }
        });
    put("fuzion.sys.internal_array.freeze"
                                         , cl -> Value.UNIT);
    put("fuzion.sys.internal_array.ensure_not_frozen"
                                         , cl -> Value.UNIT);
    put("fuzion.sys.env_vars.has0"       , cl -> cl._dfa._bool );
    put("fuzion.sys.env_vars.get0"       , cl -> cl._dfa.newConstString(null, cl) );
    put("fuzion.sys.env_vars.set0"       , cl -> cl._dfa._bool );
    put("fuzion.sys.env_vars.unset0"     , cl -> cl._dfa._bool );
    put("fuzion.sys.misc.unique_id"      , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("fuzion.sys.thread.spawn0"       , cl ->
        {
          var oc = cl._dfa._fuir.clazzActualGeneric(cl._cc, 0);
          var call = cl._dfa._fuir.lookupCall(oc);

          if (CHECKS) check
            (cl._dfa._fuir.clazzNeedsCode(call));

          // NYI: spawn0 needs to set up an environment representing the new
          // thread and perform thread-related checks (race-detection. etc.)!
          var ncl = cl._dfa.newCall(call, NO_SITE, cl._args.get(0).value(), new List<>(), null /* new environment */, cl);
          return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc));
        });
    put("fuzion.sys.thread.join0"        , cl -> Value.UNIT);

    // NYI these intrinsics manipulate an array passed as an arg.
    put("fuzion.sys.net.bind0"           , cl ->
        {
          setArrayI64ElementsToAnything(cl, 5, "fuzion.sys.net.bind0");
          return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc));
        });
    put("fuzion.sys.net.listen"          , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("fuzion.sys.net.accept"          , cl ->
        {
          setArrayI64ElementsToAnything(cl, 1, "fuzion.sys.net.accept");
          return cl._dfa._bool;
        });
    put("fuzion.sys.net.connect0"        , cl ->
        {
          setArrayI64ElementsToAnything(cl, 5, "fuzion.sys.net.connect0");
          return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc));
        });
    put("fuzion.sys.net.get_peer_address", cl ->
        {
          setArrayU8ElementsToAnything(cl, 1, "fuzion.sys.net.get_peer_address");
          return new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc));
        });
    put("fuzion.sys.net.get_peer_port"   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("fuzion.sys.net.read"            , cl ->
        {
          setArrayU8ElementsToAnything(cl, 1, "fuzion.sys.net.read");
          setArrayI64ElementsToAnything(cl, 3, "fuzion.sys.net.read");
          return cl._dfa._bool;
        });
    put("fuzion.sys.net.write"           , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("fuzion.sys.net.close0"          , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("fuzion.sys.net.set_blocking0"   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );

    put("fuzion.sys.process.create" , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("fuzion.sys.process.wait"   , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("fuzion.sys.pipe.read"      , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("fuzion.sys.pipe.write"     , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("fuzion.sys.pipe.close"     , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );

    put("fuzion.std.nano_sleep"          , cl -> Value.UNIT );
    put("fuzion.std.nano_time"           , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );

    put("fuzion.std.date_time"           , cl ->
        {
          setArrayI32ElementsToAnything(cl, 0, "fuzion.sys.net.date_time");
          return Value.UNIT;
        });

    put("effect.replace"                 , cl ->
        {
          var ecl = cl._dfa._fuir.effectType(cl._cc);
          var new_e = cl._target;
          cl.replaceEffect(ecl, new_e);
          return Value.UNIT;
        });
    put("effect.default"                 , cl ->
        {
          var ecl = cl._dfa._fuir.effectType(cl._cc);
          var new_e = cl._target;
          var old_e = cl._dfa._defaultEffects.get(ecl);
          if (old_e != null)
            {
              new_e = old_e.join(new_e);
            }
          if (old_e == null || Value.compare(old_e, new_e) != 0)
            {
              cl._dfa._defaultEffects.put(ecl, new_e);
              cl._dfa._defaultEffectContexts.put(ecl, cl);
              cl._dfa.wasChanged(() -> "effect.default called: " + cl._dfa._fuir.clazzAsString(cl._cc));
            }
          return Value.UNIT;
        });
    put(EFFECT_ABORTABLE_NAME            , cl ->
        {
          var ecl = cl._dfa._fuir.effectType(cl._cc);
          var oc = cl._dfa._fuir.clazzActualGeneric(cl._cc, 0);
          var call = cl._dfa._fuir.lookupCall(oc);

          if (CHECKS) check
            (cl._dfa._fuir.clazzNeedsCode(call));

          var env = cl._env;
          var newEnv = cl._dfa.newEnv(cl, env, ecl, cl._target);
          var ncl = cl._dfa.newCall(call, NO_SITE, cl._args.get(0).value(), new List<>(), newEnv, cl);
          // NYI: result must be null if result of ncl is null (ncl does not return) and effect.abort is not called
          return Value.UNIT;
        });
    put("effect.abort0"                  , cl ->
        {
          var ecl = cl._dfa._fuir.effectType(cl._cc);
          var new_e = cl._target;
          cl.replaceEffect(ecl, new_e);
          // NYI: we might have to do cl.returns() for 'cl' being the
          // corresponding call to 'effect.abortable' and make sure new_e is
          // used to create the value produced by the effect.
          return null;
        });
    put("effect.type.is_installed"       , cl -> cl.getEffectCheck(cl._dfa._fuir.clazzActualGeneric(cl._cc, 0)) != null
        ? cl._dfa._true
        : cl._dfa._bool  /* NYI: currently, this is never FALSE since a default effect might get installed turning this into TRUE
                          * should reconsider if handling of default effects changes
                          */
        );

    put("fuzion.java.Java_Object.is_null0"  , cl ->
        {
          cl._dfa._readFields.add(cl._dfa._fuir.clazzArg(cl._cc, 0));
          return cl._dfa._bool;
        });
    put("fuzion.java.array_get"             , cl ->
        {
          var jref = cl._dfa._fuir.lookupJavaRef(cl._dfa._fuir.clazzArgClazz(cl._cc, 0));
          cl._dfa._readFields.add(jref);
          return cl._dfa.newInstance(cl._dfa._fuir.clazzResultClazz(cl._cc), NO_SITE, cl._context);
        });
    put("fuzion.java.array_length"          , cl -> new NumericValue(cl._dfa, cl._dfa._fuir.clazzResultClazz(cl._cc)) );
    put("fuzion.java.array_to_java_object0" , cl ->
        {
          var rc = cl._dfa._fuir.clazzResultClazz(cl._cc);
          var jref = cl._dfa._fuir.lookupJavaRef(rc);
          var data = cl._dfa._fuir.lookup_fuzion_sys_internal_array_data(cl._dfa._fuir.clazzArgClazz(cl._cc,0));
          cl._dfa._readFields.add(data);
          var result = cl._dfa.newInstance(cl._dfa._fuir.clazzResultClazz(cl._cc), NO_SITE, cl._context);
          result.setField(cl._dfa, jref, Value.UNKNOWN_JAVA_REF); // NYI: record putfield of result.jref := args.get(0).data
          return result;
        });
    put("fuzion.java.bool_to_java_object"   , cl -> cl._dfa.newInstance(cl._dfa._fuir.clazzResultClazz(cl._cc), NO_SITE, cl._context) );
    put("fuzion.java.f32_to_java_object"    , cl -> cl._dfa.newInstance(cl._dfa._fuir.clazzResultClazz(cl._cc), NO_SITE, cl._context) );
    put("fuzion.java.f64_to_java_object"    , cl -> cl._dfa.newInstance(cl._dfa._fuir.clazzResultClazz(cl._cc), NO_SITE, cl._context) );
    put("fuzion.java.get_field0"            , cl -> cl._dfa.newInstance(cl._dfa._fuir.clazzResultClazz(cl._cc), NO_SITE, cl._context) );
    put("fuzion.java.i16_to_java_object"    , cl -> cl._dfa.newInstance(cl._dfa._fuir.clazzResultClazz(cl._cc), NO_SITE, cl._context) );
    put("fuzion.java.i32_to_java_object"    , cl -> cl._dfa.newInstance(cl._dfa._fuir.clazzResultClazz(cl._cc), NO_SITE, cl._context) );
    put("fuzion.java.i64_to_java_object"    , cl -> cl._dfa.newInstance(cl._dfa._fuir.clazzResultClazz(cl._cc), NO_SITE, cl._context) );
    put("fuzion.java.i8_to_java_object"     , cl -> cl._dfa.newInstance(cl._dfa._fuir.clazzResultClazz(cl._cc), NO_SITE, cl._context) );
    put("fuzion.java.java_string_to_string" , cl ->
        {
          var jref = cl._dfa._fuir.lookupJavaRef(cl._dfa._fuir.clazzArgClazz(cl._cc, 0));
          cl._dfa._readFields.add(jref);
          return cl._dfa.newConstString(null, cl);
        });
    put("fuzion.java.create_jvm", cl -> Value.UNIT);
    put("fuzion.java.string_to_java_object0", cl ->
      {
        var rc = cl._dfa._fuir.clazzResultClazz(cl._cc);
        var jref = cl._dfa._fuir.lookupJavaRef(rc);
        var jobj = cl._dfa.newInstance(rc, NO_SITE, null);
        jobj.setField(cl._dfa, jref, Value.UNKNOWN_JAVA_REF);
        return jobj;
      });
  }
  static Value newFuzionJavaCall(Call cl) {
    var rc = cl._dfa._fuir.clazzResultClazz(cl._cc);
    if (cl._dfa._fuir.clazzBaseName(rc).startsWith("outcome"))
    // NYI: HACK: should properly check if rc is an outcome
      {
        var oc = cl._dfa._fuir.clazzChoice(rc, 0);
        var res = switch (cl._dfa._fuir.getSpecialClazz(oc))
          {
            case c_i8, c_u16, c_i16, c_i32, c_i64,
              c_f32, c_f64, c_bool, c_unit -> {
              var v = switch (cl._dfa._fuir.getSpecialClazz(oc))
                {
                  case c_i8, c_u16, c_i16, c_i32, c_i64,
                    c_f32, c_f64 -> new NumericValue(cl._dfa, oc);
                  case c_bool -> cl._dfa._bool;
                  case c_unit -> Value.UNIT;
                  default -> Value.UNIT; // to match all the cases, should not be reached
                };
              yield v;
            }
            default -> {
              var jref = cl._dfa._fuir.lookupJavaRef(oc);
              var jobj = cl._dfa.newInstance(oc, NO_SITE, null);
              jobj.setField(cl._dfa, jref, Value.UNKNOWN_JAVA_REF);
              yield jobj;
            }
          };
        var okay = new TaggedValue(cl._dfa, rc, res, 0);
        var error_cl = cl._dfa._fuir.clazzChoice(rc, 1);
        var error = cl._dfa.newInstance(error_cl, NO_SITE, null);
        var msg = cl._dfa._fuir.lookup_error_msg(error_cl);
        error.setField(cl._dfa, msg, cl._dfa.newConstString(null, cl));
        var err = new TaggedValue(cl._dfa, rc, error, 1);
        return okay.join(err);
      }
    return switch (cl._dfa._fuir.getSpecialClazz(rc))
      {
        case c_i8, c_u16, c_i16, c_i32, c_i64,
          c_f32, c_f64 -> new NumericValue(cl._dfa, rc);
        case c_bool -> cl._dfa._bool;
        case c_unit -> Value.UNIT;
        default -> {
          var jref = cl._dfa._fuir.lookupJavaRef(rc);
          var jobj = cl._dfa.newInstance(rc, NO_SITE, null);
          jobj.setField(cl._dfa, jref, Value.UNKNOWN_JAVA_REF);
          yield jobj;
        }
      };
  }
  static {
    put("fuzion.java.call_c0"               , cl ->
      {
        var cc = cl._cc;
        var fuir = cl._dfa._fuir;
        var sref0 = fuir.lookupJavaRef(fuir.clazzArgClazz(cc, 0));
        var sref1 = fuir.lookupJavaRef(fuir.clazzArgClazz(cc, 1));
        var data2 = fuir.lookup_fuzion_sys_internal_array_data(fuir.clazzArgClazz(cc, 2));
        cl._dfa._readFields.add(sref0);
        cl._dfa._readFields.add(sref1);
        cl._dfa._readFields.add(data2);
        return newFuzionJavaCall(cl);
      });
    put("fuzion.java.call_s0"               , cl ->
      {
        var cc = cl._cc;
        var fuir = cl._dfa._fuir;
        var sref0 = fuir.lookupJavaRef(fuir.clazzArgClazz(cc, 0));
        var sref1 = fuir.lookupJavaRef(fuir.clazzArgClazz(cc, 1));
        var sref2 = fuir.lookupJavaRef(fuir.clazzArgClazz(cc, 2));
        var data3 = fuir.lookup_fuzion_sys_internal_array_data(fuir.clazzArgClazz(cc, 3));
        cl._dfa._readFields.add(sref0);
        cl._dfa._readFields.add(sref1);
        cl._dfa._readFields.add(sref2);
        cl._dfa._readFields.add(data3);
        return newFuzionJavaCall(cl);
      });
    put("fuzion.java.call_v0"               , cl ->
      {
        var cc = cl._cc;
        var fuir = cl._dfa._fuir;
        var sref0 = fuir.lookupJavaRef(fuir.clazzArgClazz(cc, 0));
        var sref1 = fuir.lookupJavaRef(fuir.clazzArgClazz(cc, 1));
        var sref2 = fuir.lookupJavaRef(fuir.clazzArgClazz(cc, 2));
        var sref3 = fuir.clazzArgClazz(cc, 3);
        var data4 = fuir.lookup_fuzion_sys_internal_array_data(fuir.clazzArgClazz(cc, 4));
        cl._dfa._readFields.add(sref0);
        cl._dfa._readFields.add(sref1);
        cl._dfa._readFields.add(sref2);
        cl._dfa._readFields.add(sref3);
        cl._dfa._readFields.add(data4);
        return newFuzionJavaCall(cl);
      });
    put("fuzion.java.get_static_field0"     , cl ->
      {
        var rc = cl._dfa._fuir.clazzResultClazz(cl._cc);
        var jref = cl._dfa._fuir.lookupJavaRef(rc);
        var jobj = cl._dfa.newInstance(rc, NO_SITE, null);
        jobj.setField(cl._dfa, jref, Value.UNKNOWN_JAVA_REF);
        return jobj;
      });
    put("fuzion.java.u16_to_java_object"    , cl -> cl._dfa.newInstance(cl._dfa._fuir.clazzResultClazz(cl._cc), NO_SITE, null) );
  }


  /**
   * Add given value to the set of default effect values for effect type ecl.
   */
  void replaceDefaultEffect(int ecl, Value e)
  {
    var old_e = _defaultEffects.get(ecl);
    var new_e = old_e == null ? e : old_e.join(e);
    if (old_e == null || Value.compare(old_e, new_e) != 0)
      {
        _defaultEffects.put(ecl, new_e);
        wasChanged(() -> "effect.replace called: " + _fuir.clazzAsString(ecl));
      }
  }


  /**
   * Check if given clazz is a built-in numeric clazz: i8..i64, u8..u64, f32 or f64.
   *
   * @param cl the clazz
   *
   * @return true iff cl is built-in numeric;
   */
  boolean isBuiltInNumeric(int cl)
  {
    return switch (_fuir.getSpecialClazz(cl))
      {
      case
        c_i8   ,
        c_i16  ,
        c_i32  ,
        c_i64  ,
        c_u8   ,
        c_u16  ,
        c_u32  ,
        c_u64  ,
        c_f32  ,
        c_f64  -> true;
      default -> false;
      };
  }


  /**
   * Create instance of given clazz.
   *
   * @param cl the clazz
   *
   * @param site the site index where the new instances is creates, NO_SITE
   * if not within code (intrinsics etc.)
   *
   * @param context for debugging: Reason that causes this instance to be part
   * of the analysis.
   */
  Value newInstance(int cl, int site, Context context)
  {
    if (PRECONDITIONS) require
      (!_fuir.clazzIsChoice(cl) || _fuir.clazzIs(cl, SpecialClazzes.c_bool));

    Value r;
    if (isBuiltInNumeric(cl))
      {
        r = new NumericValue(DFA.this, cl);
      }
    else if(_fuir.clazzIs(cl, SpecialClazzes.c_bool))
      {
        r = _bool;
      }
    else
      {
        if (_fuir.clazzIsRef(cl))
          {
            var vc = _fuir.clazzAsValue(cl);
            r = newInstance(vc, site, context).box(this, vc, cl, context);
          }
        else
          {
            r = new Instance(this, cl, site, context);
          }
      }
    return cache(r);
  }


  /**
   * Check if value 'r' exists already. If so, return the existing
   * one. Otherwise, add 'r' to the set of existing values, set _changed since
   * the state has changed and return r.
   */
  Value cache(Value r)
  {
    var e = _instances.get(r);
    if (e == null)
      {
        _instances.put(r, r);
        e = r;
        wasChanged(() -> "DFA.newInstance for " + _fuir.clazzAsString(r._clazz));
      }
    return e;
  }


  /**
   * Create constant string with given utf8 bytes.
   *
   * @param utf8Bytes the string contents or null if contents unknown
   *
   * @param context for debugging: Reason that causes this const string to be
   * part of the analysis.
   */
  Value newConstString(byte[] utf8Bytes, Context context)
  {
    var cs            = _fuir.clazz_Const_String();
    var internalArray = _fuir.clazz_Const_String_internal_array();
    var data          = _fuir.clazz_fuzionSysArray_u8_data();
    var length        = _fuir.clazz_fuzionSysArray_u8_length();
    var sysArray      = _fuir.clazzResultClazz(internalArray);
    var adata = utf8Bytes != null ? new SysArray(this, utf8Bytes, _fuir.clazz(FUIR.SpecialClazzes.c_u8))
                                  : new SysArray(this, new NumericValue(this, _fuir.clazz(FUIR.SpecialClazzes.c_u8)));
    var r = newInstance(cs, NO_SITE, context);
    var a = newInstance(sysArray, NO_SITE, context);
    a.setField(this,
               length,
                utf8Bytes != null ? new NumericValue(this, _fuir.clazzResultClazz(length), utf8Bytes.length)
                                  : new NumericValue(this, _fuir.clazzResultClazz(length)));
    a.setField(this, data  , adata);
    r.setField(this, internalArray, a);
    return r;
  }


  /**
   * Create call to given clazz with given target and args.
   *
   * @param cl the called clazz
   *
   * @param site the call site, -1 if unknown (from intrinsic or program entry
   * point)
   *
   * @param tvalue the target value on which cl is called
   *
   * @param args the arguments passed to the call
   *
   * @param env the environment at the call or null if none.
   *
   * @param context for debugging: Reason that causes this call to be part of
   * the analysis.
   *
   * @return cl a new or existing call to cl with the given target, args and
   * environment.
   */
  Call newCall(int cl, int site, Value tvalue, List<Val> args, Env env, Context context)
  {
    var r = new Call(this, cl, site, tvalue, args, env, context);
    var e = _calls.get(r);
    if (e == null)
      {
        _newCalls.add(r);
        _calls.put(r,r);
        e = r;
        wasChanged(() -> "DFA.newCall to " + r);
        analyzeNewCall(r);
      }
    return e;
  }


  /**
   * Helper for newCall to analyze a newly created call immediately. This helps
   * to avoid quadratic performance when analyzing a sequence of calls as in
   *
   *  a 1; a 2; a 3; a 4; a 5; ...
   *
   * Since a new call does not return, the analysis would stop for each iteration
   * after the fist new call.
   *
   * However, we cannot analyze all calls immediately since a recursive call
   * would result in an unbounded recursion during DFA.  So this analyzes the
   * call immediately unless it is part of a recursion or there are already
   * MAX_NEW_CALL_RECURSION new calls being analyzed right now.
   *
   * This might run into quadratic performance for code like the code above if
   * `a` would itself perform a new call to `b`, and `b` to `c`, etc. to a depth
   * that exceeds MAX_NEW_CALL_RECURSION.
   */
  private void analyzeNewCall(Call e)
  {
    var cnt = _newCallRecursiveAnalyzeCalls;
    if (cnt < _newCallRecursiveAnalyzeClazzes.length)
      {
        var rec = false;
        for (var i = 0; i<cnt; i++)
          {
            rec = rec || _newCallRecursiveAnalyzeClazzes[i] == e._cc;
          }
        if (!rec)
          {
            _newCallRecursiveAnalyzeClazzes[cnt] = e._cc;
            _newCallRecursiveAnalyzeCalls = cnt + 1;
            analyze(e);
            _newCallRecursiveAnalyzeCalls = cnt ;
          }
      }
  }


  /**
   * Create instance of 'Site' for given site
   *
   * @param s a FUIR site
   *
   * @return corresponding Site instance
   */
  Site site(int s)
    {
      var cs = new Site(s);
      var res = _sites.get(cs);
      if (res == null)
        {
          _sites.put(cs, cs);
          res = cs;
        }
      return res;
    }


  /**
   * Create new Env for given existing env and effect type  and value pair.
   *
   * @param cl the current clazz that installs a new effect
   *
   * @param env the previous environment.
   *
   * @param ecl the effect types
   *
   * @param ev the effect value
   *
   * @return new or existing Env instance created from env by adding ecl/ev.
   */
  Env newEnv(Call cl, Env env, int ecl, Value ev)
  {
    var newEnv = new Env(cl, env, ecl, cl._target);
    var e = _envs.get(newEnv);
    if (e == null)
      {
        _envs.put(newEnv, newEnv);
        e = newEnv;
        wasChanged(() -> "DFA.newEnv for " + newEnv);
      }
    return e;
  }

}

/* end of file */
