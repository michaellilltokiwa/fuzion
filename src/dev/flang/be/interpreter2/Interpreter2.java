package dev.flang.be.interpreter2;

import dev.flang.air.Clazz;
import dev.flang.air.Clazzes;
import dev.flang.be.interpreter.DynamicBinding;
import dev.flang.be.interpreter.Value;
import dev.flang.fuir.FUIR;
import dev.flang.fuir.analysis.AbstractInterpreter;
import dev.flang.util.FuzionOptions;

public class Interpreter2
{
  public static AbstractInterpreter<Value, SideEffect> _ai;
  private FUIR fuir;

  public Interpreter2(FuzionOptions options, FUIR fuir)
  {
    this.fuir = fuir;
    _ai = new AbstractInterpreter<Value, SideEffect>(fuir, new Processor(fuir));

    for (var cl : Clazzes.all())
      {
        DynamicBinding db = null;
        for (var e : cl._inner.entrySet())
          {
            if (db == null)
              {
                db = new DynamicBinding(cl);
                cl._dynamicBinding = db;
              }
            if (e.getValue() instanceof Clazz innerClazz)
              {
                db.add(null, e.getKey(), innerClazz, cl);
              }
          }
      }

  }

  public void run()
  {
    _ai.process(fuir.mainClazzId(), false);
  }

}
