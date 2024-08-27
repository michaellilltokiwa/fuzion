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
 * Source of class Clazzes
 *
 *---------------------------------------------------------------------*/

package dev.flang.air;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import dev.flang.ast.AbstractAssign; // NYI: remove dependency!
import dev.flang.ast.AbstractBlock; // NYI: remove dependency!
import dev.flang.ast.AbstractCall; // NYI: remove dependency!
import dev.flang.ast.AbstractCase; // NYI: remove dependency!
import dev.flang.ast.Constant; // NYI: remove dependency!
import dev.flang.ast.Context; // NYI: remove dependency!
import dev.flang.ast.AbstractCurrent; // NYI: remove dependency!
import dev.flang.ast.AbstractFeature; // NYI: remove dependency!
import dev.flang.ast.AbstractMatch; // NYI: remove dependency!
import dev.flang.ast.AbstractType; // NYI: remove dependency!
import dev.flang.ast.Box; // NYI: remove dependency!
import dev.flang.ast.Env; // NYI: remove dependency!
import dev.flang.ast.Expr; // NYI: remove dependency!
import dev.flang.ast.InlineArray; // NYI: remove dependency!
import dev.flang.ast.State; // NYI: remove dependency!
import dev.flang.ast.Tag; // NYI: remove dependency!
import dev.flang.ast.Types; // NYI: remove dependency!
import dev.flang.ast.Universe; // NYI: remove dependency!

import dev.flang.util.ANY;
import dev.flang.util.Errors;
import dev.flang.util.FuzionOptions;
import dev.flang.util.HasSourcePosition;
import dev.flang.util.List;
import dev.flang.util.SourcePosition;


/**
 * Clazzes manages the actual clazzes used in the system during runtime.
 *
 * @author Fridtjof Siebert (siebert@tokiwa.software)
 */
public class Clazzes extends ANY
{


  /*----------------------------  constants  ----------------------------*/


  /**
   * All clazzes found in the system.
   *
   * NYI: One of these maps is probably redundant!
   */
  private final Map<Clazz, Clazz> clazzes = new TreeMap<>();
  private final Map<AbstractType, Clazz> _clazzesForTypes_ = new TreeMap<>();


  /**
   * All clazzes found so far that have not been analyzed yet for clazzes that
   * they require.
   */
  private final LinkedList<Clazz> clazzesToBeVisited = new LinkedList<>();


  public static Clazzes instance = new Clazzes();


  interface TypF
  {
    AbstractType get();
  }
  public class OnDemandClazz
  {
    final TypF _t;
    Clazz _clazz = null;
    OnDemandClazz(TypF t) { _t = t; }
    OnDemandClazz() { this(null); }

    /**
     * Get this clazz only if it was created, by a call to get() or by direct
     * call to Clazzes.instance.create():
     */
    public Clazz getIfCreated()
    {
      if (_clazz == null && _clazzesForTypes_.containsKey(_t.get()))
        {
          _clazz = clazz(_t.get());
        }
      return _clazz;
    }
    public Clazz get()
    {
      if (_clazz == null)
        {
          _clazz = clazz(_t.get());
        }
      return _clazz;
    }
    public void clear()
    {
      _clazz = null;
    }
  }

  /**
   * Handy preallocated classes to be used during execution:
   */
  public final OnDemandClazz universe    = new OnDemandClazz(() -> Types.resolved.universe.selfType());
  public final OnDemandClazz c_void      = new OnDemandClazz(() -> Types.resolved.t_void             );
  public final OnDemandClazz bool        = new OnDemandClazz(() -> Types.resolved.t_bool             );
  public final OnDemandClazz c_TRUE      = new OnDemandClazz(() -> Types.resolved.f_TRUE .selfType() );
  public final OnDemandClazz c_FALSE     = new OnDemandClazz(() -> Types.resolved.f_FALSE.selfType() );
  public final OnDemandClazz c_true      = new OnDemandClazz(() -> Types.resolved.f_true .selfType() );
  public final OnDemandClazz c_false     = new OnDemandClazz(() -> Types.resolved.f_false.selfType() );
  public final OnDemandClazz i8          = new OnDemandClazz(() -> Types.resolved.t_i8               );
  public final OnDemandClazz i16         = new OnDemandClazz(() -> Types.resolved.t_i16              );
  public final OnDemandClazz i32         = new OnDemandClazz(() -> Types.resolved.t_i32              );
  public final OnDemandClazz i64         = new OnDemandClazz(() -> Types.resolved.t_i64              );
  public final OnDemandClazz u8          = new OnDemandClazz(() -> Types.resolved.t_u8               );
  public final OnDemandClazz u16         = new OnDemandClazz(() -> Types.resolved.t_u16              );
  public final OnDemandClazz u32         = new OnDemandClazz(() -> Types.resolved.t_u32              );
  public final OnDemandClazz u64         = new OnDemandClazz(() -> Types.resolved.t_u64              );
  public final OnDemandClazz f32         = new OnDemandClazz(() -> Types.resolved.t_f32              );
  public final OnDemandClazz f64         = new OnDemandClazz(() -> Types.resolved.t_f64              );
  public final OnDemandClazz Any         = new OnDemandClazz(() -> Types.resolved.t_Any              );
  public final OnDemandClazz Const_String= new OnDemandClazz(() -> Types.resolved.t_Const_String     );
  public final OnDemandClazz Const_String_utf8_data= new OnDemandClazz(() -> Types.resolved.f_Const_String_utf8_data.selfType());
  public final OnDemandClazz String      = new OnDemandClazz(() -> Types.resolved.t_String           );
  public final OnDemandClazz c_unit      = new OnDemandClazz(() -> Types.resolved.t_unit             );
  public final OnDemandClazz error       = new OnDemandClazz(() -> Types.t_ERROR                     )
    {
      public Clazz get()
      {
        if (CHECKS) check
          (Errors.any());
        return super.get();
      }
    };
  public final OnDemandClazz undefined   = new OnDemandClazz(() -> Types.t_UNDEFINED                 );
  public Clazz Const_StringUtf8Data;      // field Const_String.utf8_data
  public Clazz constStringInternalArray;  // field Const_String.internal_array
  public Clazz fuzionJavaObject;          // clazz representing a Java Object in Fuzion
  public Clazz fuzionJavaObject_Ref;      // field fuzion.java.Java_Object.Java_Ref
  public Clazz fuzionSysPtr;              // internal pointer type
  public Clazz fuzionSysArray_u8;         // result clazz of Const_String.array.internal_array
  public Clazz fuzionSysArray_u8_data;    // field fuzion.sys.array<u8>.data
  public Clazz fuzionSysArray_u8_length;  // field fuzion.sys.array<u8>.length
  public Clazz c_address;                 // clazz representing address type
  public Clazz c_error;                   // clazz representing error-feature


  /**
   * NYI: This will eventually be part of a Fuzion IR Config class.
   */
  public FuzionOptions _options_;


  /*----------------------------  variables  ----------------------------*/


  /**
   * Flag that is set during runtime execution to make sure there are no classes
   * created accidentally during runtime.
   */
  boolean closed = false;


  /**
   * Collection of actions to be performed during findClasses phase when it is
   * found that a feature is called dynamically: Then this features needs to be
   * added to the dynamic binding data of heir classes of f.outer).
   */
  private TreeMap<AbstractFeature, List<Runnable>> _whenCalledDynamically_ = new TreeMap<>();
  TreeMap<Clazz, List<Runnable>> _whenCalled_ = new TreeMap<>();


  /**
   * Set of features that are called dynamically. Populated during findClasses
   * phase.
   */
  private TreeSet<FeatureAndActuals> _calledDynamically_ = new TreeSet<>();


  /*--------------------------  constructors  ---------------------------*/



  /*-----------------------------  methods  -----------------------------*/


  /**
   * Initialize Clazzes with given Options.
   */
  public void init(FuzionOptions options)
  {
    _options_ = options;
    universe.get();
  }


  /**
   * Find the unique instance of a clazz.
   *
   * @param c a Clazz
   *
   * @return in case a class equal to c was interned before, returns that
   * existing clazz, otherwise returns c.
   */
  public Clazz intern(Clazz c)
  {
    if (PRECONDITIONS) require
      (Errors.any() || c._type != Types.t_ERROR);

    Clazz existing = clazzes.get(c);
    if (existing == null)
      {
        clazzes.put(c, c);
        existing = c;
      }

    return existing;
  }


  /**
   * Create a clazz for the given actual type and the given outer clazz.
   * Clazzes created are recorded to be handed by findAllClasses.
   *
   * @param actualType the type of the clazz, must be free from generics
   *
   * @param outer the runtime clazz of the outer feature of
   * actualType.feature.
   *
   * @return the existing or newly created Clazz that represents actualType
   * within outer.
   */
  public Clazz create(AbstractType actualType, Clazz outer)
  {
    return create(actualType, -1, outer);
  }


  /**
   * Create a clazz for the given actual type and the given outer clazz.
   * Clazzes created are recorded to be handed by findAllClasses.
   *
   * @param actualType the type of the clazz, must be free from generics
   *
   * @param select in case actualType is a field with open generic result, this
   * chooses the actual field from outer's actual generics. -1 otherwise.
   *
   * @param outer the runtime clazz of the outer feature of
   * actualType.feature.
   *
   * @return the existing or newly created Clazz that represents actualType
   * within outer. undefined.getIfCreated() in case the created clazz cannot
   * exist (due to precondition `T : x` where type typerameter `T` is not
   * constraintAssignableFrom `x`.
   */
  public Clazz create(AbstractType actualType, int select, Clazz outer)
  {
    if (PRECONDITIONS) require
      (Errors.any() || !actualType.dependsOnGenericsExceptTHIS_TYPE(),
       Errors.any() || !actualType.containsThisType(),
       Errors.any() || outer == null || outer._type != Types.t_UNDEFINED);

    Clazz o = outer;
    var ao = actualType.feature().outer();
    while (o != null)
      {
        if (actualType.isRef() && ao != null && ao.inheritsFrom(o.feature()) && !outer.isRef())
          {
            outer = o;  // short-circuit outer relation if suitable outer was found
          }

        if (o._type.compareTo(actualType) == 0 &&
            // example where the following logic is relevant:
            // `((Unary i32 i32).compose i32).#fun`
            // here `compose i32` is not a constructor but a normal routine.
            // `compose i32` does not define a type. Thus it will not lead
            // to a recursive value type.
            actualType.feature().definesType() &&
            actualType != Types.t_ERROR &&
            // a recursive outer-relation

            // This is a little ugly: we do not want outer to be a value
            // type in the source code (see tests/inheritance_negative for
            // reasons why), but we are fine if outer is an 'artificial'
            // value type that is created by Clazz.asValue(), since these
            // will never be instantiated at runtime but are here only for
            // the convenience of the backend.
            //
            // So instead of testing !o.isRef() we use
            // !o._type.feature().isThisRef().
            !o._type.feature().isThisRef() &&
            !o._type.feature().isIntrinsic())
          {  // but a recursive chain of value types is not permitted

            // NYI: recursive chain of value types should be detected during
            // types checking phase!
            StringBuilder chain = new StringBuilder();
            chain.append("1: "+actualType+" at "+actualType.declarationPos().show()+"\n");
            int i = 2;
            Clazz c = outer;
            while (c._type.compareTo(actualType) != 0)
              {
                chain.append(""+i+": "+c._type+" at "+c._type.declarationPos().show()+"\n");
                c = c._outer;
                i++;
              }
            chain.append(""+i+": "+c._type+" at "+c._type.declarationPos().show()+"\n");
            Errors.error(actualType.declarationPos(),
                         "Recursive value type is not allowed",
                         "Value type " + actualType + " equals type of outer feature.\n"+
                         "The chain of outer types that lead to this recursion is:\n"+
                         chain + "\n" +
                         "To solve this, you could add a 'ref' after the arguments list at "+o._type.feature().pos().show());
          }
        o = o._outer;
      }

    // NYI: We currently create new clazzes for every different outer
    // context. This gives us plenty of opportunity to specialize the code,
    // but it might be overkill in some cases. We might rethink this and,
    // e.g. treat clazzes of inherited features with a reference outer clazz
    // the same.

    Clazz result = null, newcl = null;

    // find preconditions `T : x` that prevent creation of instances of this clazz.
    //
    // NYI: UNDER DEVELOPMENT: This is very manual code to extract this info
    // from the code created for the preFeature. This is done automatically by
    // DFA, so this code will disappear once DFA and AIR phases are merged.
    //
    var pF = actualType.feature().preFeature();
    if (pF != null)
      {
        var pFcode = pF.code();
        var ass0 = pFcode instanceof AbstractBlock b ? b._expressions.get(0) : pFcode;
        if (ass0 instanceof AbstractAssign ass)
          {
            var e0 = ass._value;
            var e1 = e0 instanceof AbstractBlock ab ? ab._expressions.get(0) :
                     e0 instanceof AbstractCall ac  ? ac.target() :
                     e0;
            if (e1 instanceof AbstractBlock ab &&
                ab._expressions.get(0) instanceof AbstractMatch m &&
                m.subject() instanceof AbstractCall sc &&
                sc.calledFeature() == Types.resolved.f_Type_infix_colon)
              {
                var pFc = outer.lookup(pF);
                if (clazzesToBeVisited.contains(pFc))
                  {
                    clazzesToBeVisited.remove(pFc);
                    pFc.findAllClasses();
                  }
                var args = pFc.actualClazzes(sc, null);
                if (CHECKS)
                  check(args[0].feature() == Types.resolved.f_Type_infix_colon_true  ||
                        args[0].feature() == Types.resolved.f_Type_infix_colon_false   );
                if (args[0].feature() == Types.resolved.f_Type_infix_colon_false)
                  {
                    result = undefined.get();
                  }
              }
          }
      }

    if (result == null)
      {
        newcl = new Clazz(actualType, select, outer);
        result = newcl;
        if (actualType != Types.t_UNDEFINED)
          {
            result = intern(newcl);
          }
      }

    if (result == newcl)
      {
        if (CHECKS) check
          (Errors.any() || result.feature().state().atLeast(State.RESOLVED));
        if (result.feature().state().atLeast(State.RESOLVED))
          {
            clazzesToBeVisited.add(result);
          }
        result.registerAsHeir();
        if (_options_.verbose(5))
          {
            _options_.verbosePrintln(5, "GLOBAL CLAZZ: " + result);
            if (_options_.verbose(10))
              {
                Thread.dumpStack();
              }
          }
        result.dependencies();
      }

    if (POSTCONDITIONS) ensure
      (Errors.any() || actualType == Types.t_ADDRESS || actualType.compareToIgnoreOuter(result._type) == 0 || true,
       outer == result._outer || true /* NYI: Check why this sometimes does not hold */);

    return result;
  }


  /**
   * As long as there are clazzes that were created via create(), call
   * findAllClasses on that clazz and layout the class.
   *
   * Once this returns, all runtime classes required during execution have been
   * created.
   */
  public void findAllClasses(Clazz main)
  {
    var toLayout = new LinkedList<Clazz>();

    // make sure internally referenced clazzes do exist:
    Any.get();
    var c_universe = universe.get();
    c_universe.called(SourcePosition.builtIn);
    c_universe.instantiated(SourcePosition.builtIn);
    c_address = create(Types.t_ADDRESS, c_universe);

    // mark internally referenced clazzes as called or instantiated:
    if (CHECKS) check
      (Errors.any() || main != null);
    if (main != null)
      {
        main.called(SourcePosition.builtIn);
        main.instantiated(SourcePosition.builtIn);
      }
    Const_StringUtf8Data           = Const_String_utf8_data.get();
    fuzionSysArray_u8              = Const_StringUtf8Data.resultClazz().fields()[0].resultClazz();
    fuzionSysArray_u8_data         = fuzionSysArray_u8.lookup(Types.resolved.f_fuzion_sys_array_data  , SourcePosition.builtIn);
    fuzionSysArray_u8_length       = fuzionSysArray_u8.lookup(Types.resolved.f_fuzion_sys_array_length, SourcePosition.builtIn);
    fuzionSysPtr                   = fuzionSysArray_u8_data.resultClazz();
    var fuzion                     = universe.get().lookup(Types.resolved.f_fuzion, SourcePosition.builtIn);
    var fuzionJava                 = fuzion.lookup(Types.resolved.f_fuzion_java, SourcePosition.builtIn);
    fuzionJavaObject               = fuzionJava.lookup(Types.resolved.f_fuzion_Java_Object, SourcePosition.builtIn);
    fuzionJavaObject_Ref           = fuzionJavaObject.lookup(Types.resolved.f_fuzion_Java_Object_Ref, SourcePosition.builtIn);
    c_error                        = universe.get().lookup(Types.resolved.f_error, SourcePosition.builtIn);

    while (!clazzesToBeVisited.isEmpty())
      {
        Clazz cl = clazzesToBeVisited.removeFirst();

        cl.findAllClasses();
        if (!cl.feature().isField())
          {
            toLayout.add(cl);
          }

        while (clazzesToBeVisited.isEmpty() && !toLayout.isEmpty())
          {
            toLayout.removeFirst().layoutAndHandleCycle();
            /* NYI: There are very few fields for which layout() causes the
             * creation of new clazzes. Examples are some inherited outer refs
             * and i32.val in case there is a user defined feature inheriting
             * from i32.  We might want to make sure that these are also
             * found before the layout phase.
             */
            if (!clazzesToBeVisited.isEmpty() && _options_.verbose(1))
              {
                Errors.warning("New clazz created during layout phase: "+clazzesToBeVisited.get(0));
              }
          }
      }
    if (CHECKS) check
      (clazzesToBeVisited.size() == 0);
    closed = true;
  }


  /**
   * When it is detected that f is called dynamically, execute r.run().
   */
  void whenCalledDynamically(AbstractFeature f,
                                    Runnable r)
  {
    if (isCalledDynamically(f))
      {
        r.run();
      }
    else
      {
        var l = _whenCalledDynamically_.get(f);
        if (l == null)
          {
            l = new List<Runnable>(r);
            _whenCalledDynamically_.put(f, l);
          }
        else
          {
            l.add(r);
          }
      }
  }


  /**
   * Remember that f is called dynamically.  In case f was not known to be
   * called dynamically, execute all the runnables registered for f by
   * whenCalledDynamically.
   */
  void calledDynamically(AbstractFeature f, List<AbstractType> tp)
  {
    if (PRECONDITIONS) require
      (Errors.any() || isUsed(f) || true /* NYI: clazzes are created for type features's type parameters without being called,
                                                * see tests/reg_issue1236 for an example. We might treat clazzes that are only used
                                                * in types differently.
                                                */);

    var ft = new FeatureAndActuals(f, tp);
    var added = _calledDynamically_.add(ft);
    if (added)
      {
        var l = _whenCalledDynamically_.remove(f);
        if (l != null)
          {
            for (var r : l)
              {
                r.run();
              }
          }
      }

    if (POSTCONDITIONS) ensure
      (isCalledDynamically(f));
  }


  /**
   * Has f been found to be called dynamically?
   */
  boolean isCalledDynamically(AbstractFeature f)
  {
    return !calledDynamicallyWithTypePars(f).isEmpty();
  }

  /**
   * Has f been found to be called dynamically?
   */
  Set<FeatureAndActuals> calledDynamicallyWithTypePars(AbstractFeature f)
  {
    var fmin = new FeatureAndActuals(f, false);
    var fmax = new FeatureAndActuals(f, true);
    return _calledDynamically_.subSet(fmin, fmax);
  }


  /**
   * Print statistics on clazzes defined per feature, for verbose output.
   */
  public void showStatistics()
  {
    if (_options_.verbose(1))
      {
        int fields = 0;
        int routines = 0;
        int clazzesForFields = 0;
        Map<AbstractFeature, List<Clazz>> clazzesPerFeature = new TreeMap<>();
        for (var cl : clazzes.keySet())
          {
            var f = cl.feature();
            var l = clazzesPerFeature.get(f);
            if (l == null)
              {
                l = new List<>();
              }
            l.add(cl);
            clazzesPerFeature.put(f, l);
            if (f.isField())
              {
                clazzesForFields++;
                if (l.size() == 1)
                  {
                    fields++;
                  }
              }
            else
              {
                if (l.size() == 1)
                  {
                    routines++;
                  }
              }
          }
        if (_options_.verbose(2))
          {
            for (var e : clazzesPerFeature.entrySet())
              {
                var f = e.getKey();
                String fn = (f.isField() ? "field " : "routine ") + f.qualifiedName();
                say(""+e.getValue().size()+" classes for " + fn);
                if (_options_.verbose(5))
                  {
                    int i = 0;
                    for (var c : e.getValue() )
                      {
                        i++;
                        say(""+i+"/"+e.getValue().size()+" classes for " + fn + ": " + c);
                      }
                  }
              }
          }
        say("Found "+Clazzes.instance.num()+" clazzes (" +
                           clazzesForFields + " for " + fields+ " fields, " +
                           (clazzes.size()-clazzesForFields) + " for " + routines + " routines).");
      }
  }


  /**
   * Obtain a set of all clazzes.
   */
  public Set<Clazz> all()
  {
    return clazzes.keySet();
  }


  /**
   * Return the total number of unique runtime clazzes stored globally.
   */
  public int num()
  {
    return clazzes.size();
  }


  /*-----------------  methods to find runtime Clazzes  -----------------*/


  /**
   * Find all clazzes for this case and store them in outerClazz.
   */
  public void findClazzes(AbstractAssign a, AbstractFeature outer, Clazz outerClazz, List<AbstractCall> inh)
  {
    if (PRECONDITIONS) require
      (a != null, outerClazz != null);

    if (CHECKS) check
      (Errors.any() || a._target != null);

    if (a._target != null)
      {
        Clazz sClazz = clazz(a._target, outerClazz, inh);
        var vc = sClazz.asValue();
        var fc = vc.lookup(a._assignedField, a);
        propagateExpectedClazz(a._value, fc.resultClazz(), outer, outerClazz, inh);
        if (!outerClazz.hasActualClazzes(a, outer))
          {
            outerClazz.saveActualClazzes(a, outer,
                                         new Clazz[] { sClazz,
                                                       isUsed(a._assignedField) ? fc : null,
                                                       fc.resultClazz()
                                         });
          }
      }
  }


  /**
   * propagate the expected clazz of an expression.  This is used to find the
   * result type of Box() expressions that are a NOP if the expected type is a
   * value type or the boxed type is already a ref type, while it performs
   * boxing if a value type is used as a ref.
   *
   * @param e the expression we are propagating the expected clazz into
   *
   * @param ec the expected result clazz of e
   *
   * @param outerClazz the current clazz
   *
   * @param inh the inheritance chain that brought the code here (in case it is
   * an inlined inherits call).
   */
  void propagateExpectedClazz(Expr e, Clazz ec, AbstractFeature outer, Clazz outerClazz, List<AbstractCall> inh)
  {
    if (e instanceof Box b)
      {
        if (!outerClazz.hasActualClazzes(b, outer))
          {
            Clazz vc = clazz(b._value, outerClazz, inh);
            Clazz rc = vc;
            if (asRefAssignable(ec, vc))
              {
                rc = vc.asRef();
                if (CHECKS) check
                  (Errors.any() || ec._type.isAssignableFrom(rc._type, Context.NONE));
              }
            outerClazz.saveActualClazzes(b, outer, new Clazz[] {vc, rc});
            if (vc != rc)
              {
                rc.instantiated(b);
              }
            else
              {
                propagateExpectedClazz(b._value, ec, outer, outerClazz, inh);
              }
          }
      }
    else if (e instanceof AbstractBlock b)
      {
        var s = b._expressions;
        if (!s.isEmpty())
          {
            propagateExpectedClazz(s.getLast(), ec, outer, outerClazz, inh);
          }
      }
    else if (e instanceof Tag t)
      {
        propagateExpectedClazz(t._value, ec, outer, outerClazz, inh);
      }
  }


  /*
   * Is vc.asRef assignable to ec?
   */
  private boolean asRefAssignable(Clazz ec, Clazz vc)
  {
    return asRefDirectlyAssignable(ec, vc) || asRefAssignableToChoice(ec, vc);
  }


  /*
   * Is vc.asRef directly assignable to ec, i.e. without the need for tagging?
   */
  private boolean asRefDirectlyAssignable(Clazz ec, Clazz vc)
  {
    return ec.isRef() && ec._type.isAssignableFrom(vc.asRef()._type, Context.NONE);
  }


  /*
   * Is ec a choice and vc.asRef assignable to ec?
   */
  private boolean asRefAssignableToChoice(Clazz ec, Clazz vc)
  {
    return ec._type.isChoice() &&
      !ec._type.isAssignableFrom(vc._type, Context.NONE) &&
      ec._type.isAssignableFrom(vc._type.asRef(), Context.NONE);
  }


  /**
   * Find the mapping from all calls to actual frame clazzes
   *
   * In an inheritance clause of the form
   *
   *   o<p,q>
   *   {
   *     a<x,y> : b<x,p>.c<y,q>;
   *
   *     d<x,y> { e<z> { } };
   *     d<i32,p>.e<bool>;
   *   }
   *
   * for the call b<x,p>.c<y,q>, the outerClazz is a<x,y>, while the frame for
   * b<x,p>.c<y,q> will be created with outerClazz.outer, i.e., o<p,q>.
   *
   * In contrast, for the call to e in d<i32,p>.e<bool>, outerClazz is d<x,y>
   * and will be used both as frame clazz for d<i32,p> and as the context for
   * call to e<z>.
   */
  public void findClazzes(AbstractCall c, AbstractFeature outer, Clazz outerClazz, List<AbstractCall> inh)
  {
    if (PRECONDITIONS) require
      (Errors.any() || c.calledFeature() != null && c.target() != null);

    if (c.calledFeature() == null  || c.target() == null)
      {
        return;  // previous errors, give up
      }

    var tclazz  = clazz(c.target(), outerClazz, inh);
    var cf      = c.calledFeature();
    var callToOuterRef = c.target().isCallToOuterRef();
    boolean dynamic = c.isDynamic() && (tclazz.isRef() || callToOuterRef);
    if (callToOuterRef)
      {
        tclazz._isCalledAsOuter = true;
      }
    var typePars = outerClazz.actualGenerics(c.actualTypeParameters());
    if (!tclazz.isVoidOrUndefined())
      {
        if (dynamic)
          {
            calledDynamically(cf, typePars);
          }

        var innerClazz        = tclazz.lookup(new FeatureAndActuals(cf, typePars), c.select(), c, c.isInheritanceCall());
        if (outerClazz.hasActualClazzes(c, outer))
          {
            // NYI: #2412: Check why this is done repeatedly and avoid redundant work!
            //  say("REDUNDANT save for "+innerClazz+" to "+outerClazz+" at "+c.pos().show());
          }
        else
          {
            if (c.calledFeature() == Types.resolved.f_Type_infix_colon)
              {
                var T = innerClazz.actualGenerics()[0];
                cf = T._type.constraintAssignableFrom(Context.NONE, tclazz._type.generics().get(0))
                  ? Types.resolved.f_Type_infix_colon_true
                  : Types.resolved.f_Type_infix_colon_false;
                innerClazz = tclazz.lookup(new FeatureAndActuals(cf, typePars), -1, c, c.isInheritanceCall());
              }
            outerClazz.saveActualClazzes(c, outer, new Clazz[] {innerClazz, tclazz});
          }

        if (innerClazz._type != Types.t_UNDEFINED)
          {
            var afs = innerClazz.argumentFields();
            var i = 0;
            for (var a : c.actuals())
              {
                if (CHECKS) check
                  (Errors.any() || i < afs.length);
                if (i < afs.length) // actuals and formals may mismatch due to previous errors,
                                    // see tests/typeinference_for_formal_args_negative
                  {
                    propagateExpectedClazz(a, afs[i].resultClazz(), outer, outerClazz, inh);
                  }
                i++;
              }
          }

        var f = innerClazz.feature();
        if (f.kind() == AbstractFeature.Kind.TypeParameter)
          {
            var tpc = innerClazz.resultClazz();
            do
              {
                addUsedFeature(tpc.feature(), c.pos());
                tpc.instantiated(c.pos());
                tpc = tpc._outer;
              }
            while (tpc != null && !tpc.feature().isUniverse());
          }
      }
  }


  /**
   * Find actual clazzes used by a constant expression
   *
   * @param c the constant
   *
   * @param outerClazz the surrounding clazz
   */
  public void findClazzes(Constant c, AbstractFeature outer, Clazz outerClazz, List<AbstractCall> inh)
  {
    if (PRECONDITIONS) require
      (c != null, outerClazz != null);

    if (!outerClazz.hasActualClazzes(c, outer))
      {
        var p = c.pos();
        var const_clazz = clazz(c, outerClazz, inh);
        outerClazz.saveActualClazzes(c, outer, new Clazz[] {const_clazz});
        const_clazz.instantiated(p);
        if (const_clazz.feature() == Types.resolved.f_array)
          { // add clazzes touched by constant creation:
            //
            //   array.internal_array
            //   fuzion.sys.internal_array
            //   fuzion.sys.internal_array.data
            //   fuzion.sys.Pointer
            //
            var array          = const_clazz;
            var internal_array = array.lookup(Types.resolved.f_array_internal_array);
            var sys_array      = internal_array.resultClazz();
            var data           = sys_array.lookup(Types.resolved.f_fuzion_sys_array_data);
            array.instantiated(p);
            sys_array.instantiated(p);
            data.resultClazz().instantiated(p);
          }
      }
  }


  /**
   * Find all clazzes for this case and store them in outerClazz.
   */
  public void findClazzes(AbstractCase c, AbstractFeature outer, Clazz outerClazz, List<AbstractCall> inh)
  {
    // NYI: Check if this works for a case that is part of an inherits clause, do
    // we need to store in outerClazz.outer?
    var f = c.field();
    var t = c.types();
    if ((f != null || t != null) &&  !outerClazz.hasActualClazzes(c, outer))
      {
        Clazz[] acl;
        if (f != null)
          {
            var fOrFc = isUsed(f)
              ? outerClazz.lookup(f)
              : Clazzes.instance.clazz(outerClazz._type.actualType(f.resultType(), Context.NONE)); // NYI: better Clazzes.instance.c_void.get(), does not work in interpreter backend yet...
            acl = new Clazz[] {fOrFc};
          }
        else
          {
            acl = new Clazz[t.size()];
            int i = 0;
            for (var caseType : t)
              {
                acl[i] = outerClazz.handDown(caseType, inh, c);
                i++;
              }
          }
        outerClazz.saveActualClazzes(c, outer, acl);
      }
  }


  /**
   * Find all clazzes for this case and store them in outerClazz.
   */
  public void findClazzes(AbstractMatch m, AbstractFeature outer, Clazz outerClazz, List<AbstractCall> inh)
  {
    if (!outerClazz.hasActualClazzes(m, outer))
      {
        var subjClazz = clazz(m.subject(), outerClazz, inh);
        outerClazz.saveActualClazzes(m, outer, new Clazz[] {subjClazz});
      }
  }


  /**
   * Find all clazzes for this Tag and store them in outerClazz.
   */
  public void findClazzes(Tag t, AbstractFeature outer, Clazz outerClazz, List<AbstractCall> inh)
  {
    if (!outerClazz.hasActualClazzes(t, outer))
      {
        Clazz vc = clazz(t._value, outerClazz, inh);
        var tc = outerClazz.handDown(t._taggedType, inh, t);
        outerClazz.saveActualClazzes(t, outer, new Clazz[] { vc, tc });
        tc.instantiated(t);
      }
  }


  /**
   * Find all clazzes for this array and store them in outerClazz.
   */
  public void findClazzes(InlineArray i, AbstractFeature outer, Clazz outerClazz, List<AbstractCall> inh)
  {
    if (!outerClazz.hasActualClazzes(i, outer))
      {
        Clazz ac = clazz(i, outerClazz, inh);
        outerClazz.saveActualClazzes(i, outer, new Clazz[] {ac});
      }
  }


  /**
   * Find all clazzes for this Env and store them in outerClazz.
   */
  public void findClazzes(Env v, AbstractFeature outer, Clazz outerClazz, List<AbstractCall> inh)
  {
    if (!outerClazz.hasActualClazzes(v, outer))
      {
        Clazz ac = clazz(v, outerClazz, inh);
        outerClazz.saveActualClazzes(v, outer, new Clazz[] {ac});
      }
  }


  /**
   * Determine the result clazz of an Expr.
   *
   * @param inh the inheritance chain that brought the code here (in case it is
   * an inlined inherits call).
   */
  public Clazz clazz(Expr e, Clazz outerClazz, List<AbstractCall> inh)
  {
    Clazz result;
    if (e instanceof AbstractBlock b)
      {
        Expr resExpr = b.resultExpression();
        result = resExpr != null ? clazz(resExpr, outerClazz, inh)
                                 : c_unit.get();
      }
    else if (e instanceof Box b)
      {
        result = outerClazz.handDown(b.type(), inh, e);
      }

    else if (e instanceof AbstractCall c)
      {
        var tclazz = clazz(c.target(), outerClazz, inh);
        if (!tclazz.isVoidOrUndefined())
          {
            var at = outerClazz.handDownThroughInheritsCalls(c.actualTypeParameters(), inh);
            var inner = tclazz.lookup(new FeatureAndActuals(c.calledFeature(),
                                                            outerClazz.actualGenerics(at)),
                                      c.select(),
                                      c,
                                      false);
            result = inner.resultClazz();
          }
        else
          {
            result = tclazz;
          }
      }
    else if (e instanceof AbstractCurrent c)
      {
        result = outerClazz;
      }

    else if (e instanceof AbstractMatch m)
      {
        result = outerClazz.handDown(m.type(), inh, e);
      }

    else if (e instanceof Universe)
      {
        result = universe.get();
      }

    else if (e instanceof Constant c)
      {
        result = outerClazz.handDown(c.typeOfConstant(), inh, e);
      }

    else if (e instanceof Tag tg)
      {
        result = outerClazz.handDown(tg._taggedType, inh, e);
      }

    else if (e instanceof InlineArray ia)
      {
        result = outerClazz.handDown(ia.type(), inh, e);
      }

    else if (e instanceof Env v)
      {
        result = outerClazz.handDown(v.type(), inh, e);
      }

    else
      {
        if (!Errors.any())
          {
            throw new Error("" + e.getClass() + " should no longer exist at runtime");
          }

        result = error.get(); // dummy class
      }

    if (POSTCONDITIONS) ensure
      (result != null);

    return result;
  }


  /*----------------  methods to convert type to clazz  -----------------*/


  /**
   * clazz
   *
   * @return
   */
  public Clazz clazz(AbstractType thiz)
  {
    if (PRECONDITIONS) require
      (Errors.any() || !thiz.dependsOnGenerics(),
       !thiz.isThisType());

    var result = _clazzesForTypes_.get(thiz);
    if (result == null)
      {
        Clazz outerClazz = thiz.outer() != null
          ? outerClazz = clazz(thiz.outer())
          : null;

        result = create(thiz, outerClazz);
        _clazzesForTypes_.put(thiz, result);
      }

    if (POSTCONDITIONS) ensure
      (Errors.any() || thiz.isRef() == result._type.isRef());

    return result;
  }


  /**
   * clazzWithSpecificOuter creates a clazz from this type with a specific outer
   * clazz that is inserted as the outer clazz for the outermost type that was
   * explicitly given in the source code.
   *
   * @param thiz the type of the clazz, must be free from generics
   *
   * @param select in case thiz is a field with open generic result, this
   * chooses the actual field from outer's actual generics. -1 otherwise.
   *
   * @param outerClazz the outer clazz
   *
   * @return the corresponding Clazz.
   */
  public Clazz clazzWithSpecificOuter(AbstractType thiz, int select, Clazz outerClazz)
  {
    if (PRECONDITIONS) require
      (Errors.any() || !thiz.dependsOnGenericsExceptTHIS_TYPE(),
       outerClazz != null || thiz.feature().outer() == null,
       (outerClazz.feature().isTypeFeature() /* NYI: REMOVE: workaround for #3160 */) ||
       Errors.any() || thiz == Types.t_ERROR || outerClazz == null || outerClazz.feature().inheritsFrom(thiz.feature().outer()));

    var result = create(thiz, select, outerClazz);

    return result;
  }


  /*-------------  methods for clazzes related to features  -------------*/


  /**
   * NYI: recycle this comment whose method has disappeared.
   *
   * thisClazz returns the clazz of this feature's frame object.  This can be
   * called even if !hasThisType() since thisClazz() is used also for abstract
   * or intrinsic feature to determine the resultClazz().
   *
   * Depending on the generics of this and its outer features, we consider the
   * following cases:
   *
   * a.b.c.f
   *
   *   A feature with no generic arguments and no generic outer features,
   *   there is exactly one clazz for a.b.c.f's frame that can be used for all
   *   calls to f. outerClazz is not really needed in this case.
   *
   * a.b.c.f<A,B>
   *
   *   A feature with generic arguments and no generic outer features has
   *   exactly one clazz for each set of actual generic arguments <X,Y> used
   *   at any call site.
   *
   * a.b<A,B>.c<C>.d
   *
   *   A feature with no generic arguments but generic outer features has
   *   exactly one clazz for each set of actual generic arguments <X,Y>,<Z>
   *   for its outer features used at any call site.
   *
   * a.b<A,B>.c<C>.f<D,E>
   *
   *   A feature with generic arguments and generic outer features has one
   *   clazz for each complete set of actual generic arguments <V,W>,<X>,<Y,Z>
   *   used at any call site.
   *
   * a.b.c.f : g<x,y>.h<z>
   *
   *   For a feature f that inherits from a generic feature g.h, the inherits
   *   clause specifies actual generic arguments to g and g.h and these actual
   *   generic argument may contain only the formal generic arguments of
   *   a.b.c.f.  Consequently, the presence of generics in the parent feature
   *   does not add any new clazzes.
   *
   * The complete set of actual generics of a feature including all actual
   * generics of all outer features will be called the generic signature s of
   * a call.
   *
   * Note that a generic signature <V,W>,<X>,<Y,Z> cannot be flattened to
   * <V,W,X,Y,Z> since formal generic lists can be open, i.e, they do not have
   * a fixed length.
   *
   * So, essentially, we need one clazz for each (f,s) where f is a feature
   * and s is any generic signatures used in calls.
   *
   * Since every call is performed in the code of a feature that is executed
   * for an actual clazz (caller), we need a mapping
   *
   *  caller x call -> callee
   *
   * that gives the actual class to be called.
   *
   * Special thought is required for calls in an inherits clause of a feature:
   * Since calls to parent features operate on the same data, so they should
   * be performed using the same clazz. I.e., the mapping caller x call ->
   * callee also has to include all calls performed in any parent features.
   *
   * @param thiz the feature whose clazz we create
   *
   * @param actualGenerics the actual generics arguments
   *
   * @param outerClazz the clazz of this.outer(), null for universe
   *
   * @return this feature's frame clazz
   */


  /**
   * Has this feature been found to be used?
   */
  public boolean isUsed(AbstractFeature thiz)
  {
    return thiz._usedAt != null;
  }


  /**
   * Has this feature been found to be used?
   */
  public HasSourcePosition isUsedAt(AbstractFeature thiz)
  {
    return thiz._usedAt;
  }


  /**
   * Add f to the set of used features, record at as the position of the first
   * use.
   */
  public void addUsedFeature(AbstractFeature f, HasSourcePosition at)
  {
    f._usedAt = at;
  }

  /**
   * reset the instance and set closed to false again
   */
  public void reset()
  {
    instance = new Clazzes();
  }


}

/* end of file */
