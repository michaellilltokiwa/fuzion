package dev.flang.be.diag;

import java.util.TreeSet;

import dev.flang.fuir.FUIR;
import dev.flang.fuir.FUIR.ContractKind;
import dev.flang.fuir.analysis.AbstractInterpreter;
import dev.flang.util.List;
import dev.flang.util.Pair;

public class Diagram
{


  private FUIR _fuir;
  private AbstractInterpreter<Integer, Object> _ai;
  private TreeSet<Integer> _processed = new TreeSet<>();


  class Processor extends AbstractInterpreter.ProcessExpression<Integer, Object>
  {

    @Override
    public Object sequence(List<Object> l)
    {
      return null;
    }

    @Override
    public Integer unitValue()
    {
      return -1;
    }

    @Override
    public Object expressionHeader(int s)
    {
      return new Object();
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
    public Integer arg(int s, int i)
    {
      return -1;
    }

    @Override
    public Pair<Integer, Object> constData(int s, int constCl, byte[] d)
    {
      return new Pair<Integer,Object>(constCl, null);
    }

    @Override
    public Pair<Integer, Object> env(int s, int ecl)
    {
      return new Pair<Integer, Object>(-1, new Object());
    }

    @Override
    public Object contract(int s, ContractKind ck, Integer cc)
    {
      return -1;
    }


    /**
     * Get the target clazz and the called clazz as a Pair of two Integers.
     *
     * @param tvalue the actual value of the target.
     */
    private Pair<Integer, Integer> ttcc(int s, int tt0)
    {
      var ccs = _fuir.accessedClazzes(s);
      var tt = ccs.length < 2 ? -1 : ccs[0];
      var cc = ccs.length < 2 ? -1 : ccs[1];

      if (cc == -1)
        {
          tt = tt0;

          for (var cci = 0; cci < ccs.length && cc==-1; cci += 2)
          {
            if (ccs[cci] == tt)
              {
                cc = ccs[cci+1];
              }
          }
        }

      return cc == -1
        ? new Pair<Integer,Integer>(_fuir.clazzUniverse(), _fuir.clazzUniverse())
        : new Pair<Integer,Integer>(tt, cc);
    }

    @Override
    public Object assignStatic(int s, int tc, int f, int rt, Integer tvalue, Integer val)
    {
      return null;
    }

    @Override
    public Object assign(int s, Integer tvalue, Integer avalue)
    {
      return null;
    }

    @Override
    public Pair<Integer, Object> call(int s, Integer tvalue, List<Integer> args)
    {

      var ttcc = ttcc(s, tvalue);
      var tt = ttcc.v0();
      var cc = ttcc.v1();

      say (_fuir.clazzOriginalName(tt) +  " -> " +  _fuir.clazzOriginalName(cc));



      var result = new Pair<Integer, Object>(cc, new Object());
      switch (_fuir.clazzKind(cc))
        {
        case Routine :
          if (!_processed.contains(cc))
            {
              _processed.add(cc);
              result = _ai.process(cc, false);
            }
          break;
        default:

        };
      return result;
    }

    @Override
    public Pair<Integer, Object> box(int s, Integer v, int vc, int rc)
    {
      return new Pair<>(rc, null);
    }

    @Override
    public Pair<Integer, Object> current(int s)
    {
      return new Pair<>(_fuir.clazzAt(s), null);
    }

    @Override
    public Pair<Integer, Object> match(int s, AbstractInterpreter<Integer, Object> ai, Integer subv)
    {
      for (int cix = 0; cix < _fuir.matchCaseCount(s); cix++)
        {
          ai.process(_fuir.matchCaseCode(s, cix));
        }
      return new Pair<Integer,Object>(-1, null);
    }

    @Override
    public Pair<Integer, Object> tag(int s, Integer value, int newcl, int tagNum)
    {
      return new Pair<Integer,Object>(newcl, null);
    }

    @Override
    public Pair<Integer, Object> outer(int s)
    {
      return new Pair<Integer,Object>(-1, null);
    }

  }
  public Diagram(FUIR fuir)
  {
    this._fuir = fuir;
    this._ai = new AbstractInterpreter<>(fuir, new Processor());
    _processed.add(_fuir.mainClazzId());
    _ai.process(_fuir.mainClazzId(), false);
  }
}
