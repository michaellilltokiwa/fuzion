## 2024-**-**: V0.091


## 2024-09-16: V0.090

Fuzion language

  - Type constraints can now be made more restrictive in preconditions and conditional code, fix ([#967](https://github.com/tokiwa-software/fuzion/issues/967), [#3480](https://github.com//tokiwa-software/fuzion/pull/3480))

    This provides a simple mechanism to add inner features that require additional type constraints such as `Sequence.find` that requires the element type to `T` to be constrained by `property.quatable`. As a precondition, this is written as

        public find(pattern Sequence T) option i32
          pre
            T : property.equatable
        =>
          ...

    using a new operator `infix :` that operates on types

        Type.infix : (T type) bool

    Such type constraints can also be done using conditions as follows:

        f(x T) =>
          if T : String
            say x.byte_count

    These type-tests are evaluated at compile-time, there is no code generated
    for the type tests. Instead, the compiler validates that preconditions that
    impose addition type constraints can statically be verified to be met for
    every call of a corresponding feature.

  - Effects in fuzion now clearly distinguish _effect types_ and _effect
    instances_: For a given effect type, one may _instate_ any instance
    that is assignable to the effect type.

    This means that instances installed for effect types that a reference types
    may be child features that inherit from the effect type, redefinition may be
    used to implement effect operation and dynamic binding is used when these
    operations are used.

    Here is an example of an effect `Color` with an operation
    `get` and two instances `green` and
    `purple`:

        Color ref : effect is
          get String => abstract

        green  : Color is
          redef get => "green"

        purple : Color is
          redef get => "purple"

    To `instate` one of them, we need to call `instate` on
    the effect type `color`, providing an argument of the value type
    `green` or `purple`:

        Color.instate green  ()->{say "color is {Color.env.get}"}
        Color.instate purple ()->{say "color is {Color.env.get}"}

    This avoids the need to have separate effect handler features that implement
    the operations and are called using dynamic binding from the effect's
    operation.  The drawback is that to instate an effect, we now need the
    effect value and the effect type since the value's type might be different.

    related pull requests

    - lib: support `ref` effect types, make all effects simple, remove `effect_mode` ([#3574](https://github.com//tokiwa-software/fuzion/pull/3574))

    - bin/ebnf.fz: switch to `instate_self` syntax of effects ([#3584](https://github.com//tokiwa-software/fuzion/pull/3584))

    - C: fix code for `effect.type.instate0` intrinsic for unit type effect ([#3606](https://github.com//tokiwa-software/fuzion/pull/3606))

    - fix add_simple_test, replace `go` by `instate_self` ([#3594](https://github.com//tokiwa-software/fuzion/pull/3594))

    - jvm: Fix code generated for intrinsic `effect.instate0` on a unit type effect ([#3610](https://github.com//tokiwa-software/fuzion/pull/3610))

    - update example_1,  `go ()-&gt;` is now `instate_self ()-&gt;` ([#3639](https://github.com//tokiwa-software/fuzion/pull/3639))

  - Improved handling of semicolon after lambda expression, in particular, code like

        m := (1..10).map x->2*x; say m

    is valid now while code with a parser ambiguity at semicolons like

        my_feat => say "Hi"; say "ambiguous"

        if true if true then say "OK"; say "ambiguous"

    causes an error now. `{ }` can be used for disambiguation:

        my_feat => { say "Hi" }; say "ambiguous"
        my_feat => { say "Hi"; say "ambiguous" }

        if true if true then { say "OK" }; say "ambiguous"
        if true if true then { say "OK"; say "ambiguous" }

    related pull requests

    - parser: flag error for ambiguous use of semicolons ([#3510](https://github.com//tokiwa-software/fuzion/pull/3510))

    - parser: add semicolon limit for parsing lambda expressions ([#3501](https://github.com//tokiwa-software/fuzion/pull/3501))

- Base library

  - New base library features

    - lib,be/c: add mutexes and conditions ([#3428](https://github.com//tokiwa-software/fuzion/pull/3428))

    - lib: Add `ignore(T type, x T) unit` to drop a value ([#3679](https://github.com//tokiwa-software/fuzion/pull/3679))

    - lib: add `thread_pool` effect ([#3615](https://github.com//tokiwa-software/fuzion/pull/3615))

    - lib: add simple, fixed-size thread pool ([#3579](https://github.com//tokiwa-software/fuzion/pull/3579))

    - lib/net/server: add experimental feature handling connections in threads ([#3554](https://github.com//tokiwa-software/fuzion/pull/3554))

    - lib: add internationalization effect ([#3571](https://github.com//tokiwa-software/fuzion/pull/3571))

      related pull requests

      - lib: Add currency to internationalization and fix small mistakes ([#3635](https://github.com//tokiwa-software/fuzion/pull/3635))

      - lib: add default effect for internationalization ([#3620](https://github.com//tokiwa-software/fuzion/pull/3620))

      - lib: add simple tests for internationalization effect ([#3627](https://github.com//tokiwa-software/fuzion/pull/3627))

      - lib: use new effects syntax for internationalization ([#3587](https://github.com//tokiwa-software/fuzion/pull/3587))

      - lib: replace int_eff with provide ([#3616](https://github.com//tokiwa-software/fuzion/pull/3616))

    - io/buffered/reader: switch to circular buffer ([#3514](https://github.com//tokiwa-software/fuzion/pull/3514))

  - Changes to the following standard library features

    - effect: Made `effect.abort` a type feature and removed `effect.run`, fix [#3634](https://github.com/tokiwa-software/fuzion/issues/3634)) ([#3640](https://github.com//tokiwa-software/fuzion/pull/3640))

    - lib: fix oneway monads ([#3590](https://github.com//tokiwa-software/fuzion/pull/3590))

    - lib: move `/!` and `%!` from `wrap_around` to `integer` ([#3522](https://github.com//tokiwa-software/fuzion/pull/3522))

    - lib: move `infix ∈` and `infix ∉` from `property.equatable` to `universe` ([#3622](https://github.com//tokiwa-software/fuzion/pull/3622))

    - lib: move features from `float_sequence`, `numeric_sequence` to `Sequence` ([#3668](https://github.com//tokiwa-software/fuzion/pull/3668))

    - lib/net: create the required mutate effect in the server/client effects ([#3540](https://github.com//tokiwa-software/fuzion/pull/3540))

    - lib/net: rename `Request_Handler` to `Connection_Handler` ([#3509](https://github.com//tokiwa-software/fuzion/pull/3509))

    - lib: `close` on a `mutate.mutable_element` should close only that element ([#3573](https://github.com//tokiwa-software/fuzion/pull/3573))

    - lib/modules: fix typos in terminal API ([#3636](https://github.com//tokiwa-software/fuzion/pull/3636))

- Parser

  - parser: end expression at keyword `then` ([#3503](https://github.com//tokiwa-software/fuzion/pull/3503))

  - Relax precondition in `InlineArray.checkTypes` and improve position output, fix [#3552](https://github.com/tokiwa-software/fuzion/issues/3552) fix [#3553](https://github.com/tokiwa-software/fuzion/issues/3553) ([#3558](https://github.com//tokiwa-software/fuzion/pull/3558))

- Front end

  - fix accessing field of other instance in field init ([#3418](https://github.com//tokiwa-software/fuzion/pull/3418))

  - show error on choice constraint ([#3629](https://github.com//tokiwa-software/fuzion/pull/3629))

  - `Const_String` which is used for string literals no longer inherits from `array u8` which caused conflicts due to equally named features in `String` and `array` ([#3538](https://github.com//tokiwa-software/fuzion/pull/3538))

  - improved error handling

    - ast: improve error message when inheritance is used without `ref` ([#3570](https://github.com//tokiwa-software/fuzion/pull/3570))

    - ast: suppress subsequent error on ambiguous choice containing errors ([#3561](https://github.com//tokiwa-software/fuzion/pull/3561))

    - ast/tests: Improve error message for [#3619](https://github.com/tokiwa-software/fuzion/issues/3619), add regression test ([#3626](https://github.com//tokiwa-software/fuzion/pull/3626))

    - suppress consequential errors ([#3588](https://github.com//tokiwa-software/fuzion/pull/3588))

- Middle End

  - air: add preconditions that helped me debug [#3613](https://github.com/tokiwa-software/fuzion/issues/3619) / [#3619](https://github.com/tokiwa-software/fuzion/issues/3619) ([#3623](https://github.com//tokiwa-software/fuzion/pull/3623))

  - air: Clazz.toString created equal strings for a feature and its cotype ([#3481](https://github.com//tokiwa-software/fuzion/pull/3481))

  - air: FeatureAndActuals, add precondition checking type parameters mat… ([#3643](https://github.com//tokiwa-software/fuzion/pull/3643))

  - air: For a missing implementation of an abstract feature, show call chain ([#3525](https://github.com//tokiwa-software/fuzion/pull/3525))

  - air: make fields in Clazzes non static ([#3479](https://github.com//tokiwa-software/fuzion/pull/3479))

  - air: treat all intrinsics as intrinsic constructors ([#3624](https://github.com//tokiwa-software/fuzion/pull/3624))

  - ast: AbstractType.toString show `.this.type` not only `.this` ([#3665](https://github.com//tokiwa-software/fuzion/pull/3665))

  - ast: change visibility of internal THIS#TYPE to private ([#3582](https://github.com//tokiwa-software/fuzion/pull/3582))

  - ast: For ambiguous calls, show the position of the feature declarations ([#3572](https://github.com//tokiwa-software/fuzion/pull/3572))

  - ast: Remove `Expr.typeForCallTarget` ([#3669](https://github.com//tokiwa-software/fuzion/pull/3669))

- DFA

  - dfa: do not mark intrinsics as escaping, which prohibited tail call optimization to  ([#3434](https://github.com//tokiwa-software/fuzion/pull/3434))

- FUIR

  - fuir: add method `isConstructor` to `FUIR` ([#3504](https://github.com//tokiwa-software/fuzion/pull/3504))

  - fuir: change output of `codeAtAsString` for `Assign` ([#3653](https://github.com//tokiwa-software/fuzion/pull/3653))

  - fuir: tagged values, use same tag numbers as front-end ([#3614](https://github.com//tokiwa-software/fuzion/pull/3614))

- JVM back end

  - be/jvm: implement intrinsics, mtx_*, cond_* ([#3534](https://github.com//tokiwa-software/fuzion/pull/3534))

- C back end

  - be/c: add fzE_memcpy, fzE_memset ([#3496](https://github.com//tokiwa-software/fuzion/pull/3496))

  - be/c: add mutex implementation for windows ([#3512](https://github.com//tokiwa-software/fuzion/pull/3512))

  - be/c: C.call, move call to args into `if` ([#3528](https://github.com//tokiwa-software/fuzion/pull/3528))

- Interpreter back end

  - ai: add method `reportErrorInCode` ([#3515](https://github.com//tokiwa-software/fuzion/pull/3515))

  - be/int: implement intrinsics, mtx_*, cond_* ([#3535](https://github.com//tokiwa-software/fuzion/pull/3535))

- FZ Tool

  - Improved error source code location display using underlining via ANSI escapes and fixes of some corner cases:

    - util: underline error position at line end when ANSI escapes are enabled ([#3530](https://github.com//tokiwa-software/fuzion/pull/3530),[#3559](https://github.com//tokiwa-software/fuzion/pull/3559))

    - util: fix cut-off code in error message ([#3511](https://github.com//tokiwa-software/fuzion/pull/3511))

    - util: fix caret missing at eol ([#3529](https://github.com//tokiwa-software/fuzion/pull/3529))

  - util: env var / property to Enable stack trace printing on error reporting ([#3663](https://github.com//tokiwa-software/fuzion/pull/3663))

  - replaced the System.getenv,System.getProperty functions with FuzionOptions.boolPropertyOrEnv ([#3569](https://github.com//tokiwa-software/fuzion/pull/3569))

- Tests

  - tests: calls_on_ref_and_val_target: remove NYI ([#3489](https://github.com//tokiwa-software/fuzion/pull/3489))

  - tests: add regression tests [#1840](https://github.com/tokiwa-software/fuzion/issues/1840) ([#3612](https://github.com//tokiwa-software/fuzion/pull/3612)), [#2339](https://github.com/tokiwa-software/fuzion/issues/2339) ([#3605](https://github.com//tokiwa-software/fuzion/pull/3605)), [#1518](https://github.com/tokiwa-software/fuzion/issues/1518) ([#3519](https://github.com//tokiwa-software/fuzion/pull/3519)), [#3147](https://github.com/tokiwa-software/fuzion/issues/3147) ([#3656](https://github.com//tokiwa-software/fuzion/pull/3656)), [#3352](https://github.com/tokiwa-software/fuzion/issues/3352) ([#3633](https://github.com//tokiwa-software/fuzion/pull/3633)), [#2111](https://github.com/tokiwa-software/fuzion/issues/2111) ([#3492](https://github.com//tokiwa-software/fuzion/pull/3492))

  - tests: Added regression tests for safety, debug, and debug_level (Issue [#3518](https://github.com/tokiwa-software/fuzion/issues/3518)) ([#3580](https://github.com//tokiwa-software/fuzion/pull/3580))

  - tests: turn positive into simple tests ([#3526](https://github.com//tokiwa-software/fuzion/pull/3526))

- Examples

  - examples: use connection handler instead of request handler ([#3513](https://github.com//tokiwa-software/fuzion/pull/3513))

- Other

  - ported shell script ebnf.sh to Fuzion ebnf.fz ([#3436](https://github.com//tokiwa-software/fuzion/pull/3436))


## 2024-08-23: V0.089

- Fuzion language

  - Shadowing of fields is no longer permitted to avoid confusion ([#3322](https://github.com//tokiwa-software/fuzion/pull/3322))

  - numeric literals are no longer automatically converted to `i64` if they do not fit the range of `i32` ([#3414](https://github.com//tokiwa-software/fuzion/pull/3414))

  - relaxed semantics for choice features: They now may inherit from one other choice feature ([#3060](https://github.com//tokiwa-software/fuzion/pull/3060))

    This permits code to, e.g., inherit from `option`:

        maybe_String : option String is
          redef as_string =>
            is_nil ? "no String" : "a String: $val"

  - Defining a type feature in `universe` now results in an error ([#3411](https://github.com//tokiwa-software/fuzion/pull/3411))

  - Make use of `redef` mandatory when implementing abstract features ([#3228](https://github.com/tokiwa-software/fuzion/pull/3228), [#3213](https://github.com/tokiwa-software/fuzion/pull/3213), [#3218](https://github.com/tokiwa-software/fuzion/pull/3218)).

    So `redef` is required now, e.g., in this code:

        new_line : String is
          public redef utf8 => [u8 10]

    This avoids bugs due to accidental redefinition and underlines the fact that redefinition includes inheritance of pre- and post-conditions.

  - Precondition are now syntax sugar that uses effects ([#3260](https://github.com/tokiwa-software/fuzion/pull/3260)). This permits code to handle pre-condition failures as in

        v := fuzion.runtime.pre_fault
          .try ()->
            a := i32 1000000
            a*a
          .catch s->
            say "precondition failed $s"
            -1

    If `debug` is enabled, this will result in printing that the precondition of `infix *` did not hold due to the overflow caused by the code.

    Internally, what happens is that code with a precondition like

        feat(a t)
          pre cc
        => code

        r := feat v

    will be compiled into

        feat(a t)
        => code

        pre_feat(a t) =>
          if !cc then
            fuzion.runtime.pre_fault.cause "cc"

        pre_and_call_feat(a t) =>
          pre_feat a
          feat a

        r := pre_and_call_feat  v

    And calls to `feat` will be replaced by calls to `pre_and_call_feat`.

    This change resulted in a significant simplification of the Fuzion middle end and back ends, code related to pre- and post-conditions could be removed.

  - Precondition inheritance is now supported ([#3260](https://github.com/tokiwa-software/fuzion/pull/3260)): Preconditions in redefined features can only be weakened.

  - use keyword (for/do/while/until) as reference for indentation ([#3177](https://github.com/tokiwa-software/fuzion/pull/3177)). This avoids errors as in

        for i := 0, i+1
        until i > 3 do
          say "hey!"

    which used to be parsed as two loops (the second one being `do say "hey!"`, now causes an error.

  - remove `intrinsic_contructor` features ([#3062](https://github.com/tokiwa-software/fuzion/pull/3062)), using `instrinsic` instead.

  - Fuzion no longer permits silently ignoring a result ([#3095](https://github.com/tokiwa-software/fuzion/pull/3095)). If `feat` returns a result that is no `unit` or `void`, calling `feat` will cause an error:

        feat     # ignoring result causes an error

    we need to use the result and have to ignore it explicitly, e.g., by

        ignore := feat  # assign result to a dummy field is ok
        _ := feat       # assign result to _ is also ok

  - Chained-boolean operations like `a <= b <= c` are now restricted to infix operators`=`, `&lt;=`, etc., this avoids broken code like `8 %% 5 = 3` to cause a compile time error ([#3264](https://github.com/tokiwa-software/fuzion/pull/3264))

  - Post conditions of constructors can now access the newly created instance using `result` ([#3138](https://github.com/tokiwa-software/fuzion/pull/3138))

  - Redefinition of feature with type parameters is now supported, fixed [#3136](https://github.com/tokiwa-software/fuzion/issues/3136) ([#3143](https://github.com/tokiwa-software/fuzion/pull/3143))

  - Post-conditions are implemented using effects now. It is possible to handle failed post-conditions in application code now.

    Post-condition of redefined features are inherited now, i.e, a post-condition can only be made stronger by a redefinition. ([#3037](https://github.com/tokiwa-software/fuzion/pull/3037))

    Post-condition are purely syntax sugar now, the definition of a post-condition causes the creation of a post-condition feature that checks that condition and causes the corresponding post-condition fault in case it does not hold. ([#3100](https://github.com/tokiwa-software/fuzion/pull/3100))

  - EBNF Grammar: added missing rules for native, abstract and intrinsic routines defined using `=&gt;` ([#3044](https://github.com/tokiwa-software/fuzion/pull/3044))

  - It is now possible to use `universe` in type qualifiers as in `universe.unit` to solve ambiguities ([#3068](https://github.com/tokiwa-software/fuzion/pull/3068))

  - destructuring no longer permits overwriting fields ([#3089](https://github.com/tokiwa-software/fuzion/pull/3089))

  - Permit `:=` in a field declaration at minimum indentation ([#3093](https://github.com/tokiwa-software/fuzion/pull/3093))

    This permits the same indentation syntax for field and routine declarations, e.g.,

        # a routine:
        abc
        pre
          true
        => 42

        # a field:
        def
        pre
          true
        := 4711

- Base library

  - Added libraries for base16 (hex) ([#3375](https://github.com//tokiwa-software/fuzion/pull/3375)), base32/base32hex ([#3368](https://github.com//tokiwa-software/fuzion/pull/3368)), and base64/base64url encodings ([#3319](https://github.com//tokiwa-software/fuzion/pull/3319))

  - `panic` and `try` now inherit from `eff.fallible` ([#3335](https://github.com//tokiwa-software/fuzion/pull/3335))

     This permits catching a `panic` as follows:

        panic.try   function_that_may_panic
             .catch (msg -> say "*** caught panic $msg ***")

  - New base library features

      - circular buffer ([#3186](https://github.com/tokiwa-software/fuzion/pull/3186))

      - buffered writer ([#3269](https://github.com/tokiwa-software/fuzion/pull/3269))

      - `fallible` effect is an abstract parent intended for effects that may fail abruptly like `fault`, pre- and post-conditions, `panic`, etc. ([#3157](https://github.com/tokiwa-software/fuzion/pull/3157))

      - `io.buffered`: variant of `read_line`/`read_lines` with custom delimiter ([#3067](https://github.com/tokiwa-software/fuzion/pull/3067))

      - The absolute value of `x` can now be calculated using `|x|` instead of `x.abs`. This is done using prefix and postfix operators, but we might add a `prepost` operator ([#3082](https://github.com/tokiwa-software/fuzion/pull/3082))

      - Support for mutable two-dimensional arrays was added ([#3090](https://github.com/tokiwa-software/fuzion/pull/3090))

  - Changes related to precondition features [#3260](https://github.com/tokiwa-software/fuzion/pull/3260): added `contract_fault` ([#3184](https://github.com/tokiwa-software/fuzion/pull/3184)), fixed preconditions of numeric operator ([#3283](https://github.com/tokiwa-software/fuzion/pull/3283))-

  -  Changes to the following standard library features

      - Add redundant preconditions since precondition inheritance is not supported yet (issue [#3051](https://github.com/tokiwa-software/fuzion/issues/3051), PR [#3053](https://github.com/tokiwa-software/fuzion/pull/3053))

      - Optimized `array.nth` for `array_backed` sequences ([#3063](https://github.com/tokiwa-software/fuzion/pull/3063))

      - lib/mutate.fz: code for array was split off into a separate file ([#3070](https://github.com/tokiwa-software/fuzion/pull/3070))

      - code cleanup to avoid feature declared in nested blocks ([#3084](https://github.com/tokiwa-software/fuzion/pull/3084))

  - Minor improvements

    - Made `outcome.is_error` `public` ([#3351](https://github.com//tokiwa-software/fuzion/pull/3351))

    - `ctrie` now uses ids for root and snapshot like the reference implementation ([#3280](https://github.com//tokiwa-software/fuzion/pull/3280))

    - merged `character_encodings` and `encodings` ([#3393](https://github.com//tokiwa-software/fuzion/pull/3393))

    - merged `list.filter0` with `list.filter` ([#3417](https://github.com//tokiwa-software/fuzion/pull/3417))

    - moved `infix ∈` and `infix ∉` to `property.equatable` ([#3325](https://github.com//tokiwa-software/fuzion/pull/3325))

    - Removed feature `Types`, move `Types.get` to `type_as_value`, fix [#3431](https://github.com/tokiwa-software/fuzion/issues/3431) ([#3466](https://github.com//tokiwa-software/fuzion/pull/3466))

    - remove type parameter from `effect.type.unsafe_get` ([#3450](https://github.com//tokiwa-software/fuzion/pull/3450))

    - rename `list.zip0` as `list.zip` ([#3413](https://github.com//tokiwa-software/fuzion/pull/3413))

    - In feature documentation, replaced triple backticks by quadruple indentation. ([#3331](https://github.com//tokiwa-software/fuzion/pull/3331))

    - `writer`, changed arg-type from `array` to `Sequence` ([#3384](https://github.com//tokiwa-software/fuzion/pull/3384))

    - `io.buffered.reader`: pass mutate effect as argument ([#3389](https://github.com//tokiwa-software/fuzion/pull/3389))

    - `io.buffered.writer`: fix out of bounds access to source array ([#3475](https://github.com//tokiwa-software/fuzion/pull/3475))

    - `net.client`: switched to `io.buffered.writer` ([#3309](https://github.com//tokiwa-software/fuzion/pull/3309))

    - `net.server`: switched to `io.buffered.writer` ([#3332](https://github.com//tokiwa-software/fuzion/pull/3332))

    - `process`: made it possible to use `io.buffered.writer` for writing ([#3304](https://github.com//tokiwa-software/fuzion/pull/3304))

    - `String`: changed `cut` feature result from tuple to record type ([#3400](https://github.com//tokiwa-software/fuzion/pull/3400))

    - `bool`: made the ternary operator lazy ([#3404](https://github.com//tokiwa-software/fuzion/pull/3404))

    - change `exit(handler, code)` to return unit ([#3173](https://github.com/tokiwa-software/fuzion/pull/3173))

    - change visibility of Random_Handler ([#3265](https://github.com/tokiwa-software/fuzion/pull/3265))

    - cleanup, rename `foldf_list` to `foldf` ([#3240](https://github.com/tokiwa-software/fuzion/pull/3240))

    - hide `f16`/`f128` for now ([#3148](https://github.com/tokiwa-software/fuzion/pull/3148))

    - process, add `with_out/with_err` ([#3146](https://github.com/tokiwa-software/fuzion/pull/3146))

    - remove `set` from `buffered.reader` ([#3263](https://github.com/tokiwa-software/fuzion/pull/3263))

- Documentation

  - Fuzion Reference Manual

    - Additions to reference manual glossary: Add definitions of _braces_ `{ }`, _brackets_ `[ ]` and _parentheses_ `( )` to reference manual. ([#3467](https://github.com//tokiwa-software/fuzion/pull/3467)), `cotype`, `original` and `type` `feature` ([#3472](https://github.com//tokiwa-software/fuzion/pull/3472))

  - Fuzion API Documentation

    - show `ref` keyword to API documentation ([#3425](https://github.com//tokiwa-software/fuzion/pull/3425))

    - added reference constructor section in API docs ([#3433](https://github.com//tokiwa-software/fuzion/pull/3433))

    - split types section into 'reference types' and 'value types', include constructors ([#3435](https://github.com//tokiwa-software/fuzion/pull/3435))

    - fix code lines not added if last in comment ([#3333](https://github.com//tokiwa-software/fuzion/pull/3333))

    - generate type feature documentation inside original feature ([#3407](https://github.com//tokiwa-software/fuzion/pull/3407))

    - improve representation of type features ([#3439](https://github.com//tokiwa-software/fuzion/pull/3439), [#3453](https://github.com//tokiwa-software/fuzion/pull/3453))

    - include inherited features in the docs ([#3451](https://github.com//tokiwa-software/fuzion/pull/3451))

    - sort feature names in API docs case insensitively ([#3427](https://github.com//tokiwa-software/fuzion/pull/3427))

    - underline all links in API docs on mouse-over ([#3398](https://github.com//tokiwa-software/fuzion/pull/3398), [#3440](https://github.com//tokiwa-software/fuzion/pull/3440))

    - use `cursor:pointer` on complete `&lt;details&gt;` element. ([#3441](https://github.com//tokiwa-software/fuzion/pull/3441))

- Parser

  - fixed error handling at end of file ([#3474](https://github.com//tokiwa-software/fuzion/pull/3474))

  - fixed parsing of `debug:` qualified in combination with `.this` ([#3437](https://github.com//tokiwa-software/fuzion/pull/3437))

  - removed rule for `dotTypeSuffix` ([#3464](https://github.com//tokiwa-software/fuzion/pull/3464))

  - Fixed error handling for wrong target of env/type expression, now creates error message instead of null pointer exception ([#3438](https://github.com//tokiwa-software/fuzion/pull/3438))

  - cleanup: Add `Num` to methods and fields related to numeric literals ([#3277](https://github.com/tokiwa-software/fuzion/pull/3277))

  - improve syntax error on wrong `set` usage. ([#3262](https://github.com/tokiwa-software/fuzion/pull/3262))

  - lexer: raise error if num literal is followed by letters. ([#3258](https://github.com/tokiwa-software/fuzion/pull/3258))

  - Fix issue parsing tuples ([#3242](https://github.com/tokiwa-software/fuzion/pull/3242)), it is no longer required to use double parentheses in code like

        f(x option ((i32,i32))) => ...

    which can now be written as

        f(x option (i32,i32)) => ...

  - Better errors for choice declared with result type ([#3103](https://github.com/tokiwa-software/fuzion/pull/3103), [#3104](https://github.com/tokiwa-software/fuzion/pull/3104))

- Front End

  - improve errors reported for too restrictive visibility of outer types ([#3432](https://github.com//tokiwa-software/fuzion/pull/3432))

  - performance: cache result of `ResolvedNormalType.isRef()` ([#3448](https://github.com//tokiwa-software/fuzion/pull/3448))

  - Fixed outer type resolution for partial calls resulting in unjustified errors, fix [#3306](https://github.com/tokiwa-software/fuzion/issues/3306) ([#3307](https://github.com//tokiwa-software/fuzion/pull/3307))

  - Minor improvements

    - destructure, update java doc ([#3282](https://github.com//tokiwa-software/fuzion/pull/3282))

    - made code more robust in case of compilation errors. ([#3327](https://github.com//tokiwa-software/fuzion/pull/3327))

    - Improved error shown on failure of a pre- / post-condition, now shows full failing expression ([#3291](https://github.com//tokiwa-software/fuzion/pull/3291))

    - raise error on redefining field ([#3387](https://github.com//tokiwa-software/fuzion/pull/3387))

    - Re-resolve Current if used as partial function, improve [#3308](https://github.com/tokiwa-software/fuzion/issues/3308) ([#3314](https://github.com//tokiwa-software/fuzion/pull/3314))

    - Show declared feature in case of wrong number of actual arguments ([#3329](https://github.com//tokiwa-software/fuzion/pull/3329))

    - Fixed bug when checking actual type par against constraints for choice, fix [#3362](https://github.com/tokiwa-software/fuzion/issues/3362) ([#3386](https://github.com//tokiwa-software/fuzion/pull/3386))


  - fix cryptic feature names in err output ([#3155](https://github.com/tokiwa-software/fuzion/pull/3155))

  - fix reproducibility of order of parsing of source files ([#3188](https://github.com/tokiwa-software/fuzion/pull/3188))

  - Code cleanup

      - cleanup loop code ([#3279](https://github.com/tokiwa-software/fuzion/pull/3279))

      - Delay calls to `AbstractType.checkChoice` to the types checking phase ([#3197](https://github.com/tokiwa-software/fuzion/pull/3197))

      - make error handling for types more fault tolerant ([#3183](https://github.com/tokiwa-software/fuzion/pull/3183))

      - move choiceTypeCheck from type inf to type check ([#3154](https://github.com/tokiwa-software/fuzion/pull/3154))

      - remove unique anonymous feature id ([#3174](https://github.com/tokiwa-software/fuzion/pull/3174))

      - refine lookup0 to consider the scope of definition/usage of a feature ([#3112](https://github.com/tokiwa-software/fuzion/pull/3112))

      - various cleanups ([#3047](https://github.com/tokiwa-software/fuzion/pull/3047)) ([#3050](https://github.com/tokiwa-software/fuzion/pull/3050))

      - improve javadoc of `Destructure` ([#3083](https://github.com/tokiwa-software/fuzion/pull/3083))

  - Bug Fixes

      - fix NPE in Block.checkTypes ([#3226](https://github.com/tokiwa-software/fuzion/pull/3226))

      - Fix post-condition failure on fuzion-lang.dev's `design/examples/arg_choice.fz` ([#3193](https://github.com/tokiwa-software/fuzion/pull/3193))

      - fix unjustified "Block must end with a result expression" ([#3271](https://github.com/tokiwa-software/fuzion/pull/3271))

      - In containsThisType: Fix check-fail on call to `outer()` on type parameter ([#3181](https://github.com/tokiwa-software/fuzion/pull/3181))

      - Workaround for [#3160](https://github.com/tokiwa-software/fuzion/issues/3160) for nested and inherited type features ([#3182](https://github.com/tokiwa-software/fuzion/pull/3182))

      - fix check-failure if redefinition is a choice ([#3079](https://github.com/tokiwa-software/fuzion/pull/3079))

      - Do not create error for auto-generated fields in choice ([#3080](https://github.com/tokiwa-software/fuzion/pull/3080))

- Middle End

  - fix field falsely detected to be redefined ([#3412](https://github.com//tokiwa-software/fuzion/pull/3412))

  - reduce verbosity level of `GLOBAL CLAZZ` output ([#3402](https://github.com//tokiwa-software/fuzion/pull/3402))

  - Move reporting missing implementations of abstracts to DFA, fix [#3359](https://github.com/tokiwa-software/fuzion/issues/3359) ([#3397](https://github.com//tokiwa-software/fuzion/pull/3397))

  - fuir: `isTailCall`, handle case boxing ([#3409](https://github.com//tokiwa-software/fuzion/pull/3409))

  - dfa: When `fz` is run with `-verbose=1`, print the time spent during DFA ([#3401](https://github.com//tokiwa-software/fuzion/pull/3401))

  - Speed up of DFA Analysis, improve [#3391](https://github.com/tokiwa-software/fuzion/issues/3391) ([#3403](https://github.com//tokiwa-software/fuzion/pull/3403))

  - air: do not automatically mark ref results from intrinsics as instantiated ([#3200](https://github.com/tokiwa-software/fuzion/pull/3200))

  - air: for intrinsic constructors mark outers as instantiated as well ([#3206](https://github.com/tokiwa-software/fuzion/pull/3206))

  - air: in abstractCalled-check use `isCalled` instead of `isInstantiated` ([#3061](https://github.com/tokiwa-software/fuzion/pull/3061))

  - dfa: Use `StringBuilder` instead of `say` in `showWhy` ([#3195](https://github.com/tokiwa-software/fuzion/pull/3195))

  - fuir: move `addClasses` to constructor ([#3145](https://github.com/tokiwa-software/fuzion/pull/3145))

- Back ends

  - Preconditions are front-end syntax sugar now, so precondition handling was removed from all back ends([#3285](https://github.com//tokiwa-software/fuzion/pull/3285))

  - JVM back end

    - int/jvm: improved stack trace by using human readable feature names instead of internal names ([#3303](https://github.com//tokiwa-software/fuzion/pull/3303), [#3360](https://github.com//tokiwa-software/fuzion/pull/3360))

    - always do tail call optimization no matter the lifetime, use heap allocation if needed ([#3463](https://github.com//tokiwa-software/fuzion/pull/3463))

    - be/jvm/int: workaround f64 `e^1.0 != e` ([#3239](https://github.com/tokiwa-software/fuzion/pull/3239))

    - ids of long class names appearing in stack traces makes tests fragile and were therefore removed ([#3135](https://github.com/tokiwa-software/fuzion/pull/3135))

  - C back end

      - always do tail call optimization no matter the lifetime ([#3462](https://github.com//tokiwa-software/fuzion/pull/3462))

      - create call `fzE_create_jvm` only if `JAVA_HOME` is set ([#3361](https://github.com//tokiwa-software/fuzion/pull/3361))

      - fix code being generated for effect detected not to be used ([#3366](https://github.com//tokiwa-software/fuzion/pull/3366))

      - fix logic for `FUZION_DEBUG_TAIL_CALL` ([#3408](https://github.com//tokiwa-software/fuzion/pull/3408))

      - follow clang-tidy suggestions ([#3365](https://github.com//tokiwa-software/fuzion/pull/3365))

      - implement dir traversal for win.c ([#3349](https://github.com//tokiwa-software/fuzion/pull/3349))

      - fix "use of an empty initializer is a C23 extension" ([#3139](https://github.com/tokiwa-software/fuzion/pull/3139), [#3209](https://github.com/tokiwa-software/fuzion/pull/3209))

      - fix JAVA_HOME and jvm.dll-path on windows ([#3210](https://github.com/tokiwa-software/fuzion/pull/3210))

      - fix switch-expr in fzE_call_v0/fzE_call_s0 ([#3231](https://github.com/tokiwa-software/fuzion/pull/3231))

      - fix test process on windows ([#3198](https://github.com/tokiwa-software/fuzion/pull/3198))

      - fix void function should not return void expression ([#3227](https://github.com/tokiwa-software/fuzion/pull/3227))

      - improve c11 compat., no empty initializer ([#3190](https://github.com/tokiwa-software/fuzion/pull/3190))

      - win.c remove some includes ([#3215](https://github.com/tokiwa-software/fuzion/pull/3215))

      - Fixed handling of signed integer overflow for 16-bit integers, added compiler switch `-ftrapv`([#3049](https://github.com/tokiwa-software/fuzion/pull/3049))

      - enable more warnings `-Wextra -Wpedantic` etc. ([#3091](https://github.com/tokiwa-software/fuzion/pull/3091))

      - fix issues running tests on windows ([#3098](https://github.com/tokiwa-software/fuzion/pull/3098))

      - fix warning about uninitialized `fzCur` ([#3132](https://github.com/tokiwa-software/fuzion/pull/3132))

  - Interpreter back end

    - improve thread safety ([#3252](https://github.com/tokiwa-software/fuzion/pull/3252))

  - fix test atomic for macOS ([#3057](https://github.com/tokiwa-software/fuzion/pull/3057))

  - cleanup ([#3126](https://github.com/tokiwa-software/fuzion/pull/3126))

- util

  - fix `ArrayIndexOutOfBoundsException` when creating error message ([#3457](https://github.com//tokiwa-software/fuzion/pull/3457))

  - Fix handling of env var `FUZION_DISABLE_ANSI_ESCAPSE` ([#3455](https://github.com//tokiwa-software/fuzion/pull/3455))

  - fix source range length in error message ([#3443](https://github.com//tokiwa-software/fuzion/pull/3443))

  - move non Error specific helper methods to `StringHelper` ([#3430](https://github.com//tokiwa-software/fuzion/pull/3430))

  - sort allocation statistics output ([#3447](https://github.com//tokiwa-software/fuzion/pull/3447))

  - use curly underline instead to display error location ([#3367](https://github.com//tokiwa-software/fuzion/pull/3367))

  - Move handling of properties to FuzionOptions, add env var support ([#3449](https://github.com//tokiwa-software/fuzion/pull/3449))

  - Suppress any further error output during shutdown due to errors, fix #3142 ([#3144](https://github.com/tokiwa-software/fuzion/pull/3144))

- Tests

  - io.dir, align behavior of back ends and add test ([#3270](https://github.com//tokiwa-software/fuzion/pull/3270))

  - add regression tests for [#2194](https://github.com/tokiwa-software/fuzion/issues/2194) ([#3395](https://github.com//tokiwa-software/fuzion/pull/3395))

  - add regression test for [#3406](https://github.com/tokiwa-software/fuzion/issues/3406) ([#3468](https://github.com//tokiwa-software/fuzion/pull/3468))

  - always use `-XmaxErrors=-1` ([#3396](https://github.com//tokiwa-software/fuzion/pull/3396))

  - change filenames for `record_int` ([#3444](https://github.com//tokiwa-software/fuzion/pull/3444))

  - fix `grep: empty (sub)expression` ([#3305](https://github.com//tokiwa-software/fuzion/pull/3305))

  - fix two problems in `check_example` scripts ([#3377](https://github.com//tokiwa-software/fuzion/pull/3377))

  - In scripts for simple C tests, handle PIPE_FAIL (141) ([#3286](https://github.com//tokiwa-software/fuzion/pull/3286))

  - increase stack size `process_buffered_writer` ([#3320](https://github.com//tokiwa-software/fuzion/pull/3320))

  - portable nproc ([#3290](https://github.com//tokiwa-software/fuzion/pull/3290))

  - prepare for removing variable shadowing ([#3311](https://github.com//tokiwa-software/fuzion/pull/3311))

  - use some local mutate for buffered writer and web-server ([#3382](https://github.com//tokiwa-software/fuzion/pull/3382))

  - ast/tests: Do not turn ambiguous type into a free type, fix [#3337](https://github.com/tokiwa-software/fuzion/issues/3337) ([#3339](https://github.com//tokiwa-software/fuzion/pull/3339))

  - ast/tests: Repeat type resolution for arguments in lambda to fix [#3315](https://github.com/tokiwa-software/fuzion/issues/3315) and fix [#3308](https://github.com/tokiwa-software/fuzion/issues/3308) ([#3318](https://github.com//tokiwa-software/fuzion/pull/3318))

  - add regression tests ([#3153](https://github.com/tokiwa-software/fuzion/pull/3153), [#3205](https://github.com/tokiwa-software/fuzion/pull/3205), [#3273](https://github.com/tokiwa-software/fuzion/pull/3273))

  - add test for tail recursion [#1588](https://github.com/tokiwa-software/fuzion/issues/1588) ([#3212](https://github.com/tokiwa-software/fuzion/pull/3212))

  - for simple tests, pre-process err output to avoid common false pos errors ([#3185](https://github.com/tokiwa-software/fuzion/pull/3185))

  - remove skip file from test fz_cmd ([#3272](https://github.com/tokiwa-software/fuzion/pull/3272))

  - improve segfault handling ([#3256](https://github.com/tokiwa-software/fuzion/pull/3256))

  - increase stack size ([#3257](https://github.com/tokiwa-software/fuzion/pull/3257))

  - utf8 output on Windows: add workaround for [#2586](https://github.com/tokiwa-software/fuzion/issues/1588) ([#3175](https://github.com/tokiwa-software/fuzion/pull/3175))

  - fix trailing CR handling for windows test runs ([#3236](https://github.com/tokiwa-software/fuzion/pull/3236))

  - renamed dfa.mk as compile.mk ([#3268](https://github.com/tokiwa-software/fuzion/pull/3268))

  - add regression tests ([#3048](https://github.com/tokiwa-software/fuzion/pull/3048), [#3052](https://github.com/tokiwa-software/fuzion/pull/3052), [#3065](https://github.com/tokiwa-software/fuzion/pull/3065), [#3066](https://github.com/tokiwa-software/fuzion/pull/3066))

  - remove non working workaround ([#3096](https://github.com/tokiwa-software/fuzion/pull/3096))

  - fix syntax error, expected is or => ([#3113](https://github.com/tokiwa-software/fuzion/pull/3113))

  - set LANGUAGE to en_US ([#3122](https://github.com/tokiwa-software/fuzion/pull/3122))

  - explicitly ignore results of expressions ([#3133](https://github.com/tokiwa-software/fuzion/pull/3133))

  - add regression test for #776 ([#3137](https://github.com/tokiwa-software/fuzion/pull/3137))

- `fz` command

  - fz: underline code in errors instead of marking with circumflex ([#3342](https://github.com//tokiwa-software/fuzion/pull/3342))

- Infrastructure

  - bin: fix add_simple_test ([#3394](https://github.com//tokiwa-software/fuzion/pull/3394))

  - scripts: fix more shellcheck issues ([#3381](https://github.com//tokiwa-software/fuzion/pull/3381))

  - makefile: add linter pmd ([#3334](https://github.com//tokiwa-software/fuzion/pull/3334))

  - lsp: initial infrastructure for language server ([#3343](https://github.com//tokiwa-software/fuzion/pull/3343))

## 2024-05-17: V0.088

- General

  - Work a a Fuzion module for a nom-like parser combinator framework has started ([#2808](https://github.com/tokiwa-software/fuzion/pull/2808))

- Documentation

   - readme: add section for installing Fuzion from package manger ([#2919](https://github.com/tokiwa-software/fuzion/pull/2919))

   - ref_manual: Add more examples for comments and strings ([#2939](https://github.com/tokiwa-software/fuzion/pull/2939))

   - Minor fixes in the reference manual ([#2741](https://github.com/tokiwa-software/fuzion/pull/2741)).

   - Added MacOS section to readme ([#2783](https://github.com/tokiwa-software/fuzion/pull/2783)).

- Build infrastructure


  - run_tests: add workaround for macOS ([#3017](https://github.com/tokiwa-software/fuzion/pull/3017))

  - run_tests: show total time ([#2946](https://github.com/tokiwa-software/fuzion/pull/2946))

  - Reduced level of parallelism of test runs ([#2632](https://github.com/tokiwa-software/fuzion/pull/2632))

  - Added test-class dfa for tests not requiring backend ([#2690](https://github.com/tokiwa-software/fuzion/pull/2690))

  - Fixed MacOS build problem related to libjvm.dylib ([#2727](https://github.com/tokiwa-software/fuzion/pull/2727))

  - Added makefile target to fix Java imports ([#2823](https://github.com/tokiwa-software/fuzion/pull/2823))

- Fuzion language

  - A string literal containing exactly one codepoint is now of type codepoint, which inherits from String ([#2676](https://github.com/tokiwa-software/fuzion/pull/2676)). This permits writing

        code := "࿋".val                        # results in `4043`

    instead of

        code := "࿋".as_codepoints.first.val    # results in `4043`

  - Support for multiple inheritance and redefinition of several features ([#3034](https://github.com/tokiwa-software/fuzion/pull/3034))

  - Extend type visibility into type features ([#2999](https://github.com/tokiwa-software/fuzion/pull/2999))

    e.g., in

        abc is
          def is

          type.test(x abc.def) is
            say x

    it is no longer required to qualify the type abc.def, one can use def instead:

        abc is
          def is

          type.test(x def) is
            say x


  - pre/post conditions in redef-ined features now need else/then to clarify that preconditions are weakened and postconditions are strengthened ([#3023](https://github.com/tokiwa-software/fuzion/pull/3023))

  - A new effect fuzion.runtime.fault was added to be used to permit handling of failed pre/post conditions in applications. Support check expressions to use this effect was added ([#2996](https://github.com/tokiwa-software/fuzion/pull/2996), [#3011](https://github.com/tokiwa-software/fuzion/pull/3011))

  - Fix [#2224](https://github.com/tokiwa-software/fuzion/issues/2224): precondition of abstract features ([#2962](https://github.com/tokiwa-software/fuzion/pull/2962))

  - Visibility modifiers are no longer allowed within inner blocks ([#2864](https://github.com/tokiwa-software/fuzion/pull/2864))

  - Support for automatic unwrapping of values ([#2644](https://github.com/tokiwa-software/fuzion/pull/2644)). This enables using values of types like mutate.new or atomic directly without explicit unwrapping using v.get.

  - Functions and constructors declared using { } now also require the is keyword or an arrow => ([#2655](https://github.com/tokiwa-software/fuzion/pull/2655), [#2657](https://github.com/tokiwa-software/fuzion/pull/2657), [#2658](https://github.com/tokiwa-software/fuzion/pull/2658), [#2774](https://github.com/tokiwa-software/fuzion/pull/2774)).

  - Allow then to be placed at minimum indentation ([#2656](https://github.com/tokiwa-software/fuzion/pull/2656)). This permits code as follows:

          if a=b
          then
            say "equal"
          else
            say "not equal"

- Base library

  - Addition of the following new standard library features

    - union/intersection/except etc. in Set ([#2904](https://github.com/tokiwa-software/fuzion/pull/2904))

    - Map.type.empty ([#2831](https://github.com/tokiwa-software/fuzion/pull/2831))

    - as_persistent_map to lock_free.map ([#2977](https://github.com/tokiwa-software/fuzion/pull/2977))

    - container.Persistent_Map ([#2960](https://github.com/tokiwa-software/fuzion/pull/2960))

    - io.buffered.read_line_while ([#2881](https://github.com/tokiwa-software/fuzion/pull/2881))

    - option/outcome: prefix ! ([#2834](https://github.com/tokiwa-software/fuzion/pull/2834))

    - In Buffer, add redef as_string ([#3024](https://github.com/tokiwa-software/fuzion/pull/3024))

    - Add general definition of a mutable map ([#2832](https://github.com/tokiwa-software/fuzion/pull/2832))

  - Removal of the following standard library features

    - numeric.to_u32 ([#3026](https://github.com/tokiwa-software/fuzion/pull/3026))

    - set from ps_map and sorted_array ([#2852](https://github.com/tokiwa-software/fuzion/pull/2852))

    - All uses of thiz in numeric types ([#3038](https://github.com/tokiwa-software/fuzion/pull/3038))

    - Effect map_of_total_order ([#2833](https://github.com/tokiwa-software/fuzion/pull/2833))

    - fuzion.java.Java_Object._ignore_ ([#2923](https://github.com/tokiwa-software/fuzion/pull/2923))

  - Changes to the following standard library features

    - Any.as_string is no longer an intrinsic but implemented in Fuzion directly([#2893](https://github.com/tokiwa-software/fuzion/pull/2893))

    - Rename option.get default to option.or default ([#2925](https://github.com/tokiwa-software/fuzion/pull/2925))

    - For sets, maps and lists, renamed type.from_sequence to type.new ([#2873](https://github.com/tokiwa-software/fuzion/pull/2873))

    - Split up file.write and make dir effect ([#2982](https://github.com/tokiwa-software/fuzion/pull/2982))

    - In spawn change args from array to Sequence ([#2972](https://github.com/tokiwa-software/fuzion/pull/2972))

    - memoize accepts custom mutable maps as args ([#2832](https://github.com/tokiwa-software/fuzion/pull/2832))

    - Make it easier to handle a network request ([#2883](https://github.com/tokiwa-software/fuzion/pull/2883))

    - Make Print_Handler.println public ([#3032](https://github.com/tokiwa-software/fuzion/pull/3032))

    - In Mutable_Array, inherit from Buffer instead of Sequence ([#2870](https://github.com/tokiwa-software/fuzion/pull/2870))

  - Fix code of potentially uninitialized access ([#2866](https://github.com/tokiwa-software/fuzion/pull/2866))

  - A new feature auto_unwrap can be used to enable automatic unwrapping for arbitrary features ([#2644](https://github.com/tokiwa-software/fuzion/pull/2644)).

  - Added features array2.transpose and array2.type.create2 ([#2663](https://github.com/tokiwa-software/fuzion/pull/2663)).

  - Renamed id(T) as identity(T) ([#2705](https://github.com/tokiwa-software/fuzion/pull/2705)). This enables partial application for id(T,x) to do things like l.filter id on a Sequence bool, which was not possible before since the reference to id was ambiguous.

  - Added precondition for array.slice ([#2736](https://github.com/tokiwa-software/fuzion/pull/2736)).

  - Increased buffer size in file.read to 4K ([#2737](https://github.com/tokiwa-software/fuzion/pull/2737)).

  - Removed feature Stream since it require mutable state ([#2769](https://github.com/tokiwa-software/fuzion/pull/2769)). Instead, list should be used to iterate a Sequence.

  - Minor implementation improvements ([#2739](https://github.com/tokiwa-software/fuzion/pull/2739), [#2757](https://github.com/tokiwa-software/fuzion/pull/2757), [#2772](https://github.com/tokiwa-software/fuzion/pull/2772), [#2776](https://github.com/tokiwa-software/fuzion/pull/2776), [#2784](https://github.com/tokiwa-software/fuzion/pull/2784), [#2793](https://github.com/tokiwa-software/fuzion/pull/2793), [#2818](https://github.com/tokiwa-software/fuzion/pull/2818), [#2819](https://github.com/tokiwa-software/fuzion/pull/2819)).

- Parser

  - Performance improvements

    - Lexer performance optimizations ([#2943](https://github.com/tokiwa-software/fuzion/pull/2943), [#2944](https://github.com/tokiwa-software/fuzion/pull/2944), [#2942](https://github.com/tokiwa-software/fuzion/pull/2942))

    - Parse .this as part of callTail or typeTail ([#2980](https://github.com/tokiwa-software/fuzion/pull/2980))

    - Avoid repeated parsing of actual args in a call, instead parse actual as expr and convert to type as needed ([#2953](https://github.com/tokiwa-software/fuzion/pull/2953))

    - Minor performance improvement when parsing operatorExpr ([#2981](https://github.com/tokiwa-software/fuzion/pull/2981))

    - Create second fork in skipOneType a little less eagerly ([#2952](https://github.com/tokiwa-software/fuzion/pull/2952))

    - When parsing name and the result is dumped, do not create ParsedName ([#2948](https://github.com/tokiwa-software/fuzion/pull/2948))

  - Clean up parser rules ([#2974](https://github.com/tokiwa-software/fuzion/pull/2974), [#2994](https://github.com/tokiwa-software/fuzion/pull/2994), [#2976](https://github.com/tokiwa-software/fuzion/pull/2976))

  - Move parsing of .env and .type to callTail ([#2979](https://github.com/tokiwa-software/fuzion/pull/2979))

  - For better error messages, set source range for Expr ([#2995](https://github.com/tokiwa-software/fuzion/pull/2995))

- AST

  - Remove Types.intern ([#2454](https://github.com/tokiwa-software/fuzion/pull/2454))

  - Remove state from abstract interpreter ([#2933](https://github.com/tokiwa-software/fuzion/pull/2933))

  - Loop: various improvements ([#2956](https://github.com/tokiwa-software/fuzion/pull/2956), [#2908](https://github.com/tokiwa-software/fuzion/pull/2908), [#2858](https://github.com/tokiwa-software/fuzion/pull/2858), [#2843](https://github.com/tokiwa-software/fuzion/pull/2843), [#2859](https://github.com/tokiwa-software/fuzion/pull/2859))

  - fix Feature.toString() when used very early in frontend ([#2868](https://github.com/tokiwa-software/fuzion/pull/2868))

  - fix Empty Stack during DFA ([#2768](https://github.com/tokiwa-software/fuzion/pull/2768))

  - fix destructuring where with field name reuse ([#2920](https://github.com/tokiwa-software/fuzion/pull/2920))

  - show warning if -> is used in operator ([#2884](https://github.com/tokiwa-software/fuzion/pull/2884))

  - improved error handling ([#3033](https://github.com/tokiwa-software/fuzion/pull/3033), [#3006](https://github.com/tokiwa-software/fuzion/pull/3006), [#3014](https://github.com/tokiwa-software/fuzion/pull/3014))

  - type inference for arguments for feature with generics ([#3008](https://github.com/tokiwa-software/fuzion/pull/3008))

  - fix dynamic_type used on features defined inside of type features ([#2792](https://github.com/tokiwa-software/fuzion/pull/2792))

- Front End

  - add logic evaluating visibility when inheriting ([#3028](https://github.com/tokiwa-software/fuzion/pull/3028))

  - turn If into match expression early ([#2661](https://github.com/tokiwa-software/fuzion/pull/2661))

  - mark fields as final, improve precondition ([#2837](https://github.com/tokiwa-software/fuzion/pull/2837))

  - merge logic of addInheritedFeature and addToDeclaredOrInherited ([#3019](https://github.com/tokiwa-software/fuzion/pull/3019))

  - For better error messages, source ranges are not stored in Fuziom module files instead of source position ([#2844](https://github.com/tokiwa-software/fuzion/pull/2844))

  - The universe is not written to a fum-file ([#2846](https://github.com/tokiwa-software/fuzion/pull/2846))

  - Better error messages for features defined in modules ([#2654](https://github.com/tokiwa-software/fuzion/pull/2654), [#2670](https://github.com/tokiwa-software/fuzion/pull/2670))

  - Replace streams by lists when a loop index variable iterates over a Sequence ([#2758](https://github.com/tokiwa-software/fuzion/pull/2758))

  - Front end always creates a module file and passes this to the later phases instead of the AST ([#2817](https://github.com/tokiwa-software/fuzion/pull/2817)).

  - Minor implementation improvements ([#2447](https://github.com/tokiwa-software/fuzion/pull/2447), [#2610](https://github.com/tokiwa-software/fuzion/pull/2610), [#2651](https://github.com/tokiwa-software/fuzion/pull/2651), [#2659](https://github.com/tokiwa-software/fuzion/pull/2659), [#2660](https://github.com/tokiwa-software/fuzion/pull/2660), [#2666](https://github.com/tokiwa-software/fuzion/pull/2666), [#2669](https://github.com/tokiwa-software/fuzion/pull/2669), [#2672](https://github.com/tokiwa-software/fuzion/pull/2672), [#2674](https://github.com/tokiwa-software/fuzion/pull/2674), [#2688](https://github.com/tokiwa-software/fuzion/pull/2688), [#2729](https://github.com/tokiwa-software/fuzion/pull/2729), [#2730](https://github.com/tokiwa-software/fuzion/pull/2730), [#2734](https://github.com/tokiwa-software/fuzion/pull/2734), [#2735](https://github.com/tokiwa-software/fuzion/pull/2735), [#2767](https://github.com/tokiwa-software/fuzion/pull/2767), [#2777](https://github.com/tokiwa-software/fuzion/pull/2777), [#2779](https://github.com/tokiwa-software/fuzion/pull/2779), [#2787](https://github.com/tokiwa-software/fuzion/pull/2787), [#2789](https://github.com/tokiwa-software/fuzion/pull/2789), [#2821](https://github.com/tokiwa-software/fuzion/pull/2821))

- Middle End

   - bugfix: Implicitly instantiate type features ([#2987](https://github.com/tokiwa-software/fuzion/pull/2987))

   - air: add !t.isOpenGeneric() to preconditions ([#2968](https://github.com/tokiwa-software/fuzion/pull/2968))

- Fuzion Intermediate Representation

  - fix mixture of endianess for constant data ([#2839](https://github.com/tokiwa-software/fuzion/pull/2839))

  - Code cleanup and simplifications ([#2888](https://github.com/tokiwa-software/fuzion/pull/2888), [#2927](https://github.com/tokiwa-software/fuzion/pull/2927), [#2907](https://github.com/tokiwa-software/fuzion/pull/2907), [#2924](https://github.com/tokiwa-software/fuzion/pull/2924), [#2917](https://github.com/tokiwa-software/fuzion/pull/2917), [#2886](https://github.com/tokiwa-software/fuzion/pull/2886))

  - IR simplifications: remove ExprKind.AdrOf ([#2880](https://github.com/tokiwa-software/fuzion/pull/2880)) and ExprKind.Unit ([#2887](https://github.com/tokiwa-software/fuzion/pull/2887))

  - Improved quality of dumped intermediate code ([#2692](https://github.com/tokiwa-software/fuzion/pull/2692), [#2706](https://github.com/tokiwa-software/fuzion/pull/2706)).

  - Here is an example output:

         > ./build/bin/fz -XdumpFUIR -e "for a := 1, a+1 while a < 10 else say a" | tail -n 30
        Code for #loop0(outer universe#0, a i32) unit
        l3:	0: Current
        	1: Call #loop0.a(outer #loop0) i32
        	2: Const of type i32 00 00 00 04 0a 00 00 00
        	3: Call infix < i32(outer universe#0, a i32, b i32) bool
        	4: Match 0=>l1 1=>l2
        l1:	0: Current
        	1: Call #loop0.a(outer #loop0) i32
        	2: Const of type i32 00 00 00 04 01 00 00 00
        	3: Call i32.infix +(outer i32, other i32) i32
        	4: Current
        	5: Assign to #loop0.a
        	6: Current
        	7: Call #loop0.a(outer #loop0) i32
        	8: Call #loop0(outer universe#0, a i32) unit
        	9: Current
        	10: Assign to #loop0.#exprResult0
        	goto l3_4
        l2:	0: Current
        	1: Call #loop0.a(outer #loop0) i32
        	2: Box i32 => ref i32
        	3: Call say(outer universe#0, s Any) unit
        	4: Current
        	5: Assign to #loop0.#exprResult0
        	goto l3_4
        l3_4:
        	7: Current
        	8: Call #loop0.#exprResult0(outer #loop0) unit
        	9: Current
        	10: Assign to #loop0.#result

  - Added support for a site, i.e., a code position in a class ([#2752](https://github.com/tokiwa-software/fuzion/pull/2752)).

  - Minor implementation improvements ([#2447](https://github.com/tokiwa-software/fuzion/pull/2447), [#2610](https://github.com/tokiwa-software/fuzion/pull/2610), [#2651](https://github.com/tokiwa-software/fuzion/pull/2651), [#2659](https://github.com/tokiwa-software/fuzion/pull/2659), [#2660](https://github.com/tokiwa-software/fuzion/pull/2660), [#2666](https://github.com/tokiwa-software/fuzion/pull/2666), [#2669](https://github.com/tokiwa-software/fuzion/pull/2669), [#2672](https://github.com/tokiwa-software/fuzion/pull/2672), [#2674](https://github.com/tokiwa-software/fuzion/pull/2674), [#2688](https://github.com/tokiwa-software/fuzion/pull/2688), [#2723](https://github.com/tokiwa-software/fuzion/pull/2723), [#2815](https://github.com/tokiwa-software/fuzion/pull/2815), [#2820](https://github.com/tokiwa-software/fuzion/pull/2820))

- Data Flow Analysis

  - Instance: fix toString for NO_SITE ([#2957](https://github.com/tokiwa-software/fuzion/pull/2957))

  - in fuzion.java.call_v0, add third argument to readFields ([#2830](https://github.com/tokiwa-software/fuzion/pull/2830))

  - improve error for target not found ([#2847](https://github.com/tokiwa-software/fuzion/pull/2847))

  - Tailcall handling improved for JVM backend ([#2697](https://github.com/tokiwa-software/fuzion/pull/2697)).

  - Fixed handling of unreachable code following void result that caused errors in backends ([#2698](https://github.com/tokiwa-software/fuzion/pull/2698)).

  - Improved error produced in case of missing effect installation before effect use ([#2732](https://github.com/tokiwa-software/fuzion/pull/2732)).

  - Increased analysis accuracy by distinguishing calls by their call site ([#2753](https://github.com/tokiwa-software/fuzion/pull/2753), [#2755](https://github.com/tokiwa-software/fuzion/pull/2755)) and by comparing effect values ([#2763](https://github.com/tokiwa-software/fuzion/pull/2763)).

  - Improved error produced for uninitialized fields ([#2754](https://github.com/tokiwa-software/fuzion/pull/2754)).

  - Code cleanup ([#2696](https://github.com/tokiwa-software/fuzion/pull/2696), [#2707](https://github.com/tokiwa-software/fuzion/pull/2707), [#2710](https://github.com/tokiwa-software/fuzion/pull/2710), [#2713](https://github.com/tokiwa-software/fuzion/pull/2713), [#2718](https://github.com/tokiwa-software/fuzion/pull/2718), [#2764](https://github.com/tokiwa-software/fuzion/pull/2764)) and fixes ([#2750](https://github.com/tokiwa-software/fuzion/pull/2750), [#2766](https://github.com/tokiwa-software/fuzion/pull/2766), [#2810](https://github.com/tokiwa-software/fuzion/pull/2810)).

- all backends

  - java intrinsics, add parameter: signature ([#2935](https://github.com/tokiwa-software/fuzion/pull/2935))

  - java intrinsics: fix creation and access of java arrays of none-primitive values ([#2998](https://github.com/tokiwa-software/fuzion/pull/2998))

Effect backend

  - Use data flow analysis instead of control flow analysis for -effects analysis ([#2720](https://github.com/tokiwa-software/fuzion/pull/2720)).

- JVM back end

  - fix issue in precondition of drop ([#2963](https://github.com/tokiwa-software/fuzion/pull/2963))

  - fix showing elapsed time stats (-verbose > 1) ([#2848](https://github.com/tokiwa-software/fuzion/pull/2848))

  - Support for exceptions in Java interface ([#2667](https://github.com/tokiwa-software/fuzion/pull/2667)).

  - For -jar backend: Add missing classes needed to handle errors such as StackOverfowError ([#2716](https://github.com/tokiwa-software/fuzion/pull/2716)).

  - Removed dependency on interpreter backend ([#2686](https://github.com/tokiwa-software/fuzion/pull/2686)).

  - Disable tail recursion optimization within precondition ([#2693](https://github.com/tokiwa-software/fuzion/pull/2693)).

  - Fixed epilog code gen for special cases ([#2747](https://github.com/tokiwa-software/fuzion/pull/2747)).

  - Fixed match with case fields in the universe ([#2762](https://github.com/tokiwa-software/fuzion/pull/2762)).

  - Add source file and line number attributes to created byte code ([#2801](https://github.com/tokiwa-software/fuzion/pull/2801), [#2802](https://github.com/tokiwa-software/fuzion/pull/2802)).

  - Increased robustness of intrinsics in case of an thrown Java Error ([#2804](https://github.com/tokiwa-software/fuzion/pull/2804)).

C back end

  - fix field access on ref and value targets ([#2928](https://github.com/tokiwa-software/fuzion/pull/2928))

  - fix nanosleep interruption by signal ([#2885](https://github.com/tokiwa-software/fuzion/pull/2885))

  - initial work for cross compilation ([#2906](https://github.com/tokiwa-software/fuzion/pull/2906))

  - Support for Java interface ([#2604](https://github.com/tokiwa-software/fuzion/pull/2604), [#2712](https://github.com/tokiwa-software/fuzion/pull/2712), [#2780](https://github.com/tokiwa-software/fuzion/pull/2780), [#2800](https://github.com/tokiwa-software/fuzion/pull/2800), [#2816](https://github.com/tokiwa-software/fuzion/pull/2816)).

  - Code cleanup ([#2637](https://github.com/tokiwa-software/fuzion/pull/2637), [#2694](https://github.com/tokiwa-software/fuzion/pull/2694), [#2812](https://github.com/tokiwa-software/fuzion/pull/2812)) and fixes ([#2645](https://github.com/tokiwa-software/fuzion/pull/2645)).

- interpreter back end

  - remove dependency on AST ([#2861](https://github.com/tokiwa-software/fuzion/pull/2861), [#2862](https://github.com/tokiwa-software/fuzion/pull/2862), [#2874](https://github.com/tokiwa-software/fuzion/pull/2874))

  - remove air, base on fuir only ([#2894](https://github.com/tokiwa-software/fuzion/pull/2894))

  - fix java intrinsics, wrap result in outcome ([#2840](https://github.com/tokiwa-software/fuzion/pull/2840))

  - fix position in error Intrinsic feature not supported ([#2954](https://github.com/tokiwa-software/fuzion/pull/2954))

  - use the fuir of DFA ([#2986](https://github.com/tokiwa-software/fuzion/pull/2986))

  - getField performance ([#2929](https://github.com/tokiwa-software/fuzion/pull/2929))

  - Added new interpreter backend based on the Fuzion intermediate representation and AbstractInterpreter ([#2724](https://github.com/tokiwa-software/fuzion/pull/2724), [#2770](https://github.com/tokiwa-software/fuzion/pull/2770), [#2796](https://github.com/tokiwa-software/fuzion/pull/2796), [#2797](https://github.com/tokiwa-software/fuzion/pull/2797), [#2803](https://github.com/tokiwa-software/fuzion/pull/2803)).

  - Increased robustness of intrinsics in case of an thrown Java Error ([#2804](https://github.com/tokiwa-software/fuzion/pull/2804)).

  - Code fixes ([#2726](https://github.com/tokiwa-software/fuzion/pull/2726)).

- util

  - Avoid repeated output of warning statistics ([#2988](https://github.com/tokiwa-software/fuzion/pull/2988))

  - Improve parser performance by

    - pre-calculating results of SourceFile.codePointInLine ([#2940](https://github.com/tokiwa-software/fuzion/pull/2940))

    - Caching line numbers ([#2941](https://github.com/tokiwa-software/fuzion/pull/2941))

  - Respect codepoint width for correct error position display with codepoints that have non-standard width ([#3030](https://github.com/tokiwa-software/fuzion/pull/3030))

  - Better debug output on contract failure in Fuzion's Java code ([#2619](https://github.com/tokiwa-software/fuzion/pull/2619)).

  - Better handling of source ranges in errors massages ([#2678](https://github.com/tokiwa-software/fuzion/pull/2678)).

  - Minor implementation improvements ([#2699](https://github.com/tokiwa-software/fuzion/pull/2699), [#2733](https://github.com/tokiwa-software/fuzion/pull/2733), [#2751](https://github.com/tokiwa-software/fuzion/pull/2751))

- fz and fzjava tools

  - Add option -frontend-only change -no-backend ([#2970](https://github.com/tokiwa-software/fuzion/pull/2970))

  - Show time in ms for building module ([#2849](https://github.com/tokiwa-software/fuzion/pull/2849))

  - fzjava: issue with colon in path, illegal character in opaque ([#2951](https://github.com/tokiwa-software/fuzion/pull/2951))

  - Minor implementation improvements ([#2671](https://github.com/tokiwa-software/fuzion/pull/2671))

- Tests

  - disable tests as workaround for [#3000](https://github.com/tokiwa-software/fuzion/pull/3000) ([#3001](https://github.com/tokiwa-software/fuzion/pull/3001))

  - fix run_tests scripts in case of test failure ([#2891](https://github.com/tokiwa-software/fuzion/pull/2891))

  - add test for [#122,](https://github.com/tokiwa-software/fuzion/pull/122,) [#1055](https://github.com/tokiwa-software/fuzion/pull/1055), [#1031](https://github.com/tokiwa-software/fuzion/pull/1031), [#2321](https://github.com/tokiwa-software/fuzion/pull/2321), [#2380](https://github.com/tokiwa-software/fuzion/pull/2380), [#2346](https://github.com/tokiwa-software/fuzion/pull/2346) ([#2973](https://github.com/tokiwa-software/fuzion/pull/2973), [#2863](https://github.com/tokiwa-software/fuzion/pull/2863), [#3031](https://github.com/tokiwa-software/fuzion/pull/3031), [#2921](https://github.com/tokiwa-software/fuzion/pull/2921))

  - record time ([#2872](https://github.com/tokiwa-software/fuzion/pull/2872))

  - use set_exit_code ([#2964](https://github.com/tokiwa-software/fuzion/pull/2964))

  - Added regression tests ([#2748](https://github.com/tokiwa-software/fuzion/pull/2748)).

  - Add test template script ([#2765](https://github.com/tokiwa-software/fuzion/pull/2765)).

  - Cleanup ([#2759](https://github.com/tokiwa-software/fuzion/pull/2759), [#2778](https://github.com/tokiwa-software/fuzion/pull/2778), [#2807](https://github.com/tokiwa-software/fuzion/pull/2807)) and fixes ([#2811](https://github.com/tokiwa-software/fuzion/pull/2811), [#2814](https://github.com/tokiwa-software/fuzion/pull/2814)).

- Overall Code

  - Added @SuppressWarnings annotations for some unchecked and rawtypes warnings ([#2869](https://github.com/tokiwa-software/fuzion/pull/2869))

  - Code cleanup ([#2708](https://github.com/tokiwa-software/fuzion/pull/2708), [#2709](https://github.com/tokiwa-software/fuzion/pull/2709), [#2711](https://github.com/tokiwa-software/fuzion/pull/2711), [#2746](https://github.com/tokiwa-software/fuzion/pull/2746), [#2785](https://github.com/tokiwa-software/fuzion/pull/2785), [#2791](https://github.com/tokiwa-software/fuzion/pull/2791), [#2798](https://github.com/tokiwa-software/fuzion/pull/2798), [#2822](https://github.com/tokiwa-software/fuzion/pull/2822), [#2824](https://github.com/tokiwa-software/fuzion/pull/2824)) and fixes ([#2742](https://github.com/tokiwa-software/fuzion/pull/2742))


## 2024-03-07: V0.087

- General

  - The Fuzion GitHub repository is open for external contributors, we added a Contributor License Agreement and documentation for new contributors ([#2500](https://github.com/tokiwa-software/fuzion/pull/2500), [#2502](https://github.com/tokiwa-software/fuzion/pull/2502), [#2505](https://github.com/tokiwa-software/fuzion/pull/2505), [#2514](https://github.com/tokiwa-software/fuzion/pull/2514), [#2538](https://github.com/tokiwa-software/fuzion/pull/2538), [#2545](https://github.com/tokiwa-software/fuzion/pull/2545)).

  - We have switched to using JDK 21 ([#2504](https://github.com/tokiwa-software/fuzion/pull/2504)).

- Documentation

  - API documentation is now grouped by feature types ([#2431](https://github.com/tokiwa-software/fuzion/pull/2431)).

  - Work on a Fuzion reference manual as started ([#2490](https://github.com/tokiwa-software/fuzion/pull/2490)).

  - Documentation for the `universe` feature was added ([#2625](https://github.com/tokiwa-software/fuzion/pull/2625)).

  - Type features are now included in the generated API documentation ([#2649](https://github.com/tokiwa-software/fuzion/pull/2649)).

- Build infrastructure

  - Added a `base-only` target to the `Makefile` for faster build-test cycles ([#2501](https://github.com/tokiwa-software/fuzion/pull/2501)).

- Fuzion language

  - A function feature can no longer be declared using `f rt is ...` it must be `f rt =&gt; ...`, declarations using `is` are used only for constructors ([#2401](https://github.com/tokiwa-software/fuzion/pull/2401), [#2443](https://github.com/tokiwa-software/fuzion/pull/2443), [#2444](https://github.com/tokiwa-software/fuzion/pull/2444)).

  - The `.type.` can now be omitted in calls to type features ([#2631](https://github.com/tokiwa-software/fuzion/pull/2631)).

- base library

  - Minor additions to `composition.fz` adding links and more birds, but still work in progress [#2491](https://github.com/tokiwa-software/fuzion/pull/2491).

  - Removed `as_stream` as a step towards removing streams altogether ([#2476](https://github.com/tokiwa-software/fuzion/pull/2476)).

  - Added `Sequence.peek` ([#2475](https://github.com/tokiwa-software/fuzion/pull/2475)).

  - Features `TRUE` and `FALSE` are no longer public, but the corresponding types are ([#2468](https://github.com/tokiwa-software/fuzion/pull/2468)).

  - Added pipe features `&lt;|`, `&lt;||`, etc. to establish symmetry with `|&gt;`, `||&gt;`, etc. ([#2462](https://github.com/tokiwa-software/fuzion/pull/2462)).

  - The `unit.type.monoid` is now `public` ([#2418](https://github.com/tokiwa-software/fuzion/pull/2418)).

  - Removed `say` without arguments to print a new line to enable the use of `say` with one argument in partial applications like `l.for_each say` ([#2406](https://github.com/tokiwa-software/fuzion/pull/2406)).

  - Replaced `is` by `=&gt;` in function features ([#2401](https://github.com/tokiwa-software/fuzion/pull/2401)).

  - Intrinsics and effects that allow directory listing have been implemented ([#1688](https://github.com/tokiwa-software/fuzion/pull/1688), [#2635](https://github.com/tokiwa-software/fuzion/pull/2635)).

  - Add `infix ::` to `list`, which allows creating a list from a given starting element and a partial function that gets applied on each iteration ([#2516](https://github.com/tokiwa-software/fuzion/pull/2516)).

    This permits code like

        ints := 0 :: +1

    to create a list of all integers starting at `0` and calculating the next element by adding one using the partial function `+1`.

  - Change `Sequence.infix |` to be an alias for `map`, the new alias for `for_each` is `Sequence.infix !` now ([#2517](https://github.com/tokiwa-software/fuzion/pull/2517)).

  - Handling of infinite sequences has been made more convenient since the number of entries shown by `as_string` is limited for non-finite sequences ([#2528](https://github.com/tokiwa-software/fuzion/pull/2528)).

    You can now safely do things like

        say (1.. | x->x**2)

    which used to crash, but now results in

        [1, 4, 9, 16, 25, 36, 49, 64, 81, 100, …]

  - Add feature `String.is_ascii` ([#2531](https://github.com/tokiwa-software/fuzion/pull/2531)).

  - `Set.add` was implemented due to errors with the abstract version ([#2533](https://github.com/tokiwa-software/fuzion/pull/2533)).

  - When producing a string from a sequence, separate the elements with `, ` instead of just a `,` ([#2548](https://github.com/tokiwa-software/fuzion/pull/2548)).

  - Facilities to launch processes have been added ([#2575](https://github.com/tokiwa-software/fuzion/pull/2575)).

  - Visibility of `Type` is now `module:public`, because nothing should inherit from or call `Type` ([#2581](https://github.com/tokiwa-software/fuzion/pull/2581)).

  - `bench` now uses the `time.nano` effect ([#2606](https://github.com/tokiwa-software/fuzion/pull/2606)).

  - `String`: add features for center, left, and right padding of strings ([#2609](https://github.com/tokiwa-software/fuzion/pull/2609)).

  - Some fixes for unsafe intrinsics ([#2521](https://github.com/tokiwa-software/fuzion/pull/2521), [#2594](https://github.com/tokiwa-software/fuzion/pull/2594),
    [#2627](https://github.com/tokiwa-software/fuzion/pull/2627)).

  - Refactoring of `interval` ([#2551](https://github.com/tokiwa-software/fuzion/pull/2551), [#2560](https://github.com/tokiwa-software/fuzion/pull/2560), [#2608](https://github.com/tokiwa-software/fuzion/pull/2608)).

- Front End

  - Added visibility checks for `abstract` features ([#2453](https://github.com/tokiwa-software/fuzion/pull/2453)).

  - Improve type inference for tagged union types ([#2582](https://github.com/tokiwa-software/fuzion/pull/2582)).

        # infers that all the numbers to be of type option f64
        a := [0.3, option 3.142, nil, 42]

        # infers that all the numbers to be of type option f64
        if b then 42  else option 3.14

  - Improve type inference type parameters ([#2585](https://github.com/tokiwa-software/fuzion/pull/2585)).

        # type parameters for `zip` can now be inferred in this case:
        prods :=  ([1,2,3].zip 0.. a,b->a*b)

  - Remove string comparisons in the code ([#2616](https://github.com/tokiwa-software/fuzion/pull/2616), [#2617](https://github.com/tokiwa-software/fuzion/pull/2617)).

  - Replace `Could not find called feature` messages by more specific errors in case the feature could be found but is invisible or the argument count does not match ([#2622](https://github.com/tokiwa-software/fuzion/pull/2622)).

  - Suppress `declarationsInLazy` error if it is a subsequent error, due to frequent false-positives ([#2628](https://github.com/tokiwa-software/fuzion/pull/2628)).

  - Fix source range of parsed lambdas ([#2646](https://github.com/tokiwa-software/fuzion/pull/2646)).

- Middle End

  - Significant performance improvement and cleanup in the handling of run-time types (clazzes), ([#2416](https://github.com/tokiwa-software/fuzion/pull/2416), [#2413](https://github.com/tokiwa-software/fuzion/pull/2413)).

- Analyzers

  - `fz -effects` now uses the data-flow analysis (DFA) infrastructure instead of the less accurate control flow graph (CFG) ([#2407](https://github.com/tokiwa-software/fuzion/pull/2407)).

- JVM back end

  - Fixed `VerifyError` for features resulting in void ([#2477](https://github.com/tokiwa-software/fuzion/issues/2477), [#2485](https://github.com/tokiwa-software/fuzion/pull/2485)).

  - Fixed race conditions between warnings printed by JVM back end and errors by running code that resulted in flaky test runs ([#2426](https://github.com/tokiwa-software/fuzion/pull/2426), [#2469](https://github.com/tokiwa-software/fuzion/pull/2469)).

  - The fzjava interface known from the interpreter that allows calling into Java code is now supported by the JVM backend as well ( [#2487](https://github.com/tokiwa-software/fuzion/pull/2487)).

  - Support for the stackmap table attribute was added ([#2499](https://github.com/tokiwa-software/fuzion/pull/2499), [#2556](https://github.com/tokiwa-software/fuzion/pull/2556)).  This allowed switching to class files for JDK version 21.

  - Improvements to tail recursive call optimization (however, this does still not work in many cases) ([#2543](https://github.com/tokiwa-software/fuzion/pull/2543)).

  - The `-jar` and `-classes` backends now create a run script that allows one to easily run the jar file or the classes ([#2547](https://github.com/tokiwa-software/fuzion/pull/2547)).

  - For the `-jar` and `-classes` backends, fix the result of `envir.args` ([#2549](https://github.com/tokiwa-software/fuzion/pull/2549)).

  - The `-jar` and `-classes` backends now support an optional argument `-o=&lt;outputName&gt;` to specify the name of the generated output ( [#2567](https://github.com/tokiwa-software/fuzion/pull/2567)).

- C back end

  - Fixed internal handling of `void` types ([#2466](https://github.com/tokiwa-software/fuzion/pull/2466)).

  - The Boehm Garbage Collector is now enabled by default fixing a giant memory leak ([#2428](https://github.com/tokiwa-software/fuzion/pull/2428)).

  - Separated `include/fz.h` into platform-specific files, refactored ([#2553](https://github.com/tokiwa-software/fuzion/pull/2553), [#2573](https://github.com/tokiwa-software/fuzion/pull/2573), [#2601](https://github.com/tokiwa-software/fuzion/pull/2601)).

  - Replace `__atomic` compiler-specific built-ins by C11 standard atomics ([#2563](https://github.com/tokiwa-software/fuzion/pull/2563)).

  - Not longer emits duplicate casts in generated code ([#2592](https://github.com/tokiwa-software/fuzion/pull/2592)).

  - The generated C source files are not deleted unless `-XkeepGeneratedCode` is set ([#2605](https://github.com/tokiwa-software/fuzion/pull/2605)).

- `fz` tool

  - Run simple examples given on the command line using the `-e &lt;code&gt;` or `-execute &lt;code&gt;` argument ([#2618](https://github.com/tokiwa-software/fuzion/pull/2618)). So it is now possible to do

        fz -e 'say "Hello, world!"'

    to run a one-liner.

  - In the case of a contract failure (pre/post-condition or check) due to a bug in the fz tool, the failed expression will now be shown ([#2619](https://github.com/tokiwa-software/fuzion/pull/2619)).

  - Running `fz` without any arguments now shows the general help instead of the default backend help ([#2648](https://github.com/tokiwa-software/fuzion/pull/2648)).

  - Experimentation with calling the Fuzion compiler via fzjava from the JVM backend have started ([#2536](https://github.com/tokiwa-software/fuzion/pull/2536)).


## 2024-01-11: V0.086

- Fuzion language

  - When redefining an inherited feature, it is now enforced that the visibility
    is not restricted ([#2233](https://github.com/tokiwa-software/fuzion/pull/2233)).

  - Syntax sugar for access to tuple elements now permits using
    `t.0/t.1/...` instead of `t.values.0/t.values.1/...:` One can now use

            t := (3,4)
            say (t.0 + t.1)

    instead of

            t := (3,4)
            say (t.values.0 + t.values.1)

    ([#2392](https://github.com/tokiwa-software/fuzion/pull/2392)).

  - Added support for partial application ([#2265](https://github.com/tokiwa-software/fuzion/pull/2265)). In particular, the following
    partial calls are supported now:

    | partial expression | expands to                    | explanation |
    | ------------------ | ----------                    | ----------- |
    | f x y              | a,b,c -> f x y a b c          | partial call turned into a lambda |
    | f x y              | () -> f x y                   | a call may be turned into a nullary function |
    | ++ x               | a -> a ++ x                   | A prefix or postfix operator may be turned into an infix operator |
    | x ++               | a -> x ++ a                   | dto. |
    | -                  | a -> -a<br>—or—<br>a,b -> a-b | A single operator may become a prefix, postfix, or infix operator in a lambda |
    | .as_string         | a -> a.as_string              | A dot followed by a call is a call in a lambda that receives its target as an argument |
    | -1                 | a -> a - 1                    | a numeric literal with a sign may become an infix operator |

    Note that a single operator given as an argument to a call will be parsed as a postfix or infix operator, so it usually will have to be enclosed in parentheses as in

            (-)

    The same holds for partial application of the target of a call using `.` as in

            (.as_string)

  - When redefining an inherited feature, it is now enforced that the visibility is not restricted ([#2233](https://github.com/tokiwa-software/fuzion/pull/2233)).


- base library

  - `io.buffered.read_string` now may return `io.end_of_file` ([#2227](https://github.com/tokiwa-software/fuzion/pull/2227)).

  - `ctrie` has been moved to the `lock_free` module ([#2238](https://github.com/tokiwa-software/fuzion/pull/2238)).

  - The monoids `all`, `any`, and `parity` have been added to `bool.type` ([#2287](https://github.com/tokiwa-software/fuzion/pull/2287)).

  - Many useful features have been made public:

    - ranges like `codepoint.ascii` [#2289](https://github.com/tokiwa-software/fuzion/pull/2289),
    - `bool.as_option` [#2309](https://github.com/tokiwa-software/fuzion/pull/2309),
    - `io.buffered.read_bytes` [#2332](https://github.com/tokiwa-software/fuzion/pull/2332),
    - function composition features like `atop`, `flip`, etc. defined in `composition` [#2342](https://github.com/tokiwa-software/fuzion/pull/2342),
    - `(has_interval.infix` ..).upper [#2384](https://github.com/tokiwa-software/fuzion/pull/2384),
    - `String.trim` [#2374](https://github.com/tokiwa-software/fuzion/pull/2374),
    - `length` and others in `java.Array` [#2395](https://github.com/tokiwa-software/fuzion/pull/2395).

  - `option` now inherits from `Sequence` ([#2291](https://github.com/tokiwa-software/fuzion/pull/2291)).

  - `time.durations.as_string` has a fixed width now such that nice looking timing tables are easier to create ([#2330](https://github.com/tokiwa-software/fuzion/pull/2330)).

  - A `stack` data type using local mutation is available now ([#2351](https://github.com/tokiwa-software/fuzion/pull/2351)).

  - `map` and `flat_map` are now features of Sequence that produce a new
    Sequence. Specific variants for array or list that produce an array or a
    list have been renamed as map_to_array and map_to_list, etc., resp. ([#2317](https://github.com/tokiwa-software/fuzion/pull/2317)).

  - `count` and `drop` were redefined for array backed sequences to achieve O(1) performance ([#2337](https://github.com/tokiwa-software/fuzion/pull/2337)).

  - `reverse`, `map_pairs`, `fold1`, and `foldf` were added to `Sequence` ([#2341](https://github.com/tokiwa-software/fuzion/pull/2341)).

  - Unused features were removed from `effects.fz` ([#2352](https://github.com/tokiwa-software/fuzion/pull/2352)).

  - For debugging of functions, a feature `log` was added which is like `say`, but instead of returning unit, it returns its argument ([#2297](https://github.com/tokiwa-software/fuzion/pull/2297)).

  - Output of `list.as_string` has been improved for empty entries ([#2314](https://github.com/tokiwa-software/fuzion/pull/2314)).

  - Performance improvements for `io.stdin` and `io.buffered.reader` ([#2343](https://github.com/tokiwa-software/fuzion/pull/2343)).

  - Pipe operators `|>`, `||>`, `|||>`, etc. have been added ([#2355](https://github.com/tokiwa-software/fuzion/pull/2355)), so you can now do things like

            (3,4) ||> (+) |> say

    which will first destructure the tuple into `3` and `4`, then apply `infix +` to add these and pass the result as an argument to `say`.

  - `memoize` is now public ([#2248](https://github.com/tokiwa-software/fuzion/pull/2248)).

  - New feature `Sequence.scan` to create lists of results of folding all prefixes of a `list` ([#2197](https://github.com/tokiwa-software/fuzion/pull/2197)). e.g.,

            [1,2,3,4,5].scan i32.type.sum

    results in

            [1,3,6,10,15]

  - `marray` has been replaced by `mutate.array` ([#1930](https://github.com/tokiwa-software/fuzion/pull/1930)).

  - A new module `lock_free.fum` was added with a lock-free `stack` implementation as a first entry ([#1917](https://github.com/tokiwa-software/fuzion/pull/1917)).


- Front end

  - `this.type` can now be used inside of the corresponding type feature ([#2296](https://github.com/tokiwa-software/fuzion/pull/2296)).

  - Support for partial application with type parameters has been implemented
    ([#2355](https://github.com/tokiwa-software/fuzion/pull/2355)). This was required, e.g., for the pipe operator to work with partial
    application as in

            "Hello" |> say.

  - Error output for this-types now uses `xyz.this` instead of `xyz.this.type` to match the current syntax ([#2356](https://github.com/tokiwa-software/fuzion/pull/2356)).

  - Fixed type inference for numeric literals wrapped in a `lazy` value ([#2363](https://github.com/tokiwa-software/fuzion/pull/2363)).

  - Fixed false detection of ambiguous code due to partial applications ([#2298](https://github.com/tokiwa-software/fuzion/pull/2298)).

  - Improved type inference for function argument types ([#2316](https://github.com/tokiwa-software/fuzion/pull/2316)).

  -  Mark result clazz of intrinsic constructor as instantiated, this paves part of the way for `fzjava` integration with the JVM backend ([#2394](https://github.com/tokiwa-software/fuzion/pull/2394)).

  - A contract whose result type is not bool now results in an error ([#2268](https://github.com/tokiwa-software/fuzion/pull/2268)).

  - Type inference from anonymous feature now results in the base type if that is a ref type, which is typically the desired type ([#2254](https://github.com/tokiwa-software/fuzion/pull/2254)).

  - Added concept of effective visibility which is the combination of the outer
    feature's visibility and its own visibility.  Visibility checks now use
    effective visibility when verifying argument and result type
    visibility. ([#2246](https://github.com/tokiwa-software/fuzion/pull/2246)).

  - Fixed feature lookup for features added in modules ([#2245](https://github.com/tokiwa-software/fuzion/pull/2245)).


- Middle end

  - Reduce recursion depth of `CFG` and `DFA` to improve performance and avoid stack overflows ([#2338](https://github.com/tokiwa-software/fuzion/pull/2338)).

  - Fixed null-pointer exceptions and check-condition failures ([#2357](https://github.com/tokiwa-software/fuzion/pull/2357)).

  - Fixed crashes when empty block is used to return a unit type result that is a tagged element in a choice ([#2267](https://github.com/tokiwa-software/fuzion/pull/2267)).

  - Fixed inheritance of types for the case of a type parameter that is replaced
    by an actual type twice: via the inheritance change and in an outer feature
    ([#2243](https://github.com/tokiwa-software/fuzion/pull/2243)).

  - Support for calls as compile-time constants ([#2067](https://github.com/tokiwa-software/fuzion/pull/2067)).


- JVM back end

  - JVM backend runtime now has its own version of `OpenResource.java` for resource handling ([#2256](https://github.com/tokiwa-software/fuzion/pull/2256)).

  - Reduced code size by joining precondition and routine body into one Java method ([#2196](https://github.com/tokiwa-software/fuzion/pull/2196)).

  - Internal improvements: Added null type ([#2089](https://github.com/tokiwa-software/fuzion/pull/2089)).

  - Code is no longer run in the main thread, but all code is run in an instance of `FuzionThread` ([#2177](https://github.com/tokiwa-software/fuzion/pull/2177)).


- C back end

  - Enable `-fno-omit-frame-pointer` by default to enhance debugging ([#2373](https://github.com/tokiwa-software/fuzion/pull/2373)).

  - Fixed an old and tough bug where a value result from a dynamically bound call could result in a dangling reference on the C stack ([#2260](https://github.com/tokiwa-software/fuzion/pull/2260)).

  - Now uses C's compound literals to create Fuzion's String constants ([#2225](https://github.com/tokiwa-software/fuzion/pull/2225)).


## 2023-10-31: V0.085

- Fuzion language

  - The syntax for a routine with an explicit result type now uses `=>`
    [#2037](https://github.com/tokiwa-software/fuzion/pull/2037) as in

            the_answer u8 => 42

- Front end

  - An error is now produced if actual type parameters do not meet the
    constraints given for the formal type parameter
    [#1054](https://github.com/tokiwa-software/fuzion/issues/1054),
    [#1818](https://github.com/tokiwa-software/fuzion/pull/1818).

  - Improved type inference for empty arrays
    [#1899](https://github.com/tokiwa-software/fuzion/issues/1899),
    [#2124](https://github.com/tokiwa-software/fuzion/pull/2124)) enabling code
    like the following:

            a =>
              if true
                []
              else
                [3]

- JVM back end

  - Added support for `concur.spawn`
    [#2038](https://github.com/tokiwa-software/fuzion/pull/2038)
        and `concur.atomic`
        [#2036](https://github.com/tokiwa-software/fuzion/pull/2036).

  - Improved stack trace printing in case of precondition failure
    [#2049](https://github.com/tokiwa-software/fuzion/pull/2049).

  - Improved stack overflow handling, now shows Fuzion stack trace
     [#2060](https://github.com/tokiwa-software/fuzion/pull/2060).

  - Added support for tail recursion optimization
    [#2051](https://github.com/tokiwa-software/fuzion/pull/2051).

  - Added checks to avoid accidental modification of immutable arrays by
    intrinsic functions
    [#2099](https://github.com/tokiwa-software/fuzion/pull/2099).

  - Added support for memory mapping of files
    [#2126](https://github.com/tokiwa-software/fuzion/pull/2126).

  - The JVM backend is now enabled when running the tests, a build fails
    if the JVM backend fails to run the tests
    [#2149](https://github.com/tokiwa-software/fuzion/pull/2149).

- C back end

  - Improved support for `concur.atomic` to support compare-and-swap for choice
    and value types and avoid accessing uninitialized memory
    [#2123](https://github.com/tokiwa-software/fuzion/pull/2123).

- base library

  - A set of features for function composition was added which is a start
    towards providing a framework for function composition
    [#2046](https://github.com/tokiwa-software/fuzion/issues/2046),
    [#2047](https://github.com/tokiwa-software/fuzion/pull/2047).

  - Added `io.buffered.reader` effect
    [#1554](https://github.com/tokiwa-software/fuzion/pull/1554).

  - The base library type `void` is now implemented as an empty choice type
    [#1962](https://github.com/tokiwa-software/fuzion/pull/1962).  Also added
    default identity features `id` with one argument, that is return, and with
    no argument, that returns a unary identity function.

  - Several visibility fixes
    [#2157](https://github.com/tokiwa-software/fuzion/pull/2157),
    [#2152](https://github.com/tokiwa-software/fuzion/pull/2152).


## 2023-08-08: V0.084

- Fuzion language

  - Preparational improvements and changes with respect to
    visibility (see:
    [#1690](https://github.com/tokiwa-software/fuzion/pull/1690),
    [#1691](https://github.com/tokiwa-software/fuzion/pull/1691),
    [#1695](https://github.com/tokiwa-software/fuzion/pull/1695),
    [#1714](https://github.com/tokiwa-software/fuzion/pull/1714),
    [#1731](https://github.com/tokiwa-software/fuzion/pull/1731),
    [#1734](https://github.com/tokiwa-software/fuzion/pull/1734),
    [#1736](https://github.com/tokiwa-software/fuzion/pull/1736),
    [#1744](https://github.com/tokiwa-software/fuzion/pull/1744))
    have resulted in visibility finally being enforced by the
    Fuzion compiler:
    [#1745](https://github.com/tokiwa-software/fuzion/pull/1745).

  - Type inference for arguments from actuals in a call:
    [#1722](https://github.com/tokiwa-software/fuzion/pull/1722).
    In a declaration like

            point(x, y i32) is


    The argument type `i32` can be omitted if there are calls to
    that feature:

            point(x, y) is

            p1 := point 3 4
            p2 := point 5 12


  - Support for *free types*
    [#1774](https://github.com/tokiwa-software/fuzion/pull/1722)
    to simplify declarations using type parameters. It is now
    possible to write

            first (s Sequence T) => s.nth 0


    instead of

            first (T type, s Sequence T) => s.nth 0


    Free types can be anonymous using `_`:

            first (s Sequence _) => s.nth 0


    one can also use constraints as in

            add_to_all (s array N:numeric, v N) => s.map x->x+v

            say (add_to_all [1, 2, 3] 10)


    or complex type expressions like

            g(v A | (B : numeric)) => ...


  - Explicit `ref` types have been removed:
    [#1705](https://github.com/tokiwa-software/fuzion/pull/1705).
    If a `ref` type variant is required, one must now declare a
    `ref` feature and inherit from this.

  - The type of an outer instance `a` is now just `a.this`, it used
    to be `a.this.type`:
    [#1706](https://github.com/tokiwa-software/fuzion/pull/1706).
    This is consistent with, e.g., `nil` being \--depending on
    context\-- either a type or a call resulting in a value.

  - The `set` keyword to assign a new value to a field is no longer
    permitted:
    [#1720](https://github.com/tokiwa-software/fuzion/pull/1720).

    As an alternative, use the `mutate` effect in your code.

- front end

  - Improve support for lazy values:
    [#1667](https://github.com/tokiwa-software/fuzion/pull/1667),
    [#1672](https://github.com/tokiwa-software/fuzion/pull/1672).

  - Fix a bug which caused unjustified errors when chained booleans
    were used in postconditions:
    [#1685](https://github.com/tokiwa-software/fuzion/pull/1685).

  - The parser and frontend now keep more accurate position
    information for names, in particular in feature declarations:
    [#1735](https://github.com/tokiwa-software/fuzion/pull/1735).

    This improves confusing error messages when multiple feature
    names are given in a declaration. Furthermore, source ranges are
    now supported such that the entire name is now marked in error
    messages.

  - Improve some error messages:
    [#1693](https://github.com/tokiwa-software/fuzion/pull/1693),
    [#1753](https://github.com/tokiwa-software/fuzion/pull/1753).

  - fix check failure in case `x` is not found in `x.this.type`
    [#1718](https://github.com/tokiwa-software/fuzion/pull/1718).

  - permit all calls with `void` as the target type
    [#1725](https://github.com/tokiwa-software/fuzion/pull/1725).

  - fix an issue where in some cases an unjustified "Choice feature
    must not contain any code" error would appear:
    [#1740](https://github.com/tokiwa-software/fuzion/pull/1740).

  - fix contract violations of lineStartPos, for files with errors on
    last line:
    [#1743](https://github.com/tokiwa-software/fuzion/pull/1743).

  - fix NullPointerException when building a bad `base.fum`:
    [#1767](https://github.com/tokiwa-software/fuzion/pull/1767).

  - harmonize indentation handling of `is`, `=>`, `:=`, and `.`:
    [#1768](https://github.com/tokiwa-software/fuzion/pull/1768).

  - flag error when assigning choice element to this type of
    type feature
    [#1793](https://github.com/tokiwa-software/fuzion/pull/1793).

  - ast: handle boxing of erroneous and empty values gracefully
    [#1795](https://github.com/tokiwa-software/fuzion/pull/1795).

  - parser: allow closing brace of formal args list at min indent:
    [#1803](https://github.com/tokiwa-software/fuzion/pull/1803).

  - check that primitives do not contain fields:
    [#1808](https://github.com/tokiwa-software/fuzion/pull/1808).

  - ast: use `Expr` instead of `Stmnt`:
    [#1812](https://github.com/tokiwa-software/fuzion/pull/1812).

  - improve backward type propagation for arrays:
    [#1814](https://github.com/tokiwa-software/fuzion/pull/1814).

    This means that the empty array can now be assigned to fields
    of explicit type `array T`, and its type will be determined
    correctly.

- back end

  - Add intrinsics to get a network socket\'s peer\'s address and
    port information:
    [#1712](https://github.com/tokiwa-software/fuzion/pull/1712).

    This is useful when writing server applications using Fuzion,
    for example to implement IP based access control.

- C back end

  - If the environment variable `FUZION_DEBUG_TAIL_CALL` is set to
    `true`, `fz` will print debug information about tail call
    optimization:
    [#1687](https://github.com/tokiwa-software/fuzion/pull/1687).

  - In the intrinsics for atomic values, use atomic compare and swap
    operations when possible, instead of emulating this using a
    global lock:
    [#1689](https://github.com/tokiwa-software/fuzion/pull/1689).

  - C FFI: `native` features have been implemented, which in the C
    backend, allow C functions to be called:
    [#1772](https://github.com/tokiwa-software/fuzion/pull/1772).

    To work with this, the C functions need to be reasonable simple,
    that is their arguments and return value must be integers,
    floating point numbers, or Strings. Additionally, Fuzion will
    not automatically take care of including the right headers and
    passing the proper arguments to the compiler and linker.

    `native` features are not supported in the interpreter.

- base library

  - The features `equatable`, `orderable`, `partially_orderable`
    have been renamed and moved into the grouping feature
    `property`:
    [#1466](https://github.com/tokiwa-software/fuzion/pull/1466).

  - The `CTrie` data structure now uses atomic compare and swap
    operations, in an attempt at making it actually thread-safe and
    lock-free:
    [#1598](https://github.com/tokiwa-software/fuzion/pull/1598).
    There are however still remaining issues related to taking
    snapshots in a multi-threaded environment.

  - Fuzion now supports IEEE floating point comparisons:
    [#1644](https://github.com/tokiwa-software/fuzion/pull/1644).
    In particular, comparing against `NaN` always results in
    `false`. IEEE semantics for floating point comparison are used
    for infix operations `=` or `<=`, etc.

    In contrast, `type.equality` and `type.lteq` for floats define
    an equality relation and a total order in a mathematical sense.
    These will be used in type parametric code requiring `equatable`
    or `orderable` and they can be used directly via
    `equals f64 a b` or `lteq f64 a b` defined in the base library.

  - Add a `join` operation to the `concur.threads` effect:
    [#1651](https://github.com/tokiwa-software/fuzion/pull/1651).

    This allows waiting for the end of threads in another thread.

  - Parameterize the `try` effect
    [#1696](https://github.com/tokiwa-software/fuzion/pull/1696).

    This makes it possible to use multiple different instances of
    the `try` effect at the same time.

  - `plus`, `minus`, `sign` moved from the universe to the `num`
    hierarchy:
    [#1702](https://github.com/tokiwa-software/fuzion/pull/1702).

  - `wrapping_integer` is now `num.wrap_around`:
    [#1719](https://github.com/tokiwa-software/fuzion/pull/1719).

  - Rename effect handlers in a consistent manner:
    [#1729](https://github.com/tokiwa-software/fuzion/pull/1729).

  - Move some features to the type features of `ps_map`:
    [#1756](https://github.com/tokiwa-software/fuzion/pull/1756).

  - Use the Ryū algorithm, which is implemented in pure Fuzion, to
    convert floating point numbers to strings:
    [#1758](https://github.com/tokiwa-software/fuzion/pull/1758).

  - Fix issues which caused the `fuzion.sys.misc.unique_id`
    intrinsic (used by the `mutate` effect) and `say` to not be
    thread-safe:
    [#1762](https://github.com/tokiwa-software/fuzion/pull/1762),
    [#1764](https://github.com/tokiwa-software/fuzion/pull/1764).

  - implement `float.type.atan2` in Fuzion, to fix an inconsistency
    between the implementations of `glibc`, `musl`, the OpenBSD `libc`,
    among others:
    [#1791](https://github.com/tokiwa-software/fuzion/pull/1791).

  - move `ps_sets` to `ps_set.type`:
    [#1738](https://github.com/tokiwa-software/fuzion/pull/1738).

  - parameterized `ryu` to also work with `f32` natively:
    [#1802](https://github.com/tokiwa-software/fuzion/pull/1802).

  - rename `quantors` as `quantor`:
    [#1810](https://github.com/tokiwa-software/fuzion/pull/1810).

- fz tool

  - Do not erase internal names from fum files by default:
    [#1623](https://github.com/tokiwa-software/fuzion/pull/1623).
    This allows better debugging of the standard library.

  - fix index out of bounds in `showInSource`
    [#1746](https://github.com/tokiwa-software/fuzion/pull/1746).

- tests

  - The simple C tests are now stricter and fail as well when the
    error they return differs from the expected error:
    [#1710](https://github.com/tokiwa-software/fuzion/pull/1710).

## 2023-07-03: V0.083

- Fuzion language

  - Fix the handling of types, in particular in conjuction with covariance and
    type constraints #1612, #1620, #1627, #1629, #1650. This allowed the removal
    of the type parameter from numeric, see below.

  - Some other bugs in the implementation have been fixed: #1616, #1619, #1648.

  - The redefine keyword has been removed from Fuzion (#1411). In contrast to
    the synonymous redef, this keyword was barely used in the standard library.

  - Fix the handling of calls to boxed values (#1426). When a function is called
    on a boxed value, the target of the call is no longer the ref instance, but
    the value instance that was boxed.

  - This change sounds subtle, but it fixes a number of problems when the
    original value type is used as the type of inner features of the boxed value
    type feature and these types must be value types even after boxing. All the
    details can be found in the the pull request description.

  - Calling type features from type parameters has been fixed (#1471).

  - Fix an IndexOutOfBoundsException in chained booleans (#1511).

  - Allow the redefinition of features with type parameters (#1515).

  - Fix a NullPointerException when inheriting from a feature that does not
    exist (#1509).

  - Fix the propagation of types in covariant features (#1523).

- base library

  -  Add the array.type.new type feature as an alternative to the array
     constructor, but also add a array-constructor-like feature marray as an
     alternative to marray.type.new #1566.

   - Remove the use of the stream feature in some features, and if possible,
     return list instead of Sequence #1569.

   - Remove the Sequences feature #1579.

   - The new concur.atomic feature allows atomic accesses to a value #1580,
     #1606.

   - String.type.from_array has been renamed as String.type.from_marray #1581.

   - The unicode feature has been moved into the character_encodings feature
     #1584.

   - The stdout feature has been removed, use say or yak instead #1585.

   - The some feature has been removed, if you need a wrapper type for choice or
     similar, define your own local wrapper type #1589.

   - The ryu feature is now grouped in the num feature #1600.

   - The type parameter has been removed from numeric and its heirs
    #1622. this.type-covariance is now used instead.  fz tool

   - The -XjavaProf option has been added, which can create a flame graph for
     profiling purposes #1555, #1557.

  - More features, including public-facing ones have been renamed to match the
    Fuzion naming convention (#1412, #1367, #1439).

  - Basic support for networking with TCP and UDP sockets landed (#1223). A
    basic demo webserver written in Fuzion has been added (#1456).

  - Ryu, a float to string algorithm has been implemented in Fuzion
    (#986). Currently, this exists besides the f32.as_string and f64.as_string
    features, which use the float to string operation of the underlying
    backend. Since the output of this varies depending on whether the
    interpreter or the C backend is used, the long-term goal is to replace the
    implementation of as_string by Ryu.

  - The effects io.file.use and io.file.open have been added (#1428), which
    simplify working with files from Fuzion code.

  - In an effort to reduce clutter in the base library, several features have
    been grouped in common outer features, such as complex, fraction, matrix ,
    which are now grouped in the num feature (#1440), or Map, Set, and friends,
    which are in a feature container now (#1465).

  - More features have been moved to type features (#1469, #1482, #1484, #1483,
    #1487, #1488). In particular numeric and heirs use type features now
    (#1506).

  - String.is_ascii_white_space is now u8.is_ascii_white_space (#1494).

  - Remove the Functions feature (#1510). Function composition is now
    implemented for Unary features.

  - Implement the String.upper_case and String.lower_case features (#355).

  - Add memoize feature, which remembers the result of a call to a given feature
    with a given value after the first computation (#1522).

  - Return an error if io.stdin.read_line is called at the end of a file
    (#1531).

  - mutable_tree_map is a heir of Map now (#1534).

  - time.date_time inherits from has_total_order now (#1539). This means that
    date_times can be compared using the standard infix operators now.

- C backend

  - Fix a crash in the garbage collector when using threads (#1397).

  - Change the name of the binary that is created when the main feature is
    omitted from #universe to universe (#1407).

  - Fix compilation of C code generated with non-glibc standard libraries, such
    as musl (#1417).

- fz tool

  - An option -no-backend has been added (#1429), which does not run any backend
    operations. This is useful for checking a Fuzion program for syntax and type
    correctness.

  - Optimized execution time of the HelloWorld example by adding caching to the
    compiler (#1526).

## 2023-05-16: V0.082

- Fuzion language

  - semantics of boxed values have been revised: When using a value type that is
    boxed into a corresponding `ref` type (e.g. an `i32` assigned to `Any`),
    calls to function features will now be performed on the value type (`i32`)
    within the boxed instance (`ref i32`). Only constructors will use the boxed
    ref type as their outer type.  This fixed #1313 and #1296.

  - dynamic binding is now permitted for features with type parameters

  - An attempt to redefine a choice feature now results in an error (#1304).

  - Fixed handling of the choice of syntax (#1289):

        # no declaration of the apple, pear, or banana features here!
        fruit : choice of apple, pear, banana.

    The code in the example above used to work, but this broke and went unnoticed for a while.

   - \' is no longer a valid escape in string constants. This is because ' is a
     valid character in strings without an escape as well (#1315).

   - The multi-line strings presented in the last Fuzion Update have been
     extended to allow the dismissal of newlines. This allows wrapping long
     strings in Fuzion code that is not supposed to contain line breaks (#1299).

        s := """
          hello \
          world"""

   - The `fun` keyword has been removed from Fuzion (#1321, #1325). Lambdas
     should be used instead.

   - the `redefine` keyword has been removed, `redef` should be used instead.

- base library

   - grouped complex, fraction and matrix feature in num outer feature

   - renames according to naming conventions

   - new effects io.file.use and io.file.open

   - searchable_list has been merged into searchable_sequence (#1333).

   - More features, including public-facing ones have been renamed to match the
     Fuzion naming convention (#1337, #1338, #1339, #1340, #1341, #1345, #1356,
     #1366, #1368).

   - The spit feature has been removed (#1343) use the equivalent yak feature to
     print text without a new line..

   - Extended the exit effect to allow setting an exit code which is used later
     when calling exit without arguments (#1346). This is useful in tests, for
     instance.

   - Many effects in the standard library now inherit from simpleEffect (#1370,
     #1371, #1372, #1373, #1374, #1375). This decreases code complexity
     significantly. interpreter backend

   - The intrinsics for setting and clearing environment variables have been
     implemented to always return failure. This is because Java does not support
     modifying environment variables at runtime, but avoids compiler errors when
     trying to use these features from the interpreter backend (#1336).

- fz tool

   - Allow specifying -1 as the maximum warning or error count. In this case, an
     unlimited amount of warnings respectively errors is shown (#1332).

   - Compatibility with different versions of the Clang compiler has been
     improved when compiling code from the C backend (#1384).  tests

   - Tests for nested option have been added (#1316). This will ensure nested
     options continue to work as they do right now even though changes to their
     internals are planned.


## 2023-04-05: V0.081

- Fuzion language

  - Introduced syntax sugar for lazy evaluation.

  - The code for handling of outer types has been rewritten entirely.

  - Parser has been improved to distinguish infix : from : used in inherits clause.

  - Support for Multi-line strings literals

  - Improved type inference of type parameters, e.g., `3 = f` now works for `f`
    being of any numeric type, not only for `i32`.

  - Using `universe.this.x` it is now possible to access a feature named `x`.

  - type features can now access the type parameters of the underlying feature, e.g.,

      Sequence (T type) is
        ...
        type.empty Sequence T is
          lists.empty T

   - added syntax for effects in feature declarations.

      HelloWorls ! io.out is
        say "HelloWorld!"

- base library

  - added Mutable_Linked_List and mutable_tree_map

  - renamed has_equality as equatable

  - lazy variants of some features where removed, the main variants are now lazy

  - `Unary` function type has been introduced.

  - `Monoid` no longer defines its own infix ==, but uses equatable

  - `time.date_time` and `time.now` were added

  - `mutate` now supports local mutation

  - `comparable_sequence` was merged into `searchable_sequence`

  - `CTrie` can now be created using a sequence of key-value pairs (#1073)

  - uses of `stream` removed for planned removal of `stream` feature

  - `String` now supports `split_n`, `split_after`, and `split_after_n`

  - `list.init` was added.

  - clean up in `fuzion.sys.fileio`

  - on Windows, `stdout` and `stderr` is now opened in binary mode.

  - documentation has been improved for many standard library features.

  - more consistent naming: `string` -> `String`, `Object` -> `Any`, or `mapOf`
    -> `map_of`

  - equality testing now done using `type.equality`

  - Haskell-style `infix :` operator for lazy lists was added

  - added `Sequence.infix []` with no argument, returns a function that takes
    an index, and returns the element of the sequence at the given index.

  - Replaced `hasHash`, `partially_ordered`, and `ordered`by `has_hash`,
    `has_partial_order`, and `has_total_order`.

- fz tool

  - quadratic compilation time was improved

  - formatting of code in error messages more readable now

  - on a call with wrong arg count, error message now proposes solution

  - some error messages have been improved.

  - output of `-XdumpFUIR`option improved for better debugging (#1110)

  - fixed many bugs relating to type features

- interpreter back-end

  - clean-up in handling of open resources like files

- C back-end

  - windows support for fileio and env vars has been improved (#1087)


- fzjava tool

  - improved performance by lazily accessing data from dependent `.fum` files.

- fzdocs tool

   - now creates links to redefined features

   - removed emojis in the page titles, 🎆 (effects) and 🌌 (universe)

   - private features are now hidden from the documentation by default.

## 2023-01-12: V0.080

- Fuzion language

  - type features can now access the type parameters of the underlying feature.

  - Support for covariant feature redefinition:

    `a.this.type` can now be used to refer to the type of an outer feature
    `a`. This can be used, e.g., to ensure that an argument or result of a
    feature has the same type as the target. E.g., `numeric.infix +` can require
    the argument and result to be of type `numeric.this.type` such that in a
    redefinition `i32.infix +` requires an argument of pf type `i32` and
    produces a result of the same type.

  - Modifiers `synchronized`, `const` and `leaf` or no longer supported (but are
    still reserved keywords).

  - Added modifier `fixed` for features that should not be inherited by children
    of the outer feature.

  - A type parameter `T` with constraint `c` is now declared using `T type : c`
    which is in sync with the syntax for feature inheritance `x : c is ...`.


- base library

  - moved many features from plural-form unit type features to type features,
    e.g. `Sequences.empty` is now `Sequence.type.empty`.

  - lots of renaming from camelCase to snake_case as in ` mapOf` -> `map_of`.

  - renamed `Object` as `Any`.

  - renamed `string` as `String` (`ref` features should start with a capital).

  - added `type.equality` to several features.

  - `infix ∪` and `infix ∩` are now used for union and intersection instead of
    `⋃` and `⋂`.

  - `unit.type.monoid` was added.

  - `file.io` now supports `seek` and `file_position`. `exists` was replaced
    by `stats`/`lstats`.

  - `String` now supports `find_last`, `to_valid_utf8`, `fields_func`, `cut`.

  - Indices in strings are now usually byte indices in the uft8-encoding, e.g.,
    `String.substring` and `String.split` now use utf8-byte indices as
    arguments.  Features working on codepoints were renamed for clarity:
    `pad_start` as `pad_codepoint_start` and `substring_codepoint`.

   - `list` now has features `prepend_to_all` and `intersperse`, no longer
     supports `forceHead`.

   - `outcome` now is a monad such that verbose match-statements can
     be replaced by concise calls to `bind`.

- fz tool

  - back-end now uses `-Wall -Werror` by default.

- fzjava tool

  - improved performance by lazily accessing data from dependent .fum files.



## 2022-12-08: V0.079

- Fuzion language

  - An implicit (empty) else-branch in an if-statement now results in 'unit'.

  - Inheritance for type features:

    Abstract type features using arguments or results of the corresponding type
    can now be redefined using the actual type. For now (until better syntax is
    supported) the implicit generic type 'THIS_TYPE' is used in the abstract
    feature.

  - conflicting argument names as in

      f(i, i i32) is

    now result in an error.

  - prefix, postfix and infix operators can now be declared with type parameters
    and one (prefix, postfix) or two (infix) value arguments:

      prefix $$(X type, a X) => "type:{X.name} val:$a"

    permits a call '$$x1' if the operator is declared in the current (or outer)
    feature.  Type parameters in this case are inferred from the actual
    arguments.

  - A 'match' statement now may have an empty list of cases

  - Syntax improvements in '.type' and '.env' expressions.

  - Updated to support Unicode 15.

  - Disallow declaration of uninitialized fields as in 'f i32 := ?'.

  - tuple types now can use '(A,B,C)' syntax, e.g.,

      triple(T type, v T) (T,T,T) is
        (v,v,v)

    is equivalent to

      triple(T type, v T) tuple T T T is
        (v,v,v)

  - function types and lambda expressions can no longer be declared using the
    'fun' keyword. Instead, syntax like '(i32, bool) -> string' or '(x,y) -> x +
    y*3' has to be used.

  - Fuzion grammar cleanup: new rule 'actual' for actual arguments, which could
    be an expression for a value argument or a type for a type argument

  - cleanup and simplification in indentation rules for actual arguments and
    cases in a match-expression.

  - ASCII control codes 'HT', 'VT', 'FF' and stray 'CR' are now rejected in the
    source code.

- base library

  - added effect to new feature 'io.file': 'exists', 'delete', 'move',
    'create_dir', 'stat.exists', etc.

  - fixed spelling errors, updated syntax for type parameters (comments still used '<'/'>')

  - removed 'java' feature and all inner features 'java...', these were unused.

  - Fuzion modules now exist for most standard Java modules:

      'java.base' 'java.compiler' 'java.datatransfer' 'java.desktop'
      'java.instrument' 'java.logging' 'java.management' 'java.management.rmi'
      'java.naming' 'java.net.http' 'java.prefs' 'java.rmi' 'java.scripting'
      'java.security.jgss' 'java.security.sasl' 'java.se' 'java.smartcardio'
      'java.sql' 'java.sql.rowset' 'java.transaction.xa' 'java.xml.crypto'
      'java.xml' 'jdk.accessibility' 'jdk.attach' 'jdk.charsets' 'jdk.compiler'
      'jdk.crypto.cryptoki' 'jdk.crypto.ec' 'jdk.dynalink' 'jdk.editpad'
      'jdk.httpserver' 'jdk.jartool' 'jdk.javadoc' 'jdk.jconsole' 'jdk.jdeps'
      'jdk.jdi' 'jdk.jdwp.agent' 'jdk.jfr' 'jdk.jlink' 'jdk.jpackage'
      'jdk.jshell' 'jdk.jsobject' 'jdk.jstatd' 'jdk.localedata'
      'jdk.management.agent' 'jdk.management' 'jdk.management.jfr'
      'jdk.naming.dns' 'jdk.naming.rmi' 'jdk.net' 'jdk.nio.mapmode' 'jdk.sctp'
      'jdk.security.auth' 'jdk.security.jgss' 'jdk.xml.dom' 'jdk.zipfs'

  - renamed several features from 'camelCase' to 'snake_case' or
    'Capitalized_snake_case' for 'ref' types.

  - abstract equality (see https://fuzion-lang.dev/design/equality") as explained by
    Noble et al in The Left Hand of Equals
    (http://web.cecs.pdx.edu/~black/publications/egal.pdf) is now supported:

    base library feature 'has_equality' defines an abstract feature
    'has_equality.type.equality' that can be redefined for heir features such as
    'string'.  Note that the equality operation is bound to the type, so
    different types along an inheritance hierarchy may define different equality
    operations.  This is in strong contrast to object-oriented approaches like
    Java that inherit 'Object.equals'.

    For now, features 'equals' and 'infix ≟' can be used to compare
    according to abstract equality as defined by the operand's type. Eventually,
    'infix =' will be used for this.

    'type.equality' was defined for base library features without type
    parameters (type features are not yet supported for features with type
    parameters)

  - Added features to set env variables.

  - In some library features, 'forAll' was renamed as 'for_each' to avoid
    confusion with 'infix ∀'.

  - Improved documentation of many features

  - added bit-wise-not operation 'prefix ~' to 'wrappingInteger'.

  - support for big integers via Fuzion features 'int' and 'uint'.

  - 'numericSequence' now inherits from 'hasEquals'

  - removed uses of 'stream' from 'string' in favor of 'list'.

  - added new module 'terminal.fum' providing ANSI escape codes. The main
    purpose of this module at this time is to provide a simple test for modules.

  - added 'ctrie' hash tries.

  - added 'ascii' for ASCII control codes.

  - generally replaced generics using '<'/'>' by type parameters.

  - improved syntax of type-expressions: Now using '(list i32).type' instead of
    the awkward 'list i32 .type'. Same for '.env'.

  - added 'cache' effect to allow a simple caching of results.

- fz tool

  - C back-end now provides options '-CC=' and '-CFlags=' to specify C compiler
    and options to be used.

  - arguments following main feature are now passed to Fuzion code via the
    'envir.args' effect.

  - new option '-moduleDir' to specify where to load modules from.

  - added option '-sourceDirs={<path>,..}' to specify where to search for sources. If this option is not provided, the current
    directory is used to search for source files.  Otherwise, the (possibly empty) list of paths provided via this option is used.

  - Added option '-saveLib=<path>' to create a module file.

  - Removed option '-XsaveBaseLib' and added '-XloadBaseLib=(on|off)' to disable
    loading of 'base.fum' module which is needed when creating 'base.fum'.

  - new option '-XdumpModules={<name>,..}' to print the contents of a loaded
    module as a colored hex dump with comments.

  - Support for several module files.  Except for the base module 'base.fum'
    that does not depend on another module, module support is still incomplete,
    e.g., modules may not use type features with types from other modules.

- fzjava tool

  - Now accepts '-modules' and '-moduleDirs' arguments to specify Fuzion modules
    already generated that a Java module depends on.  This avoids repeated
    declaration of unit-value features for Java packages.

- fzdocs tool

  - has been added to the main Fuzion repository.

- C back-end

  - Fixed use of thread locals on Windows

- tests

  - added 'tests/indentation/indentation' and
    'tests/indentation_negative/indentation_negative' to illustrate and check
    Fuzion's indentation rules for code blocks and arguments.

  - added several regression tests for bug fixes

- Build infrastructure

  - Some changes to allow building on NixOS


## 2022-08-15: V0.078

- parser

  - unified handling of indentation for blocks of statements, actual arguments,
    match and .. ? .. | .. expressions, etc.

  - actual arguments may now be indented and broken into several lines as
    follows

      call_with_four_args arg1 arg2 arg3 arg4

      call_with_four_args arg1 arg2
                          arg3 arg4

      call_with_four_args arg1
                          arg2
                          arg3
                          arg4

      call_with_four_args
        arg1 arg2 arg3 arg4

      call_with_four_args
        arg1 arg2
        arg3 arg4

      call_with_four_args
        arg1
        arg2
        arg3
        arg4

  - fixed several corner cases related to indentation

  - Exceptions to single-line expression as in

      if expr ? a => true
              | b => false
        say "true"

    have been removed, now requires parentheses

      if (expr ? a => true
               | b => false)
        say "true"

  - 't.env' and 't.type' expression now permit the type 't' to be in parentheses
    '(t).env' and '(t).type' such that type parameters can be provided '(list
    i32).type' or '(cache myType).env'.

  - remove support for function types using 'fun' keyword, must use lambda
    notation such as '(i32, bool) -> string' instead.

- base lib

  - started adding support for file I/O

  - new feature 'ascii' defining ASCII control codes

  - removed use of '<'/'>' for generics in favor of new type parameter syntax.

  - added 'cache' effect to allow a simple caching of results.

- tests

  - added positive and negative tests for indentation: 'indentation' and
    'indentation_negative', contain many examples of good and bad
    indentation.


## 2022-08-03: V0.077

- Fuzion language

  - added syntax sugar for tuple types like '(i32, list bool)'.

  - allowed types to be put in parentheses, i.e., '(stack i32)' is the same as
    'stack i32'.  Syntax of '.env' and '.type' will require parentheses, i.e.,
    instead of 'stack i32 .type' one will have to write '(stack i32).type' and
    similarly for '.env'.

  - changed parser to allow actual arguments passed to type parameters to be any
    type, including function types '(i32)->bool' or tuples '(i32, string)'.

  - removed support for lambdas using 'fun' keyword, i.e., instead of 'fun (x
    i32) -> x+x', you have to write 'x -> x+x'.  Type inference from a lambda is
    not possible, though.

  - features with empty argument list and a result type that is put in
    parentheses (usually a tuple) now require an empty argument list '()' since
    the result type would otherwise be parsed as an argument list.

- C backend

  - use DFA to remove fields and calls to read (removed) unit type fields

  - do not create code for intrinsics that are found to be unreachable during
    DFA.

- DFA

  - cleanup and minor enhancements


## 2022-07-27: V0.076

- C backend

  - GC support using the Boehm–Demers–Weiser garbage collector. Use '-useGC'
    option for the 'fz' command.

  - result of application-wide data-flow analysis is now used to control code
    generation resulting in smaller binaries and (usually) faster build
    time. This can be controlled using 'fz' with option '-Xdfa=(on|off)'.

- FUIR

  - Added generic abstract interpreter that provides the basic infrastructure
    for abstract interpretation.  This abstract interpreter is used for the C
    backend and will be used for other backend and static analysis such as
    data-flow analysis.

  - added application-wide data-flow analysis

  - 'fz' command has new option '-dfa' that runs DFA analysis as backend. Should
    be used with '-verbose=<n>' for 'n>0' to get ouput other than just errors.

- base lib

  - started adding support for file I/O.

  - 'mapOf' now can be used with an array of '(key, value)' tuples

  - 'asString' on mutable value now calls asString on the value.

## 2022-07-04: V0.075

- C backend

  - tail recursive calls are now optimized using a goto to the beginning of the
    feature's code.

- FUIR

  - added analysis to detect tail calls and escape analysis for 'this' instance.
    The analysis results are used for tail recursion optimization in the C
    backend.

- fzjava

  - fixed passing of arrays of references when calling Java code from Fuzion
    features.

- throughout

  - many bug fixes


## 2022-05-31: V0.074

- Fuzion language

  - Overloading with type parameters now works as follows: A feature f with
    m type parameters and n value parameters can be called with m+n actual
    arguments (m types and n values) or, if the types can be inferred from the
    value parameters, with just n actual value arguments.

    If an overloaded f with n formal arguments (type or value) exists, a call
    with n actual arguments will always call this f with n formal arguments.

- fz command

  - bug fixes for #113, #297, #300

- base lib

  - some changes towards using type parameter arguments instead of generics
    using '<' / '>'

## 2022-05-18: V0.073

- Fuzion language

  - add support for calling type parameters, i.e., in a feature `f(X type)`, it
    is now possible to call `X` as in `say X`.  The result is an instance of
    feature `Type`.

  - add support to declare inner feature in type feature, e.g.
    ```
      i32.type.blabla is say "this is blabla in i32.type"
    ```
  - type features now inherit from the type features corresponding to the
    plain features their corresponding plain feature inherits from.

- stdlib

  - added `fract`, `floor`, `ceil` to `float`.

  - fixed `stream.asList` to work if called repeatedly.

  - fixed `print` and `println` to stdout/stderr to print the whole string
    atomically at once, i.e., no longer split it up into single characters.

  - added `io.stdin`.

  - added `Functions.compose`.

  - added effect `mutate`. A mutable field `f` with initial value `v` can now be
    created using `f := mut v`

- fum/fuir:

  - add support for type parameters in .fum file

## 2022-04-23: V0.072

- Front End

  - improved type inference for results of lambdas: Code like
    ```
      l.map<T> (x -> f x)
    ```
    no longer need '<T>', i.e.,
    ```
      l.map (x -> f x)
    ```
    suffices.

- Fuzion language

  - Added new expression `xyz.type` to get access to a unit type value corresponding to
    type `xyz`. This is planned to work for type parameters allowing features to be defined
    for types that can be used in generic code without to have an instance of that type (e.g.,
    to call a constructor for a value).

  - type parameters can now be declared with the normal parameters, i.e., instead of
    ```
      f<A : xyz, B>(v A, w B) is ...
    ```
    one can write
    ```
      f(A xyz.type, B type, v A, w B) is ...
    ```
    also, calls of the form `f<T1,T2> a b` can now be written `f T1 T2 a b`.

    Type of the form
    ```
      list<map<string, i32>>
    ```
    can now be written as
    ```
      list(map string i32)
    ```
    The old syntax will eventually be removed.

  - match statement now supports matching `*`, which creates a default branch.

  - calls of the form `f a,b,c` are no longer supported, has be be either `f a b
    c` or `f(a,b,c)`.

  - full stop can now be used for empty constructors, e.g.
    ```
      red is
      green is
      blue is
    ```
    can now be written as
    ```
      red.
      green.
      blue.
    ```
    or even
    ```
      red, green, blue.
    ```
  - syntax sugar for choice: a declaration of a choice had the form
    ```
      red is
      green is
      blue is
      point(x,y i32) is

      colorOrPoint : choice<red, green, blue, point> is
    ```
    Now, this can be done using `of` as follows
    ```
      colorOrPoint : choice of
        red is
        green is
        blue is
        point(x,y i32) is
    ```
    or even
    ```
      colorOrPoint : choice of
        red, green, blue.
        point(x,y i32).
    ```

- stdlib

  - result of `list T h t`is now `list<T>` which is convenient in declaring lazy
    lists as in
    ```
      ones => list 1 ()->ones
    ```
    for an endless list of `1`s.

  - added `numericSequence` and `floatSequence` with operations like `average`,
    `std_dev`, etc.

  - added `float.round`

  - added `ctrie` data structure

  - added `Type` as parent feature for types.

  - added `list.flatMap` and `Sequence.flatMapSequence`

  - added `Sequence.first` with default value argument, `Sequence.nth` to get
    n-th element. Sequence is not used more often for iteration, replaces
    'stream'.

  - added `string.trimStart` and `string.trimEnd`

  - default random handler can now be seeded with an u64 given in env var
    `FUZION_RANDOM_SEED`.

  - added effects

    - `panic`
    - `exit`
    - `envi.vars` to access environment variables.
    - `envi.args` to access command line arguments.
    - `concur.thread` to spawn threads

  - joined floats/bitsets/codepoints/matrices.fz into
    float/bitset/codepoint/matrix.fz. These will eventually be defined in
    float/bitset/codepoint/matrix.type.

  - move `sys` to `fuzion/sys`, this is the location for low-level intrinsics

  - removed InitArray, no longer needed since we have array syntax sugar
    `[1,2,3]`.

  - rename `searchableList` as `searchableSequence` since it is a `Sequence`.

  - added `time.nano` for short delays and high-res timer, `time.duration` for
    time spans

- fum/fuir:

  - generics are no longer part of the Fuzion module file or the Fuzion
    intermediate representation. Instead, these are argument features.


## 2022-04-01: V0.071

- Social media

  - new Twitter channel @FuzionLang, please follow

- Fuzion language

  - added support for effects: New expression '<type>.env' permits access to
    current instance of an effect

  - unicode punctuations are now allowed as codepoints within operators

- fz

   - 'fz -effects main.fz' now performs static analysis on 'main.fz' and prints
     effects required by this feature.

- stdlib

  - new effects: io.out, io.err, random, time.nano, state<T>, envir.args, try/raise

  - f32/f63 now offer trigonometric functions, exp, log, and squareRoot, constants ℇ, π

  - intersection operator added to psSet: infix ⋂

- C backend

  - support for f32 and f64

- fuzion-lang.dev

- tests

  - tests on windows now automated using github

## 2022-02-25: V0.070

- fzjava
  - fixed name conflicts between Java's `String.isBlank`/`String.split` and
    Fuzion's `string.isBlank`/`String.split`.

## 2022-02-25: V0.069

- fuzion-lang.dev
  - design
    - added example for [Automatic Monadic
      Lifting](https://fuzion-lang.dev/design/monadic_lifting) using Knuth's
      man-or-boy example and a new feature _handles_ that provides a state monad
      similar to Haskell's _IORef_.

- C backend
  - heap allocate value instances that are kept alive through inner instances,
    fixes segfault in #168.

- IR
  - added new category of clazz: dynamic outer ref. A value clazz _c_ is a
    dynamic outer ref if it is referenced from inner clazzes _i_ and there
    exists heirs of _c_ that may redefine features such that the accesses from
    _i_ require dynamic binding.

- Fuzion language
  - added keyword *intrinsic_constructor* to mark intrinsics that create an
    instance of their result type.  This simplifies static analysis for
    frequence cases such as *fuzion.java.getField0*.
  - inline arrays now may end with a comma as in
    ```
      a := [ 1,
             2,
             3,
           ]
    ```

- stdlib
  - added *Sequence.reduce*, *string.split*, *string.isBlank*, *handles*, ...

- fuzion.ebnf
  - several fixes, cleanup

- fz, fzjava
  - minor fixes in scripts for issues reported by _shellcheck_.

## 2022-02-04: V0.068

- fzjava

  - fixed generated code to use 'Sequence' instead of 'List', which was renamed
    a while ago.

- stdlib

  - lib: Minor improvements used in tutoral at fuzion-lang.dev:

    - added 'list head tail' as a counterpart to Haskell's 'head : tail' for
      list creation.

    - added u128.name

    - added strings.fromCodepoints

## 2022-01-30: V0.067

- front end

  - speed up by

    - caching results for AbstractType.actualType

    - replaced string comparison in FeatureName.compareTo by int comparison.

    - removed usage of strings in AbstractFeature.isOuterRef

    - removed feature names for internally generated features from .fum file

    - caching for LibraryFeature.innerFeatures()

    - disabled check()s unless env var CHECKS=true

    - removed outer ref field from choice features and from any feature whose
      outer feature is the universe.

- stdlib

  - added 'elementOf'/'infix ∈' to 'string'

  - added 'add' to 'Set'

  - added explanation of the origin of the term 'yak' to the same named feature.


## 2022-01-24: V0.066

- front end:

  - speeup up by

    - creating source positions for code loaded from .fum file on demand only.

    - caching of code, result, outerRef withn LibraryFeature

    - using StatementVisitor instead of FeatureVisitor. The former does not
      support code modification, so it might be faster.


## 2022-01-21: V0.065

- fuzion-lang.dev

    - added .fum file documentation to https://fuzion-lang.dev/design/fum_file

    - added browsable fuzion API documentation to https://fuzion-lang.dev/docs/index

- parser

    - single-line operator expressions may now continue after a multi-line
      klammer-expression, e.g.,
      ```
        a := 1 * (2
                  + 3
                  + 4) - 5

      ```
      is allowed now.  The same holds for expressions using [] or {}:
      ```
        b := 2 * { say "hi"
                   + 4
                 } - 1

        c := 3 * [ "A",
                   "B",
                   "C" ].fold strings.concat
                        .byteLength - 5
      ```
    - improve handling of '.'-chained calls over several lines. It is now
      possible to write
      ```
        s := myseq.drop n
                  .take m
                  .asString
      ```
      before, '.take' was applied to 'n' instead of the result of '.drop'.  Now,
      a space separated argument list is terminated by a new line followed by
      '.', while a call in a line is permitted to continue in the next line if
      followed by an indented '.'.

- front end

  - suppoert for qualified features, even to add inner features to library code
    such as
    ```
      i32.factorial => if val <= 1 then 1 else val * (val-1).factorial
    ```
    or even

    ```
      i32.postfix ! => if val <= 1 then 1 else val * (val-1).factorial
    ```
    such that you can write
    ```
      say 6.factorial
      say 4!
    ```
  - parser now uses the same rule when parsing a .fz file read form a source
    directory as when reading stdin or an explicit file given on the command
    line.  Before, .fz files read from a source directory were not permitted to
    contain more than one feature declaration.

  - cleanup in module creation and loading code

- stdlib

  - renamed 'List' as 'Sequence'. A 'Sequence' is a finite or infinite data
    structure containing elements of one type that can be iterated as a
    (Lisp-style) 'list' or a (Java-style) 'stream'

  - cleanup: joined related source files, e.g. i32.fz now contains the code of
    i32s.fz and i32_.fz as well.  This is possible since .fz files read from a
    source directory now may contain several feature declarations.

## 2022-01-06: V0.064

- front end:

  - several optimizations like caching to improve performance of using module
    file.

  - save source code positions of feature declarations and statements to module
    file for more detailed error messages.


## 2022-01-03: V0.063

- front end:

  - support precompilation of base library code into a module file
    build/modules/base.fum.

    This file is mapped into memory instead of parsing the source files in
    build/lib resulting in faster startup up.

    For the front end and the interpreter, the AST is re-created from this file
    as needed.

    Currently, the AST is also re-created to produce the FUIR representation
    used by the C backend, but this should eventually not be needed any longer.

## 2021-10-29: V0.062

- fuzion-lang.dev website

  - added more idioms, e.g. #182 (quine), 242..258, etc.

- IR

  - major code restructuring and cleanup moving logic away from the AST and into
    the Front End, Middle End, Application IR, etc.  The goal is to be able to
    create "module IR" files as a faster replacement of source code.


  - added very basic data-flow-analysis phase that finds some very obvious cases
    of uses of uninitialized fields.


## 2021-10-15: V0.061

- fuzion-lang.dev website

  - added many idioms, updated syntax of existing idioms

- library

  - added matrix, matrices

  - lot's of minor improvements

- Frontend

  - improve type inference for type parameters

- general

  - switched to OpenJDK 17


## 2021-09-30: V0.060

- fuzion-lang.dev website

  - new design for code input / execution output text areas

  - tuturial section on string literals.

  - examples and idioms updated for new syntax of lambda expressions and
    function types.

  - new idioms 141, 142, 150

- fuzion.ebnf

  - grammar is now checked using antlr, bugs and inconsitencies have been fixed.

- Frontend/Parser:

  - new syntax for function types: '(i32, string) -> bool' or 'u8 -> string'

  - new syntax and type inference for lambdas: 'x -> 2*x', 'x,y -> x+y',
    '(a,b,c) -> a < b < c', '() -> 42', etc.

  - last expression in a constructor now must result in type 'unit'. This is to find
    problems earlier when constructors are used instead of routines.

- IR

  - removed callbacks from IR to the backend to have a clearer separation of phases.

- FUIR

  - Moved code to set outer references from backend to FUIR to simplify backend.

- C backend

  - fix wrap-around overflow handling in created code for signed integer types.

  - several code generation fixes

  - code cleanup

- tests

  - analyzed root cause of failing tests, added issues to github for tests that
    could not be fixed, added mechanism to skip these tests, such that a normal
    build does not report any errors for known problems.

- fz

  - limit number of errors and warnings printed, new options '-XmaxErrors=<n>'
    and '-XmaxWarnings=<n>'.

  - many bug fixes


## 2021-09-02: V0.059

- FrontEnd / lib / backends

  - support for types i8, i16, u8, u16, f32, f64.

- fuzion-lang.dev website

  - added sections on integer and float constants to tutorial.

- Java interface:

  - added java.desktop module that contains java.awt graphics

  - support for primitive types and Java arrays

- examples

  - added example javaGraphics that uses java.desktop module to draw into a
    window.


## 2021-08-23: V0.058

- FrontEnd

  - fix running examples such as tutorial/overflow* on fuzion-lang.dev (see git commit
    for details).

## 2021-08-20: V0.057

- Java interface

  - new tool fzjava to create Fuzion wrappers for Java code

  - 'fz -modules=java.base <x>' will run '<x>' with JDK's java.base module
    accessible, e.g., via

      serversocket := Java.java.net.ServerSocket.new 8080

- examples

  - added example 'callingJava' to show how to call Java code from Fuzion and
    'webserver' that shows how to use Java APIs to write a minimalistic web
    server in Fuzion.

- fz

  - now supports modules via '-modules' options. Currently, these are just
    additional source code directories in 'build/modules', but these should be
    packed into fuzion module files eventualls.

- Parser

  - precedence of fraction bar ⁄ is higher than basic arithmetic operations.

- library

  - support for types i8, i16, u8, u16

  - string.utf8 now returns an List<u8>.

- fuzion-lang.dev website

  - now supportes ACE editor for source code

  - use server-side events to fix many issues in the communication.

  - several new idioms implemented and old ones improved

- General

  - Fuzion logo is now generated


## 2021-07-30: V0.056

- Parser

  - Changed parsing of expressions using []: 'a[i]' is an call to 'a.index
    [](i)' as before, but 'a [i]' now is a call to 'a' with an array as
    argument, i.e., 'a([i])'.  This avoids extra parentheses when passing an
    array as an argument.

- tests

  - changed syntax in tests to not use { }, ( ), commas, semicolons, when
    possible

- fz

  - now accepts '.fuzion' as extension for source file given as command line
    argument: 'fz ./helloworld.fuzion' now works instead of printing a confusing
    message.

  - error messages now enclose code snippets in '' and highlight feature names, types
    and expressions in different colors.

## 2021-07-21: V0.055

- C backend

  - several bugs fixed: bugs related to (normlized) outer refs, calls to
    not-instantiated clazzes, code after a statement that produced a void
    result, array of unit type, etc.

- IR

  - several fixes in normlization of outer refs, i.e., in the way clazzes
    instantiated with different outer contexts are distinguished.  This fixed
    several tests that failed when using the C backend.


## 2021-07-08: V0.054

- fuzion-lang.dev

  - changed path to load 'main.js' to be absolute '/main.js'.  Before, main.js
    could not be loaded if the first page access was in a sub directory such as
    'fuzion-lang.dev/tutorial/index' and anything requiring java script, like
    executing examples, did not work.

- Front End

  - fixed issue#8: returing 'false' as 'option<V>' n logner allowed


## 2021-07-06: V0.053

- Parser

  - More haskell-style call syntax
    ```
      f(a,b)   calls f with arguments 'a', 'b'
      f (a,b)  calls f with argument '(a,b)', a tuple

      f fun () => x    no longer works
      f (fun () => x)  must be used instead
    ```

- Front End

  - Now enforce feature with 'unit' result to result in unit type value.

- lib

  - faction now uses unicode fraction slash '⁄' to create fraction, e.g., '1⁄2'

## 2021-06-30: V0.052

- Parser

  - Haskell-style call syntax with space separated arguments and left binding,
    so the call
    ```
      f a b (g c d)
    ```
    is equivalent to
    ```
      f(a,b,g(c,d))
    ```
    Additionally, infix, prefix and postfix operators now bind stronger than a call
    if there is no white space (or comment) between the operator and its operand,
    and weaker than a call if there is white space.  This means
    ```
      f x+1
    ```
    is equivalent to
    ```
      f(x+1)
    ```
    while
    ```
      f x + 1
    ```
    is equivalent to
    ```
      f(x)+1
    ```
    This makes the semantics depend on white space in a way that might be
    surprising.  This change is therefore considered for experiments only, if this
    turns out to be too confusing, the grammar should go back to not make operator
    precedence depend on spaces around operators.

- lib, tests

  - libs and tests were changed to support new call syntax, in particular the
    left binding of call and the weaker precedence of operators that are
    separated by white space.


## 2021-06-29: V0.051

- fuzion-lang.dev

  - design: added fuzion-lang.dev/design/calls.html for thoughts on Fuzion's call
    syntax.

  - added idioms 134, 135, 136

- front end

  - allow chained boolean expressions with >3 terms, e.g., 'a < b < c < d'

  - support for '? |' instead of match, allow '|' as case separator
    ```
      a ? x X => x.foo
        | y Y => y.bar
    ```
    as short-hand for
    ```
     match a
       x X => x.foo
       y Y => y.bar
    ```
    also allowed is
    ```
     match a
       | x X => x.foo
       | y Y => y.bar
    ```
  - A loop's 'until' clause now supports 'then' to separate the condition from
    the code executed for successful loop termination.  The code is now
    summetric to 'if cond then'.

- lib

  - 'o??' and 'o!!' are new postfix operator synonymes for 'exists' and 'isNil'
    in 'option', 'numOption' and 'outcome'.

- fz

  - several bug fixes in FE, IR, FUIR; interpreter and C backend.


## 2021-06-23: V0.050

- Fuzion front-end/middle-end

  - Fixed major performance bottleneck, performance improved by about 40-60%.

- fz tool

  - new option -XjavaProf (for profiling fz's own Java code) and -X/--xhelp).

- fuzion-lang.dev

  - added idiom 76

## 2021-06-22: V0.049

- lib: added

  - bitsets
  - sum
  - List.tails, takeWhile, dropWhile
  - searchableList.countMatches, countMatchesOverlapping
  - string.count, contains

- FUIR

  - suppress code generation for pre- and post-conditions that are constant and
    'true'.  Reduces binary size when using C backend on HelloWorld with
    '-debug=0 -safety=off' by 3.5%.

- fuzion-lang.dev

  - added idioms 39, 40, 49, 68, 76, 82, 116

## 2021-06-18: V0.048

- IR: Added mechanism to 'normalize' clazzes to reduce the explosion in the total
  number of clazzes generated: Clazzes that only differ in their outer clazzes
  are now fused if the outer clazzes are references.

- lib

  - replaced 'streamable' by 'List', i.e., the preferred way to iterate is by
    using lisp-syle lists consisting of a head and a cons-cell.

  - added u128 for unsigned 128-bit integers

- fuzion-lang.dev

  - added idioms 62, 63

## 2021-05-31: V0.047

- tools:

  - fz tool now prints execution time for phases prep, front end, middle end,
    ir, and back end.

- front end:

  - improved performance by caching result of Type.outer()

- lib

  - fix exponential performance when printing string built from s+(s+(s+(...)))


## 2021-05-30: V0.046

- lib

  - string: added startsWith, endsWith


- fuzion-lang.dev

  - added idioms 84, 85, 86, 87, 88, 89, 90, 93, 96, 97, 100, 108, 110, 112,
    113, 114, 117, 118, 119, 122, 124, 127, 227, 231


## 2021-05-24: V0.045

- lib:

  - integer.bin/cot/hex to create binary, octal and     hex strings

  - fix quadratic runtime of printing string consisting of many sub-strings
    concatenated using 'infix +'.

  - i32/u32/i64/u64: added ones_count (population count)

  - stream/streamable: added asList, planning to work more with (lazy,
    immutable) lists than with streams that are inherently mutable. Also added
    'take', 'drop', 'infix ++' and 'asArray' to stream/streamable.

  - added feature 'some' as standard wrapper. This can be used, e.g., to make an
    'option<some<nil>>' without getting complaints about overlapping values, the
    possible values of typ 'option<some<nil>>' are 'nil' and 'some nil'


- fuzion-lang.dev

  - added idioms # 38, 44, 46, 47, 48, 53, 54, 55, 57

## 2021-05-24: V0.044

- lib

  - new unit types strings, i32s, i64s, u32s, u64s that contain monoids and
    other features related to string, i32, i64, u32, u64 but not requiring an
    instance.  Also added new routines i32, i64, u32, u64, all without arguments
    to distinguish them from the integer types that have one argument, that
    result in the corresponding unit type i32s, i64s, u32s, and u64s,
    respectively.

    Now, we can write, e.g., '[1,2,3].fold i32.sum' to sum the elements of an
    array.

  - add new feature 'Set' as abstract parent for sets, and operations such as
    'infix ∈', so we can write '2 ∈ [1,2,3]' or '7 ∉ 10..300' (both result in
    bool value true).

- fuzion-lang.dev

  - added idioms # 51, 52

## 2021-05-21: V0.043

- lib

  - added codepoint.fz and string.codepoints/codepointsArray to create stream
    and array of codepoints from a string.

  - added 'infix **' for integer exponentiation.

- parser/lexer:

  - support for binary, octal, and hex numbers using prefix 0b, 0o, and 0x.
    Support for grouping of digits using '_' as in 'ichiOku := 1_0000_0000',
    groups must be equal in size and at least two digits long.

- fuzion-lang.dev

  - added idioms # 32, 41

  - added design page on integer literals / constants.

## 2021-05-20: V0.042

- lib

  - add string.parseI32*, parseU32*, etc to parse integers from decimal,
    binary, octal or hex numbers.  Very nice generic implementation for
    all integers in one feature!

  - added error.fz for an error condition

  - added outcome.fz similar to Rust's result: a choice between a value and an
    error.

- fuzion-lang.dev

  - added idioms # 18, 22, 26, 27, 34, 42

## 2021-05-19: V0.041

- fuzion-lang.dev

  - added idioms # 8, 9, 13, 16, 19

- lib

  - added array.reverse, list.reverse

  - added fuzion.std.panic

  - added map as parent of hashMap and orderedMap, defines keys, values, items

  - added Option/array/list.asString

- front end

  - fix#9: AdrToValue.type() was not intern()ed.

## 2021-05-18: V0.040

-lib

  - added features partiallyOrdered and ordered and implemented ordered by
    numeric and string.

  - added feature hasHash and implemented it for numeric and string.

  - added hashmap and orderedMap.

  - added psMap and psSet to have well behaved persistent sets and maps for
    ordered values.

  - added highestOneBit and trailingZeros to i32/i64/u32/u64

- C backend

  - add support for indentation in generated code.

## 2021-04-20: V0.039

- C backend

  - now works with array initialization using '[a, b, c]'.

- fuzion-lang.dev

  - updated examples to use array initialization instead of InitArray.fz

  - new design of main page using CSS magic.


## 2021-04-20: V0.038

- Fuzion language

  - Support for array initialization using '[a, b, c]' or '[e; f; g]' where the
    array elements are expressions.  The array type is either inferred from the
    union of the types of the element expressions or propagated back from the
    target the array is assigned to.  Boxing and Tagging for array elements is
    automatic.  Support in FUIR/C backend missing, only interpreter for now.


## 2021-04-14: V0.037

- Fuzion language

  - match statement:

   - now infers actual generics if this does not lead to ambiguity. So one can,
     e.g., match a 'list<string>' against 'Cons' instead of 'Cons<string,
     list<string>>'.

   - Errors are created if a case does not match anything, matches several types
     or if some cases are missing

- lib

  - bitset.fz provides persistent bitsets

  - array.fz is a persistent array now, i.e., array.put(index, value) creates a
    new array.

  - stream.map, stream.fold added

  - numeric.min, numeric.max added


## 2021-04-08: V0.036

- lib

  - array.fz is immutable now, new feature marray.fz provides mutable arrays.

- Fuzion Language:

  - Field initialization 'f type := value', instance decomposition
    '(a i32, b bool) := obj' and array writes 'a[i] := v' now use ':='
    instead of '='.

## 2021-04-07: V0.035

- Fuzion Language:

  - Assignments are no longer accepted in the format 'a = b'. Instead, an assignment
    must be of the form

      set a := b

    to make sure that mutations are clearly visible in the source code.

## 2021-04-06: V0.034

- Fuzion Language

  - identifiers now may consist of non-ASCII code points of Unicode categories
    LC, Ll, Lm, Lo, Lt, Lu, Nd, Nl, and No, while the first character may not be
    of one of the numeric types No, Nl, No.

  - operartors now may consist of non-ASCII code points of Unicode categories
    Sc, Sk, Sm, So.

- tools:

  - fz tool now supports command line options -verbose, -debug, -safety,
    -unsafeIntrinsics

  - 'fz' now can be used giving a source file name as in 'fz Hello.fz'.

  - 'fz -latex' creates a LaTeX style for Fuzion source code.

## 2021-03-29: V0.033

- lib

  - added list.fold, list.take, added predefined monoids to numeric ands string.

  - renamed FALSE.fz and TRUE.fz to avoid name clashes on case-insensitive file
    systems.

## 2021-03-25: V0.032

- Fuzion Language:

  - Constant string now allow embedded identifiers using $id and embedded
    expressions using {expr}.

  - Constant strings now support escape sequences:
    | Esc sequence |  result | unicode |
    |------|-------|---------|
    | '\b' |  BS   | 0x08    |
    | '\t' |  HT   | 0x09    |
    | '\n' |  LF   | 0x0a    |
    | '\f' |  FF   | 0x0c    |
    | '\r' |  CR   | 0x0d    |
    | '\"' |  "    | 0x22    |
    | '$'  |  $    | 0x24    |
    | '\'' |  '    | 0x27    |
    | '\\' |  \    | 0x5c    |
    | '\{' |  {    | 0x7b    |
    | '\}' |  }    | 0x7d    |

## 2021-03-19: V0.031

- C backend

  - function to compile one whole feature is pure now

- tests

  - Makefiles now run tests using c backend in addition to interpreter

## 2021-03-18: V0.030

- C backend

  - support for postconditions

## 2021-03-17: V0.029

- C backend

  - support for choice types of unit types and one reference type, (e.g.,
    Option<T>). These do not require a tag since the unit types can be encoded
    as a special pointer value.

  - support for preconditions

## 2021-03-16: V0.028

- AST/IR/BE:

  - Tagging a value when assigned to a choice type is now done explicitly by an
    IR command Tag.  This no longer happens implicitly as part of an assignment,
    simplifying the IR and backends.

- C backend

  - fix handling of boxed unit

- Parser

  - a field with formal generics or formal arguments is now disallowed as early
    as during the parsing step.

## 2021-03-15: V0.027

- IR

  - now distinguishes instantiated from called clazzes resulting in less code
    generated by C backend

- C backend

  - support for arrays of arbitrary types

  - support for fields declared in universe

## 2021-03-11: V0.026

- C backend:

  - loads of clean-up and simplifications

  - bug fixes for dynamic calls and write accesses to outer fields

## 2021-03-10: V0.025

- C backend:

  - implemented match for non-ref values (i.e, values that distinguished by tag
    and not by a type check).

- Benchmarks:

  - added c_man_or_boy, a benchmark that compiles a the man_or_boy example to C

## 2021-03-09: V0.024

- Desgn:

  - Added section "Automatic global data" in the file
    design/monadic_lifting.html to propose monadic parameters together with
    automatic monadic lifting of callers of features with these monadic
    parameters as a solution for global state (and much more).

- C backend:

  - Choice types make progress: C structs defined, assignment to choice type is
    implemented, match statement is still missing.

  - fixed dynamic calls that did not work to call inherited feature.

## 2021-03-05: V0.023

- Design

  - added 'Feature Kinds' section with very detailed comparison of the
    capabilities of constructors, routines, fields, intrinsics, abstracts and
    choices.  Includes example code for uses of each feature kind in different
    scenarios.

- C backend:

  - several bug fixes and improvements.

## 2021-03-02: V0.022

- C backend

  - Compiles and runs HelloWorld without any warnings during Fuzion to C
    compilation and during C to machine code compilation.

  - added intrinsics for i32, i64, u32, u64.

- IR/FUIR/C backend:

  - code cleanup


## 2021-02-24: V0.021

- IR:

  - no longer creates 'impossible' clazzes such as integer<i32>.infix + when there is no
    instance of integer<i32> created anywhere. So C backend does not need to filter these
    out.

  - some normalization of clazzes to reduce total number of clazzes and amount of code.

- lib:

  - monad.fz added for experiments.

## 2021-02-22: V0.020

- lib:

  - choice.fz now comes with detailed explanation of sum types

  - Tuple.fz now comes with a detailed explanation of product types.

  - new list.fz and Cons.fz provide abstract list framework

- ir:

  - removed several ways that clazzes that cannot exists at runtime such as
    could be created, e.g., 'integer<i32>.ref decimalStr', which should be
    'i32.ref deciamStr'.

## 2021-02-18: V0.019

- lib:

  - added unit.fz as a standard unit type and changed void.fz into a 'real' void
    type with no instances.

  - added monoid.fz and quantors.fz

  - added monadic 'bind' operators to Option and numOption, re-implemented numOption
    using monads.

- front end:

  - added boxing of values of generic type and for implicit assignments to function result
    field.

  - internal cleanup with respect to types, removed internally used type t_ANY, replaced it
    by void.

## 2021-02-12: V0.018

- C backend:

  - The C backend successfully compiles a "HelloWorld" example now using the
    call

      fz -c HelloWorld

    which generates a HelloWorld.c file and then invokes clang -O3 to create a
    HelloWorld binary.

Benchmarks:

  - added c_hello, a benchmark that compiles a HelloWorld example to C


## 2021-02-07: V0.017

- Frotend:

  - Inspired by FOSDEM talks on Raku, I have decided to experiment with making
    the parentheses on calls optional. This means that instead of

      f(a, g(h(-b, c + d)))

    you can now write

      f a, g h -b, c + d

    To avoid ambiguity, a new keyword 'then' separates the condition in a
    single-line 'if'-statement from the following block and uninitialized fields
    can no longer be declared as

       f type

    but usng

       f type = ?

    An operator '<op>' in an expression like 'x <op> y' is now parsed as a
    prefix operator 'x(<op>y)' if there is white space or a comment between 'x'
    and '<op>' and no white space nor a comment between '<op>' and 'y', i.e,
    'a-b', 'a- b' and 'a - b' are all calling an 'infix -' on 'a' with argument
    'b', while 'a -b' calls 'prefix -' on 'b' and passes the result as first
    argument to 'a'.

- stdlib: new features

    'say' as shorthand for 'stdout.println'

    'integer.infix %%' to check if an integer is divisibly by another integer

- Tools

  - added simple syntax highlighting tool via 'fz -pretty'

- FUIR and C backend

  - lots of minor improvements


## 2021-02-01: V0.016

- Frontend:

  - all loops are now implemented using tail recursive features.  IR and backend
    support for loops has been removed.

  - fixed several problems with field visibility and masking in nested scopes.

- Interpreter

  - stack trace printing now compresses simple recursion.

- C backend

  - can compile simple tests including loops now.  Main missing features is
    references and dynamic binding

  - made code more readable by using identifiers closer to the original Fuzion
    feature names.

- General

  - error printing now uses ANSI escapes to produce colourful output.  Can be
    disabled via env var FUZION_DISABLE_ANSI_ESCAPES=true.

- Design

  - added page on static vs. dynamic typing


## 2021-01-12: V0.015

- Fuzion IR

  - extracted dynamic binding related code from Clazz.java to
    DynamicBinging.java.

- fuzion-lang.dev

  - Removed access restrictions, made website and tutorial accessible without
    login.

  - Replaced Fusion by Fuzion at most locations.

- Design

  - Updated module dependencies with explicit backends, added dependency from
    optimizer to interpreter backend since the optimizer will need an engine to
    evaluate compile time constants and the interpreter should do a perfect job
    here.

- Fuzion Front End

  - Removed Loop from AST, a loop is now directly replaced by a tail recurive
    call.

  - Source file name extension is .fz now.

  - one line comments introduced with '#' can start in any column now.  Replaced
    '//' by '#' in the standard lib and examples.

- C backend

  - Compiles and runs first micro-HelloWorld, still many limitations.

      hw is
        fusion.std.out.write(72)
        fusion.std.out.write(87)
        fusion.std.out.write(10)

- Fusion tools

  - added proper command line handling to fusion command line tool, in
    particular -interpreter, -c, -llvm, etc. to chose backend.

## 2020-11-22: V0.014

- Fusion Middle End

  - improved smart linking: Do not include features that could be called
    dynamically but there is no corresponding dynamic call.

- Fusion Front End

  - replaced boolean prefix !, infix &&, infix || and infix : by corresponding
    if-statements to support lazy evaluation.

  - implemented simple constant propagation for bool constants.

  - Expressions of the form 'a <op1> b <op2> c' where 'a <op1> b' yields a bool
    result and '<op2>' is not defined as an infix operator on bool are now
    automatically converted to '(a <op1> { tmp := b; tmp }) && (tmp <op2> c'.
    This means that expressions such as '0 <= index < length' are now possible.

  - Renamed bool operator '==' as '<=>' to avoid confusion when comparing 3 bool
    variables using 'a == b == c' (which holds, e.g., for 'true == false ==
    false').  Maybe the front end should require parentheses as in '(a <=> b)
    <=> c' or '(a == b) == c' when 'a' is boolean to avoid confusion.  How is
    this done in other languages?

- Fusion tools

  - changed fusion.verbose property from boolean to integer, 0 is non-verbose,
    levels 1..10 show different levels of detail.

  - new properties fusion.debugLevel=<int> and fusion.safety=<bool> can be used
    to set the default values for debugLevel and safety (to enable or disable
    pre- and postcondition checks).

- tests

  - added test case redef_args to redefine argument fields.  Not working yet.

## 2020-11-16: V0.013

- Fusion Interpreter: No longer uses virtual call for a dynamic feature called
  on a value instance.

- middle end: Split up finding runtime clazzes and building virtual call tables
  into two phases.  No longer builds virtual call tables for value clazzes.

- Design

  - added page "Matches in Expressions" describing possible syntax of
    expressions using '?' to match the type of a value of choice type.

- removed "throws" keyword and its uses in feature declarations and
  pre/postconditions.

## 2020-11-13: V0.012

- Fusion Interpreter

  - fixed boxing of choice types (only the tag was copied, so values remained
    uninitialized).

- standard lib

  - added support for 2- and 3-dimensional arrays through features

          array<T>>(l1, l2,     fun (i32, i32, i32) T)
          array<T>>(l1, l2, l3, fun (i32, i32, i32) T)

    these inherit from the 1-dimensional array feature, so it maps the
    multi-dimensional array to a flat unidimensional array.

  - added feature numOption<T> as a pseudo-numeric result type of numeric's +?,
    -? and *? operations to allow expressions such as 'a *? b +? c'

  - moved code to create interval from i32/i64/u32/u64 into separate generic
    feature hasInterval to avoid code duplication.

- Fusion middle end: Major cleanup and some bugfixes in handling of outer
  features that showed up when writing hasInterval.

## 2020-11-10: V0.011

- website

  - added tutorial pages on integer type, overflows, fractions.

- Fusion Interpreter

  - now catches StackOverflowError and shows fusion stack trace instead of Java
    stack trace

- Added prefix $ as an alternative to .asString.  Now, you can write '$length +
  "cm"' to create a string like "23cm".

- added overflowing, optional, saturating and wrapping basic operations to
  integer types:

   +  , -  , *   cause runtime error (if debug enabled) on overflow / underflow
   +? , -? , *?  produce an Option that is nil in case of overflow / underflow
   +^ , -^ , *^  are saturating, result is max / min on overflow / underflow
   +° , -° , *°  are wrapping, like in Java

- Nice exercise in generic programming: Added asString feature to
  integer to replace specific implementation in heirs i32, u32, i64 and u64.

- operator precedence: Now, prefix operators always take precedence over infix
  operators, i.e., 'a + + b' is parsed as 'a + (+ b)'.  Precedence is taken into
  account, i.e. 'a * + * b' is parsed as '(a *) + (* b)', while 'a + * + b' is
  parsed as 'a + (* (+ b))'.  Infix operators are parsed left-to-right except for
  operators with right associativity (currently only '^').  Postfix operators
  have precedence over prefix operators with equal precedence value, i.e, '+a-'
  is parsed as '+(a-)'.

  The precedence of multi-character operators such as '|-*-|' is determined by
  the first character, '|' in this case.  Precedence levels currently are

   15: @
   14: ^
   13: !$ (only prefix and postfix)
   12: ~
   11: */%⊛⊗⊘⦸⊝⊚⊙⦾⦿⦸⨸⨁⨂⨷
   10: +-⊕⊖
    9: .
    8: #
    7: $ (only infix)
    6:
    5: <>=⧁⧀⊜⩹⩺⩹⩺⩻⩼⩽⩾⩿⪀⪁⪂⪃⪄⪅⪆⪇⪈⪉⪊⪋⪌⪍⪎⪏⪐⪑⪒⪓⪔⪕⪖⪗⪘⪙⪚⪛⪜⪝⪞⪟⪠⪡⪢⪤⪥⪦⪧⪨⪩⪪⪫⪬⪭⪮⪯⪰⪱⪲⪴⪵⪶⪷⪸⪹⪺⪻⪼⫷⫸⫹⫺
       ! (only infix)
    4: &
    3: |⦶⦷
    2: ∀
    1: ∃
    0: all other operators
   -1: :

  this list is a little arbitrary, is there some mathematical precedence
  definition related to the mathematical unicode chars?

- added new keyword "redef" as alternative to "redefine" (will eventually dump
  'redefine'.

- Fusion std library: Added abstract features numeric and integer as parents for
  integer types. For integer types i32, u32, i64, u64 added preconditions to
  catch overflows.  Added fraction type: 'a /-/ b' for a, b of any (but the
  same) integer type defines a fraction that can be used with all the standard
  operations defined for numeric types.

- Enabled support for generic constraints. A generic feature declared as "name<T
  : c>(args ..)" puts the constraint c on the generic argument T. NYI: Still
  need to check that the actual generic arg is assignable to the constraint.

- '°' is now a legal character in an operator. New operators +°, -° and *°
  implement wrap-around operations, while +, - and * have preconditions that
  ensure there is no overflow.

- Created Slack channel #fusion-language:
  https://join.slack.com/t/fusion-zentrale/shared_invite/zt-j9ab7feo-3fl2oZLBfnKdyfOaDgvnjg

- Added types i64, u32, u64 and numeric, made i32, i64, u32 and u64 inherit
  from numeric.

- Removed "throws" clause from the syntax of feature and function declaration, I
  do not intend to support exception: An exception must either be part of the
  function result, so it should go there, or it is a fatal error that kills (at
  least) the current thread.

- Fusion Interpreter

  - added specialization for reading fields in value type to improve performance

## 2020-11-06: V0.010

- benchmarks: Added first simple performance benchmark that runs the man_or_boy
  example repeatedly.

- replaced abstract and native modifiers by specific routine bodies "is
  abstract" and "is intrinsic". This avoids the problem of marking a field
  abstract or intrinsic.

- website

  - update tools module design (dependency graph)

- Fusion Interpreter

  - split code into two packages dev.flang.ast and dev.flang.be.interpreter.
    Removed dependencies from ast to be.interpreter, the dependencies from
    be.interpreter to ast will have to be changed to refer to fuir (once it
    exists.

## 2020-11-03: V0.009

- website

  - added Fusion grammer fusion.ebnf linked from the main page.

  - Fusion Tutorial

    - added sections on Design by Contract, developing with pre- and
      postconditions, predefined pre- and postcondition qualifiers.

- pre- and postconditions for single features are supported now.  A failing
  condition causes termination of the Fusion interpreter with an error message
  and a (Fusion!) call stack.

- Condition qualifiers have been added to control execution of pre- and
  postconditions: safety, debug, debug(i32), pedantic, analysis.

- new boolean operator "infix :", which implements "implies" ((a : b) <==> !a ||
  b). ':' has lowest precedence now, such that preconditions of the form

    pre
      safety: index < length

  are possible now.

- new keywords "pre", "post", and "inv" as alternatives for "require", "ensure",
  and "invariant". The latter might get retired soon.

- i32.infix / and i32.infix % now check that other != 0 via a pre-condition
  qualified with safety.

- new fields i32.max and i32.min to get the max/min value an i32 can hold.  A
  bit strange: Since i32 is a value type, we need an i32 to get the constant
  maximum value, e.g., 0.max == i32(-123).max == array.length.max.

- inline functions (lambdas declared with "fun (args) ...") now use the same
  syntax for their implementation as routine definititions: "is" block, "=>"
  block or a block enclosed in { }.

## 2020-11-01: V0.008

- website

  - Added "How to Support Fusion" page.

  - Added Rosetta Code Fibonacci example, updated other Rosetta Code examples to
    use indentation based blocks.

  - updated the idioms to use indentation instead of { }.

- Fusion language

  - ifstmnt and loop are expr now, not stmnt. This means they can be used
    directly as expressions, it is no longer needed to put them into { } to use
    them as a value.

  - added else and until as block ending tokens such that single line blocks
    that end with else or until no longer need to be enclosed in { }.

- Fusion Front End

  - Removed redundent directory listing in source dirs to improve speed.

## 2020-10-29: V0.007

- scopes for match cases: features declared in match cases are now visible only
  in the match case they were declared in.  In particular, the same variable
  name can be reused for the value we are matching.

- Fusion Tutorial

  - updated the examples in the tutorial to use indentation instead of { }
    whenever possible.  Added a copy of the tutorial with the original examples
    as "Fusion Tutorial using { }", linked from the main page.

- blocks no longer need to be enclosed by { }.  Routine declarations without { }
  require the new keyword 'is' before their code block.

- match statement no longer needs to be enclosed by { }. The right side of a
  case in a match statement is no longer an expr, but a block.  Cases are no
  longer separated by comma, but by semicolons or flat line breaks. This means
  that { } are no longer needed if line breaks and proper indentation is used.
  Single-line match clauses are a bit awkward, though, requiring { } and ';':

    v := match x a => { p; q }; b => { r; s }

  is equivalent to

    v := match x a => p; q
                 b => r; s

  is equivalent to

    v := match x a =>
                   p
                   q
                 b =>
                   r
                   s

- expressions

  - expressions are now restricted to a single line with the following
    exceptions:

    - an expression within parentheses ( ) or crochets [ ] may extend over
      several lines

    - a statement sequence within braces { } used as an expression may extend
      over several lines

    - a dot expression abc.def can have a line break before the dot.

- semicolons

  - semicolons between statements are now required for subsequent statements in
    the same block if they are in the same source line, semicolons are optional
    if statements are separated by a flat line break.

- Fusion design pages

  - Added much more detail to Blocks and Indentation page

  - Added page to Fusion toolchain modules

- Fusion Interpreter

  - improved performance by

    - caching interned types

    - checking and caching which types do not depend on generics,
      such that their actual type is always the same.

    - disabling pre- and postconditions in production (enabled by setting env
      variables PRECONDTIONS=true and POSCONDITIONS=true)

## 2020-10-22: V0.006

- feature declarations and visibility

  - field declarations using := can now mask previously declared fields with the
    same name.

  - features in unqualified accesses are now searched taking the scope
    visibility into account.

- a.b.this expression now produces a proper error message if a.b is not found.

- Fusion Interpreter

  - When processing input from stdin (which is used when clicking "run" on the
    website examplex), the interpreter now parses stmnts and not feature.  If
    input is a single statement that is a feature declaration, that feature will
    be executed as before. Otherwise, these statements will be added to the
    internal feature --universe-- and this will be executed.  This has the
    effect that many small examples no longer need a surrounding feature, e.g.,
    'stdout.println("Hello, World!")' can be executed directly now and Fusion
    scripts with shebang are possible.

  - uninitialized non-ref values are not initialzed with -555555555 to be more
    obvious when used.  DFA to detect possible uninitialized accesses is still
    not supported.

- Fusion design pages

  - Added page on visibility.

  - Extended page on strings with section on text blocks and converted it from
    wiki to html.

  - added sections on coroutines, light-weight threading, SIMD and fuom, the
    Fusion Optimizer.

- Loops

  Slight change in semantics: 'for i := <expr> do .. ' is now interpreted as
  "for i := <expr>, <expr> do .. ' and it is not allowed to assign a value to i
  anywhere else. This allows the following pattern

    for data := read()
    while data >= 0
      {
        process(data)
      }

- comments

  - New type of comments enabled: '#' as first codePoint in a line starts a
    comment that extends to the end of this line.  Now we can run fusion codes
    like scripts by starting the file with shebang, e.g.,

       #!/bin/fusion.sh

       stdout.println("Hello, World!");

    if fusion.sh starts the fusion interpreter.

  - Fixed bug in Lexer that disallowed '/' in operators. Now, operators such as
    infix -/*^-^*/- are allowed, this does not contain a comment.

- Errors

  - In case of compilation errors, suppress certain subsequent errors:

     - wrong type if if/while/until expression if type is t_ERROR,

     - missing result type in field declaration if field name is
       Lexer.ERROR_STRING (indicating a parsing error),

     - expected EOF in case of previous errors

## 2020-10-14: V0.005

- Benchmarks

  - added simple infrastructure for daily run benchmarks, accessible through the
    website.  There is currently no dedicated server to run the benchmarks, so
    there will be jitter, but this should do for now.

- Fusion Interpreter

  - Added checks that the main feature is not a field, is not abstract, not
    native and not generic such that this produces a proper error message
    instead of a Java exception.

- Fusion frontend

  - overloading for features:

    A feature may now be overloaded in the number (not the type!) of its
    arguments.  In particular, there are two features stdout.println now, one
    with no argument and one that expects a ref Object argument.

    This change also added infrastructure that will be needed for Eiffel-style
    renaming of inherited features.

  - initial assignments to fields are no longer allowed unless the field is
    declared within the code of an outer routine.  In particular, x := <expr>
    does not work. This code was never executed anyway and I do not want to
    leave the execution order as a arbitray decision of the implementation.

- Fusion library

  - renamed 'fusion.std.out.printline' as 'println', overloading the existing
    'println(ref Object)'.

  - In 'i32.infix..': Added feature 'lower' and renamed 'other' as 'upper' for
    convenience.  Extended 'infix :' to support negative steps to iterate from
    'lower' down to 'upper' (i.e, it requires 'lower >= upper'), e.g.,
    '10..0 : -1' counts from 10 down to 0.

  - added 'string.infix *' to repeat a string a given number of times.

- Fusion design pages

  - added page on blocks using braces vs. indentation

  - added page on semicolon and how and when it could be made optional

  - changed overloading page from wiki markup to html and added section on
    argument count overloading.

  - changed inheritance page from wiki markup to html and added section on
    multiple inheritance, conflicts during inheritance and possible solutions to
    resolve these conflicts.

  - added page on assignment syntax

  - added page on file name extensions

- webserver

  - improved logging

## 2020-10-05: V0.004

- lexer

  - fixed crash when comment following >>//<< ends with end-of-file instead of
    new line.

- webserver

  - added more examples to idioms 1, 12, 17, 20, 21, changed getRed into just red
    in the tutorial's match examples (Java's getter/setter frenzy apparently
    found its way deep into my unconsciousness).

  - removed user ids for logged in users, they are not needed.

- lib

  - removed Null.fu

## 2020-10-02: V0.003

- webserver

  - added pages to implement the idioms from programming-idioms.org

  - index.html template: Added 3sec timeout to reconnect EventSource in case no
    server response was encountered.  This seamms to work better than the
    previous solution of reconnecting if more than 3 requests were left without
    reply, but I am still unhappy about this manual reconnect.

- loops

  - if a loop that may succeed or fail has a VOID result type is used as an
    expression and type propagation asks it to be of type boolean, then the loop
    will result in true on successful termination (until condition is true) or
    false (otherwise: iteration was exhaustive or while condition is false).

  - index Vars now may be updated in the loop body if their declaration in the
    for-clause does not define a value for the next iteration.

  - added compiler checks that while- and until-conditions are actually boolean,
    so we now get a compile-time error instead of a runtime crash

  - added type propagation: The while- and until-conditions are expected to be
    of type bool.

- if statement

  - added compiler checks that if-conditions are actually boolean, so we now get
    a compile-time error instead of a runtime crash

  - added type propagation: The condition of an if-statement is expected to be
    of type bool.

- compile time errors

  - the front end no longer stops after a phase has shown errors but tries to
    continue until it would execute the application.  This might cause
    subsequent errors or even crashes, but it will help to make the front end
    more robust.

- abstract features

  - added runtime error that checks that with one or several abstract inner
    feature are called unless there are no calls to the abstract feature. No
    fancy data-flow analysis is done yet, so the following code

      f {
       x { abstract y }
       z : x { redefine y { } }
       x;
       r ref x = z;
       r.y;
      }

    does not compile even though the only call to y is in an instance of z.

  - added compile-time errors for the case that a feature with an implementation
    is marked as abstract

## 2020-09-30: V0.002

- renamed intrinsic features for reference comparison as >>infix ===<< and
  >>infix !==<< to avoid type errors when redefined in i32 and bool.

- Added checks that redefined features match the argument and result types of
  the original features. No variance for argument types allowed (so far),
  covariance allowed for ref result type.

- removed types int and String and last uses of int in testsuite, i32/string are
  used instead.


## 2020-09-29: V0.001

- First version to be presented to very few selected users via the fuzion-lang.dev website.

<!--  LocalWords:  Fuzion JDK Makefile fz lt ints ascii nano DFA CFG JVM fzjava
 -->
<!--  LocalWords:  VerifyError backend stackmap backends envir args Boehm
 -->
<!--  LocalWords:  atomics XkeepGeneratedCode
 -->
