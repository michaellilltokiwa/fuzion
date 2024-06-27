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
 * Source of class Contract
 *
 *---------------------------------------------------------------------*/

package dev.flang.ast;

import dev.flang.util.ANY;
import dev.flang.util.Errors;
import dev.flang.util.FuzionConstants;
import dev.flang.util.List;
import dev.flang.util.SourceRange;
import dev.flang.util.SourcePosition;


/**
 * Contract <description>
 *
 * @author Fridtjof Siebert (siebert@tokiwa.software)
 */
public class Contract extends ANY
{

  /*----------------------------  constants  ----------------------------*/


  /**
   * Empty list of conditions.
   */
  static final List<Cond> NO_COND = new List<>();
  static { NO_COND.freeze(); }


  /**
   * Empty contract. Note that this is currently not useable in the general case
   * since a feature that does not define a contract may still inherit one from
   * those features it redefines, which will require the `argsSupplier` to be
   * set.
   */
  public static final Contract EMPTY_CONTRACT = new Contract(NO_COND, NO_COND, null, null,
                                                             NO_COND, null, null,
                                                             null);


  /*--------------------------  static fields  --------------------------*/


  /**
   * Id used to generate unique names for pre- and postcondution features.
   */
  public static int _id_ = 0;


  /**
   * Reset static fields
   */
  public static void reset()
  {
    _id_ = 0;
  }


  /*----------------------------  variables  ----------------------------*/


  /**
   * List of declared preconditions in this contract. This might be null if
   * there is no `pre` clause or it might be NO_COND if there is a `pre` clause
   * with no conditions.
   *
   * This does not include inherited preconditions.
   */
  public List<Cond> _declared_preconditions;


  /**
   * Duplicate of _declared_preconditions needed to generate code for the pre
   * bool feature.
   */
  public List<Cond> _declared_preconditions2;


  /**
   * List of declared postconditions in this contract. This might be null if
   * there is no `post` clause or it might be NO_COND if there is a `post`
   * clause with no conditions.
   *
   * This does not include inherited postconditions.
   */
  public List<Cond> _declared_postconditions;


  /**
   * Supplier that re-parses the arguments since we will have to create clones
   * of the declared formal arguments to be used in pre feature, pre bool
   * feature. pre_and_call feature and post feature.
   */
  java.util.function.Supplier<List<AbstractFeature>> _argsSupplier;


  /**
   * Did the parser find `pre` / `post` or even `pre else` / `post then` ? These
   * might be present even if the condition list is NO_COND.
   */
  public final SourceRange _hasPre,     _hasPost;
  public final SourceRange _hasPreElse, _hasPostThen;


  /**
   * Cached names of pre and postcondition features.
   */
  private String _preConditionFeatureName = null;
  private String _preBoolConditionFeatureName = null;
  private String _preConditionAndCallFeatureName = null;
  private String _postConditionFeatureName = null;


  /*--------------------------  constructors  ---------------------------*/


  /**
   * Constructor for a contract
   *
   * @param r1, r2 the preconditions, parsed twice since we will need them
   * twice.  null if not present.
   *
   * @param hasPre if `pre` was found, this gives its position, otherwise it is null.
   *
   * @param hasElse if `else` after `pre` was found, this gives its prosition,
   * otherwise it is null.
   *
   * @param e the postcondition or null if not present.
   *
   * @param hasPost if `post` was found, this gives its position, otherwise it is null
   *
   * @param hasThen if `then` after `post` was found, this gives its prosition,
   * otherwise it is null.
   *
   * @param args supplier that forks of the parser to re-parse the formal
   * arguments to be used as arguments for pre and post features.
   */
  public Contract(List<Cond> r1, List<Cond> r2, SourceRange hasPre,  SourceRange hasElse,
                  List<Cond> e,                 SourceRange hasPost, SourceRange hasThen,
                  java.util.function.Supplier<List<AbstractFeature>> args)
  {
    _hasPre  = hasPre;
    _hasPost = hasPost;
    _hasPreElse  = hasElse;
    _hasPostThen = hasThen;
    _declared_preconditions   = r1 == null || r1.isEmpty() ? NO_COND : r1;
    _declared_preconditions2  = r2 == null || r2.isEmpty() ? NO_COND : r2;
    _declared_postconditions = e == null || e.isEmpty() ? NO_COND : e;
    _argsSupplier = args;
  }


  /*--------------------------  static methods  -------------------------*/


  /**
   * Get and cache the name of the pre feature of `f`.
   */
  static String preConditionsFeatureName(Feature f)
  {
    if (PRECONDITIONS) require
      (hasPreConditionsFeature(f));

    var c = f.contract();
    if (c._preConditionFeatureName == null)
      {
        c._preConditionFeatureName = FuzionConstants.PRECONDITION_FEATURE_PREFIX + (_id_++) + "_" + f.featureName().baseName();
      }
    return c._preConditionFeatureName;
  }


  /**
   * Get and cache the name of the pre bool feature of `f`.
   */
  static String preBoolConditionsFeatureName(AbstractFeature f)
  {
    if (PRECONDITIONS) require
      (hasPreConditionsFeature(f));

    var c = f.contract();
    if (c._preBoolConditionFeatureName == null)
      {
        c._preBoolConditionFeatureName = FuzionConstants.PREBOOLCONDITION_FEATURE_PREFIX + (_id_++) + "_"  + f.featureName().baseName();
      }
    return c._preBoolConditionFeatureName;
  }


  /**
   * Get and cache the name of the pre and call feature of `f`.
   */
  static String preConditionsAndCallFeatureName(AbstractFeature f)
  {
    if (PRECONDITIONS) require
      (hasPreConditionsFeature(f));

    var c = f.contract();
    if (c._preConditionAndCallFeatureName == null)
      {
        c._preConditionAndCallFeatureName = FuzionConstants.PREANDCALLCONDITION_FEATURE_PREFIX + (_id_++) + "_" + f.featureName().baseName();
      }
    return c._preConditionAndCallFeatureName;
  }


  /**
   * Get and cache the name of the post feature of `f`.
   */
  static String postConditionsFeatureName(AbstractFeature f)
  {
    if (PRECONDITIONS) require
      (hasPostConditionsFeature(f));

    var c = f.contract();
    if (c._postConditionFeatureName == null)
      {
        c._postConditionFeatureName = FuzionConstants.POSTCONDITION_FEATURE_PREFIX + (_id_++) + "_" + f.featureName().baseName();
      }
    return c._postConditionFeatureName;
  }


  /**
   * Does this contract require a precondition feature due to inherited or
   * declared postconditions?
   *
   * @param f the feature this is the contract of.
   *
   * @return true if a precondition feature has to be created.
   */
  static boolean requiresPreConditionsFeature(Feature f)
  {
    var fc = f.contract();

    return fc._hasPre != null &&
      (!fc._declared_preconditions.isEmpty() || !f._inheritedPre.isEmpty());
  }


  /**
   * Does this contract require a postcondition feature due to inherited or
   * declared postconditions?
   *
   * @param f the feature this is the contract of.
   *
   * @return true if a postcondition feature has to be created.
   */
  static boolean requiresPostConditionsFeature(Feature f)
  {
    var fc = f.contract();

    return !fc._declared_postconditions.isEmpty() || !f._inheritedPost.isEmpty();

  }


  /**
   * Does the given feature either have a precondition feature or, for a
   * `dev.flang.ast.Feature`, will it get one due to inherited or declared pre
   * conditions?
   *
   * @param f a feature
   *
   * @return true if there will be a precondition feature for `f`.
   */
  static boolean hasPreConditionsFeature(AbstractFeature f)
  {
    return f.preFeature() != null || f instanceof Feature ff && requiresPreConditionsFeature(ff);
  }


  /**
   * Does the given feature either have a postcondition feature or, for an
   * dev.flang.ast.Feature, will it get one due to inherited or declared post
   * conditions?
   *
   * @param f a feature
   *
   * @return true if there will be a postcondition feature for `f`.
   */
  static boolean hasPostConditionsFeature(AbstractFeature f)
  {
    return f.postFeature() != null || f instanceof Feature ff && requiresPostConditionsFeature(ff);
  }


  /*-----------------------------  methods  -----------------------------*/


  /**
   * When redefining a feature, the original contract is inherited with
   * preconditions OR-ed and postconditions AND-ed.  This feature records this
   * condition inheritance.
   *
   * @param to the redefining feature that inherits a contract
   *
   * @param from the redefined feature this contract should inherit from.
   */
  public void addInheritedContract(Feature to, AbstractFeature from)
  {
    if (PRECONDITIONS) require
      (this == to.contract());

    if (!to.isUniverse())
      {
        to._inheritedPre.add(from);
      }
    if (hasPostConditionsFeature(from))
      {
        to._inheritedPost.add(from);
      }
  }


  /**
   * Create call to outer's precondition feature
   *
   * @param res resolution instance
   *
   * @param f a feature with a precondition that should be called.
   *
   * @param outer the outer feature the call will be used in.
   *
   * @return a call to f.preFeature() to be added to code of outer.
   */
  static Call callPreCondition(Resolution res, AbstractFeature f, Feature outer)
  {
    var oc = f.contract();
    var p = oc._hasPre != null ? oc._hasPre : f.pos();
    List<Expr> args = new List<>();
    for (var a : outer.valueArguments())
      {
        var ca = new Call(p,
                          new Current(p, outer),
                          a,
                          -1);
        ca = ca.resolveTypes(res, outer);
        args.add(ca);
      }
    return callPreCondition(res, f, outer, args);
  }


  /**
   * Create call to outer's precondition feature to be added to code of feature `outer`.
   *
   * @param res resolution instance
   *
   * @param f a feature with a precondition that should be called.
   *
   * @param outer The call to f's precondition is to be added to outer's code.
   *
   * @param args actual arguments to be passed to the call
   *
   * @return a call to f.preFeature() to be added to code of outer.
   */
  private static Call callPreCondition(Resolution res,
                                       AbstractFeature f,
                                       Feature outer,
                                       List<Expr> args)
  {
    var p = outer.contract()._hasPre != null
          ? outer.contract()._hasPre    // use `pre` position if `outer` is of the form `f pre cc is ...`
          : outer.pos();                // `outer` does not have `pre` clause, only inherits preconditions. So use the feature position instead

    var t = (outer.outerRef() != null) ? new This(p, outer, outer.outer()).resolveTypes(res, outer)
                                       : new Universe();
    if (f instanceof Feature ff)  // if f is currently being compiled, make sure its contract features are created first
      {
        addContractFeatures(ff, res);
      }
    return new Call(p,
                    t,
                    outer.generics().asActuals(),
                    args,
                    f.preFeature(),
                    Types.resolved.t_unit)
      .resolveTypes(res, outer);
  }


  /**
   * Create call to f's pre bool feature
   *
   * @param res resolution instance
   *
   * @param f a feature with a precondition that should be called.
   *
   * @param outer The call to f's pre bool feature is to be added to outer's
   * code.
   *
   * @return a call to f.preBoolFeature() to be added to code of outer.
   */
  static Call callPreBool(Resolution res, AbstractFeature f, Feature outer)
  {
    var oc = f.contract();
    var p = oc._hasPre != null ? oc._hasPre : f.pos();
    List<Expr> args = new List<>();
    for (var a : outer.valueArguments())
      {
        var ca = new Call(p,
                          new Current(p, outer),
                          a,
                          -1);
        ca = ca.resolveTypes(res, outer);
        args.add(ca);
      }

    var t = (outer.outerRef() != null) ? new This(p, outer, outer.outer()).resolveTypes(res, outer)
                                       : new Universe();
    if (f instanceof Feature ff)  // if f is currently being compiled, make sure its post feature is added first
      {
        addContractFeatures(ff, res);
      }
    return new Call(p,
                    t,
                    outer.generics().asActuals(),
                    args,
                    f.preBoolFeature(),
                    Types.resolved.t_bool)
      .resolveTypes(res, outer);
  }


  /**
   * Create call to f within f's pre and call feature
   *
   * @param res resolution instance
   *
   * @param f a feature with a precondition. f should be called.
   *
   * @return a call to f to be added to code of f.preAndCallFeature().
   */
  static Call callOriginal(Resolution res, Feature f)
  {
    var preAndCallOuter = f.preAndCallFeature();
    var oc = f.contract();
    var p = oc._hasPre != null ? oc._hasPre : f.pos();
    List<Expr> args = new List<>();
    for (var a : preAndCallOuter.valueArguments())
      {
        var ca = new Call(p,
                          new Current(p, preAndCallOuter),
                          a,
                          -1);
        ca = ca.resolveTypes(res, preAndCallOuter);
        args.add(ca);
      }
    var t = new This(p, preAndCallOuter, preAndCallOuter.outer()).resolveTypes(res, preAndCallOuter);
    return new Call(p,
                    t,
                    preAndCallOuter.generics().asActuals(),
                    args,
                    f,
                    f.resultType())
      {
        @Override
        boolean preChecked() { return true; }
      };
  }


  /**
   * Create call to f's postcondition feature
   *
   * @param res resolution instance
   *
   * @param f a feature with a postcondition whose body the result will be
   * added to
   *
   * @return a call to f.postFeature() to be added to code of f.
   */
  static Call callPostCondition(Resolution res, Feature f)
  {
    var oc = f.contract();
    var p = oc._hasPost != null ? oc._hasPost : f.pos();
    List<Expr> args = new List<>();
    for (var a : f.valueArguments())
      {
        var ca = new Call(p,
                          new Current(p, f),
                          a,
                          -1);
        ca = ca.resolveTypes(res, f);
        args.add(ca);
      }
    if (f.hasResultField())
      {
        var c2 = new Call(p,
                          new Current(p, f),
                          f.resultField(),
                          -1);
        c2 = c2.resolveTypes(res, f);
        args.add(c2);
      }
    else if (f.isConstructor())
      {
        args.add(new Current(p, f));
      }
    return callPostCondition(res, f, f, args);
  }


  /**
   * Create call to outer's postcondition feature to be added to code of feature `in`.
   *
   * @param res resolution instance
   *
   * @param outer a feature with a postcondition
   *
   * @param in either equal to outer or the postcondition feature of a
   * redefinition of outer. The call ot outer's postcondition is to be added to
   * in's code.
   *
   * @param args actual arguments to be passed to the call
   *
   * @return a call to outer.postFeature() to be added to code of in.
   */
  private static Call callPostCondition(Resolution res, AbstractFeature outer, Feature in, List<Expr> args)
  {
    var p = in.contract()._hasPost != null
          ? in.contract()._hasPost   // use `post` position if `in` is of the form `f post cc is ...`
          : in.pos();                // `in` does not have `post` clause, only inherits postconditions. So use the feature position instead

    var t = (in.outerRef() != null) ? new This(p, in, in.outer()).resolveTypes(res, in)
                                    : new Universe();
    if (outer instanceof Feature of)  // if outer is currently being compiled, make sure its post feature is added first
      {
        addContractFeatures(of, res);
      }
    var callPostCondition = new Call(p,
                                     t,
                                     in.generics().asActuals(),
                                     args,
                                     outer.postFeature(),
                                     Types.resolved.t_unit);
    callPostCondition = callPostCondition.resolveTypes(res, in);
    return callPostCondition;
  }


  /**
   * Helper to create `ParsedCall` to `n` at position `p`
   */
  private static ParsedCall pc(SourcePosition p, String n)
  {
    return new ParsedCall(new ParsedName(p, n));
  }

  /**
   * Helper to create `ParsedCall` to `t`.`n` at position `p`
   */
  private static ParsedCall pc(Expr t, SourcePosition p, String n)
  {
    return new ParsedCall(t, new ParsedName(p, n));
  }

  /**
   * Helper to create `ParsedCall` to `n[0]`.`n[1]`..`n[n.length-1] a`  at position `p`
   */
  private static ParsedCall pc(SourcePosition p, String[] n, List<Expr> a)
  {
    Expr target = null;
    for (var i = 0; i<n.length-1; i++)
      {
        target = pc(target, p, n[i]);
      }
    return new ParsedCall(target, new ParsedName(p, n[n.length-1]), a);
  }


  private static void addPreFeature(Feature f, Resolution res, boolean preBool)
  {
    var fc = f.contract();
    var dc = preBool ? fc._declared_preconditions2
                     : fc._declared_preconditions;
    var inhpres = f._inheritedPre;
    var inheritingTrue = inhpres.stream()
                                .filter(inh -> !hasPreConditionsFeature(inh))
                                .findFirst();
    var name = preBool ? preBoolConditionsFeatureName(f) : preConditionsFeatureName(f);
    var args = fc._argsSupplier == null ? null : fc._argsSupplier.get();
    var pos = fc._hasPre != null ? fc._hasPre : f.pos();
    var l = new List<Expr>();
    Expr cc = null;

    for (var c : dc)
      {
        var p = c.cond.pos();
        if (preBool)
          {
            cc = cc == null
              ? c.cond
              : new ParsedCall(cc, new ParsedName(pos, "infix &&"), new List<>(c.cond));
          }
        else
          {
            var cond = c.cond;
            if (inheritingTrue.isPresent())
              { // one of the inherited preconditions is `true`, so we do not
                // need to check the conditions defined locally at all.
                // However, we want to check the condition code for errors etc.,
                // so we wrap it into `(true || <cond>)`
                cond = new ParsedCall(pc(pos, "true"),
                                      new ParsedName(pos, "infix ||"), new List<>(cond));
              }
            l.add(new If(p,
                         cond,
                         new Block(),
                         pc(p, FuzionConstants.FUZION_RUNTIME_PRECONDITION_FAULT, new List<>(new StrConst(p, p.sourceText())))
                         )
              {
                @Override boolean fromContract() { return true; }
              }
                  );
          }
      }
    if (preBool && cc != null)
      {
        l.add(cc);
      }

    if (inheritingTrue.isPresent() && !dc.isEmpty())
      {
        /*
        var inh = inheritingTrue.get();
        System.err.println("WARNING: For "+f.qualifiedName()+" there are declared preconditions "+dc.getFirst().cond.pos().show()+"\n"+
                           "but these are ignored since we inherit precondition `true` from "+inh+" at "+inh.pos().show());
        */
      }
    var code = new Block(l);
    AbstractType universe_type = null; //new ParsedType(pos, "universe", UnresolvedType.NONE, null);
    var result_type     = new ParsedType(pos,
                                         preBool ? "bool"
                                                 : "unit",
                                         UnresolvedType.NONE,
                                         universe_type);
    var pF = new Feature(pos,
                         f.visibility().eraseTypeVisibility(),
                         // 0, // NYI: why not this:
                         f.modifiers() & FuzionConstants.MODIFIER_FIXED, // modifiers
                         new FunctionReturnType(result_type),
                         new List<>(name),
                         args,
                         new List<>(), // inheritance
                         Contract.EMPTY_CONTRACT,
                         new Impl(pos, code, Impl.Kind.Routine));
    res._module.findDeclarations(pF, f.outer());
    res.resolveDeclarations(pF);
    res.resolveTypes(pF);
    if (preBool)
      {
        f._preBoolFeature = pF;
      }
    else
      {
        f._preFeature = pF;
      }

    // We add calls to preconditions of redefined features after creating pF since
    // this enables us to access pF directly:

    // List<Expr> li = null;
    // var s = inhpres.stream()
    //  .takeWhile(inh -> hasPreconditionsFeature(inh));
    //var c = s.count();
    var new_code = code._expressions;
    if (inheritingTrue.isPresent() || !dc.isEmpty() || preBool)
      { // all inherited are added using
        //
        // if (pre_bool_inh1 || pre_bool_inh2 || ... || pre_bool_inh<n>) then
        // else  check declared
        for (var i = 0; i < inhpres.size() && hasPreConditionsFeature(inhpres.get(i)); i++)
          {
            var call = callPreBool(res, inhpres.get(i), pF);
            cc = cc == null
              ? call
              : new ParsedCall(cc, new ParsedName(pos, "infix ||"), new List<>(call));
          }
      }
    else
      { // The last inherited precondition may cause a fault and is checked using
        //
        // if (pre_bool_inh1 || pre_bool_inh2 || ... || pre_bool_inh<n-1>) then
        // else pre_inh<n>
        for (var i = 0; i < inhpres.size()-1; i++)
          {
            var call = callPreBool(res, inhpres.get(i), pF);
            cc = cc == null
              ? call
              : new ParsedCall(cc, new ParsedName(pos, "infix ||"), new List<>(call));
          }
        if (inhpres.size() == 0)
          {
            System.err.println("NYI: no inherited and no declared preconditions, we should not end up here");
          }
        else
          { // code is empty anyway, replace it by call to pre_inh<n>:
            new_code = new List<>(callPreCondition(res, inhpres.getLast(), pF));
          }
      }

    if (preBool && cc != null)
      {
        new_code = new List<>(cc);
      }
    else if (preBool)
      {
        new_code = new List<>(pc(pos, "true"));
      }
    else if (cc != null)
      {
        new_code = new List<>(new If(pos,
                                     cc,
                                     new Block(),
                                     new Block(new_code)));
      }
    code._expressions = new_code;
    var e = res.resolveType(code, pF);
  }


  /**
   * Part of the syntax sugar phase: For all contracts, create artificial
   * features that check that contract.
   */
  static void addContractFeatures(Feature f, Resolution res)
  {
    if (PRECONDITIONS) require
      (f != null,
       res != null,
       Errors.any() || !f.isUniverse() || (f.contract()._declared_preconditions.isEmpty () &&
                                           f.contract()._declared_postconditions.isEmpty()   ));

    var fc = f.contract();

    // add precondition feature
    if (requiresPreConditionsFeature(f) && f._preFeature == null)
      {

        /*
    // tag::fuzion_rule_SEMANTIC_CONTRACT_PRE_ORDER[]
The conditions of a pre-condition are checked at run-time in sequential
source-code order after any inherited pre-conditions have been
checked. +
Inherited pre-conditions of redefined inherited features are checked at
runtime in the source code order of the `inherit` clause of the corresponding
outer features.  +
In case an inherited pre-condition is `false`, the
pre-conditions following the failing one will not be evaluated and checked, but
precondition checking continues with the preconditions of the next inherited
contract, if that exists, or with the declared preconditions after `require else`
in the redefining feature, unless there is no `require else` present.
Redefined inherited features that neither declare nor inherit a precondition will
have `true` as their imlicit precondition, effectively turning the precondition of
all of their redefinition to `true`. +
    // end::fuzion_rule_SEMANTIC_CONTRACT_PRE_ORDER[]
        */

        /* We add three features for every feature with an own or inherited pre-condition as follows:

           pre_<name>      is a feature that checks the precondition and causes a fault in case any condition fails.

                           First, inherited preconditions are checked via cals to their pre_bool_<name> and
                           precondition checking is stopped with success if those return true

                           If there are no own pre-conditions, the last inherited precondition is checked
                           by pre_<name> instead of pre_bool_<name>.

           pre_bool_<name> is a feature that check the precondition and results in true iff all preconditions hold.

                           First, inherited preconditions are checked via cals to their pre_bool_<name> and
                           precondition checking is stopped with success if those return true.

                           Finally, the own pre-condition is checked

           pre_and_call_<name>
                           This calls pre_<name> followed by <name>, just for convenience to avoid
                           duplicate calls in the code

           Example: For a fuzion feature with a precondition as in

             a is
               f(a,b) c
                 pre
                   cc1
                   cc2
               =>
                 x

             z := a.f x y

           we add

             a is

               pre_f(a,b) unit =>
                 if cc1 then else fuzion.runtime.precondition_fault "cc1"
                 if cc2 then else fuzion.runtime.precondition_fault "cc2"

               pre_and_call_f(a,b) c =>
                 pre_f a b
                 f a b

               f(a,b) c
                 pre
                   cc1
                   cc2
               =>
                 x

             z := a.pre_and_Call_f x y

           furthermore, in case of a redefinition

             a is
               f(a,b) c
                 pre
                   cc1
               =>
                 x

             b : a is
               redef f(a,b) c
               =>
                 x

             c is
               f(a,b) c
                 pre
                   cc2
               =>
                 x

             d : a, c is
               redef f(a,b) c
               =>
                 x

             e : a, c is
               redef f(a,b) c
                 pre else
                   cc3
                   cc4
               =>
                 x

             z := b.f x y
             z := c.f x y
             z := d.f x y
             z := e.f x y

           we add

             a is

               pre_f1(a,b) unit =>
                 if cc1 then else fuzion.runtime.precondition_fault "cc1"

               pre_bool_f1(a,b) bool =>
                 cc1

               pre_and_call_f2(a,b) c =>
                 pre_f1 a b
                 f a b

               f(a,b) c
                 pre
                   cc1
               =>
                 x

             b : a is

               pre_f3(a,b) unit =>
                 pre_f1 a b

               pre_bool_f3(a,b) bool =>
                 pre_bool_f1 a b

               pre_and_call_f4(a,b) c =>
                 pre_f3 a b
                 f a b

               redef f(a,b) c
               =>
                 x

             c is

               pre_f5(a,b) unit =>
                 if cc2 then else fuzion.runtime.precondition_fault "cc2"

               pre_bool_f5(a,b) bool =>
                 if pre_bool_f1 a b then true
                 else
                   pre_f5 a b
                   false

               pre_and_call_f6(a,b) c =>
                 pre_f5 a b
                 f a b

               f(a,b) c
                 pre
                   cc2
               =>
                 x

             d : a, c is

               pre_f7(a,b) unit =>
                 if pre_bool_f1 a b then
                 else
                   pre_f5 a b

               pre_bool_f7(a,b) bool =>
                 if      pre_bool_f1 a b then true
                 else if pre_bool_f5 a b then true
                 else                         false

               pre_and_call_f8(a,b) c =>
                 pre_f7 a b
                 f a b

               redef f(a,b) c
               =>
                 x

             e : a, c is

               pre_f9(a,b) unit =>
                 if      pre_bool_f1 a b then
                 else if pre_bool_f5 a b then
                 else if cc3 then else fuzion.runtime.precondition_fault "cc3"
                 else if cc5 then else fuzion.runtime.precondition_fault "cc5"

               pre_bool_f9(a,b) bool =>
                 if      pre_bool_f1 a b then true
                 else if pre_bool_f5 a b then true
                 else if cc3 then
                   cc4

               pre_and_call_f8(a,b) c =>
                 pre_f7 a b
                 f a b

               redef f(a,b) c
                 pre else
                   cc3
               =>
                 x

             z := b.f x y
             z := c.f x y
             z := d.f x y
             z := e.f x y
         */

        addPreFeature(f, res, true);
        addPreFeature(f, res, false);

        if (!f.isConstructor())
          {
            var pos = fc._hasPre != null ? fc._hasPre : f.pos();
            var name2 = preConditionsAndCallFeatureName(f);
            var args2 = fc._argsSupplier.get();
            var l2 = new List<Expr>();
            var code2 = new Block(l2);
            var pF2 = new Feature(pos,
                                  f.visibility().eraseTypeVisibility(),
                                  // 0, // NYI: why not this:
                                  f.modifiers() & FuzionConstants.MODIFIER_FIXED, // modifiers
                                  NoType.INSTANCE,
                                  new List<>(name2),
                                  args2,
                                  new List<>(), // inheritance
                                  Contract.EMPTY_CONTRACT,
                                  new Impl(pos, code2, Impl.Kind.RoutineDef));
            res._module.findDeclarations(pF2, f.outer());
            f._preAndCallFeature = pF2;

            res.resolveDeclarations(pF2);
            l2.add(callPreCondition(res, f, (Feature) f.preAndCallFeature()));
            l2.add(callOriginal(res, f));
            res.resolveTypes(pF2);
          }
      }

    // add postcondition feature
    if (requiresPostConditionsFeature(f) && f._postFeature == null)
      {
        var name = postConditionsFeatureName(f);
        var args = fc._argsSupplier.get();
        var pos = fc._hasPost != null ? fc._hasPost : f.pos();
        var resultField = new Feature(pos,
                                      Visi.PRIV,
                                      f.isConstructor()
                                      ? f.thisType()
                                      : f.resultType(), // NYI: replace type parameter of f by type parameters of _postFeature!
                                      FuzionConstants.RESULT_NAME)
          {
            public boolean isResultField() { return true; }
          };
        args.add(resultField);
        var l = new List<Expr>();
        for (var c : fc._declared_postconditions)
          {
            var p = c.cond.pos();
            l.add(new If(p,
                         c.cond,
                         new Block(),
                         pc(p, FuzionConstants.FUZION_RUNTIME_POSTCONDITION_FAULT, new List<>(new StrConst(p, p.sourceText())))
                         )
              {
                @Override boolean fromContract() { return true; }
              }
                  );
          }
        var code = new Block(l);
        var pF = new Feature(pos,
                             f.visibility().eraseTypeVisibility(),
                             f.modifiers() & FuzionConstants.MODIFIER_FIXED, // modifiers
                             NoType.INSTANCE,
                             new List<>(name),
                             args,
                             new List<>(), // inheritance
                             Contract.EMPTY_CONTRACT,
                             new Impl(pos, code, Impl.Kind.RoutineDef));
        res._module.findDeclarations(pF, f.outer());
        res.resolveDeclarations(pF);
        res.resolveTypes(pF);
        f._postFeature = pF;

        /*
    // tag::fuzion_rule_SEMANTIC_CONTRACT_POST_ORDER[]
The conditions of a post-condition are checked at run-time in sequential source-code order after any inherited post-conditions have been checked. Inherited post-conditions of redefined inherited features are checked at runtime in the source code order of the `inherit` clause of the corresponding outer features.
    // end::fuzion_rule_SEMANTIC_CONTRACT_POST_ORDER[]
        */

        // We add calls to postconditions of redefined features after creating pF since
        // this enables us to access pF directly:
        List<Expr> l2 = null;
        for (var inh : f._inheritedPost)
          {
            if (hasPostConditionsFeature(inh))
              {
                List<Expr> args2 = new List<>();
                for (var a : args)
                  {
                    var ca = new Call(pos,
                                      new Current(pos, pF),
                                      a,
                                      -1);
                    ca = ca.resolveTypes(res, pF);
                    args2.add(ca);
                  }
                var inhpost = callPostCondition(res, inh, pF, args2);
                inhpost = inhpost.resolveTypes(res, pF);
                if (l2 == null)
                  {
                    l2 = new List<>();
                  }
                l2.add(inhpost);
              }
          }
        if (l2 != null)
          {
            l2.addAll(code._expressions);
            code._expressions = l2;
          }
      }
  }


  /**
   * toString
   *
   * @return
   */
  public String toString()
  {
    StringBuffer res = new StringBuffer();
    if (_hasPre != null)
      {
        res
          .append("\n  pre ")
          .append(_hasPreElse != null ? "else " : "")
          .append(_declared_preconditions);
      }
    if (_hasPost != null)
      {
        res
          .append("\n  post ")
          .append(_hasPostThen != null ? "then " : "")
          .append(_declared_postconditions);
      }
    return res.toString();
  }

}

/* end of file */
