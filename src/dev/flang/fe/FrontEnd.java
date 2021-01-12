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
 * Tokiwa GmbH, Berlin
 *
 * Source of class FrontEnd
 *
 *---------------------------------------------------------------------*/

package dev.flang.fe;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.TreeMap;

import dev.flang.ast.Block;
import dev.flang.ast.FeErrors;
import dev.flang.ast.Feature;
import dev.flang.ast.Impl;
import dev.flang.ast.Resolution;

import dev.flang.mir.MIR;

import dev.flang.parser.Parser;

import dev.flang.util.ANY;
import dev.flang.util.Errors;
import dev.flang.util.SourceFile;
import dev.flang.util.SourcePosition;


/**
 * The FrontEnd creates the module IR (mir) from the AST.
 *
 * @author Fridtjof Siebert (siebert@tokiwa.eu)
 */
public class FrontEnd extends ANY
{


  /*----------------------------  variables  ----------------------------*/


  /**
   * The universe is the implicit root of all features that
   * themeselves do not have their own root.
   */
  private Feature _universe;


  /**
   * Configuration
   */
  public final FrontEndOptions _options;


  /*--------------------------  constructors  ---------------------------*/


  public FrontEnd(FrontEndOptions options)
  {
    _options = options;
  }


  /*-----------------------------  methods  -----------------------------*/



  String readStdin()
  {
    var stmnts = new Parser(SourceFile.STDIN).stmntsEof();
    ((Block) _universe.impl.code_).statements_.addAll(stmnts);
    var main = (stmnts.size() == 1 && stmnts.getFirst() instanceof Feature)
      ? ((Feature) stmnts.getFirst()).featureName().baseName()
      : null;
    return main;
  }


  public MIR createMIR()
  {
    /* create the universe */
    _universe = true ? new Feature()
                    : loadUniverse();
    var main = _options._main;
    if (_options._readStdin)
      {
        main = readStdin();
      }
    _universe.findDeclarations(null);
    var res = new Resolution(_options, _universe, (r, f) -> loadInnerFeatures(r, f));

    // NYI: middle end starts here:

    _universe.markUsed(res, SourcePosition.builtIn);
    Feature d = main == null
      ? _universe
      : _universe.markUsedAndGet(res, main);

    res.resolve(); // NYI: This should become the middle end phase!

    if (false && Errors.count() > 0)  // NYI: Eventually, we might want to stop here in case of errors. This is disabled just to check the robustness of the next steps
      {
        Errors.showStatistics();
        System.exit(1);
      }
    if (d != null && Errors.count() == 0)
      {
        if (d.arguments.size() != 0)
          {
            FeErrors.mainFeatureMustNotHaveArguments(d);
          }
        if (d.isField())
          {
            FeErrors.mainFeatureMustNotBeField(d);
          }
        if (d.impl == Impl.ABSTRACT)
          {
            FeErrors.mainFeatureMustNotBeAbstract(d);
          }
        if (d.impl == Impl.INTRINSIC)
          {
            FeErrors.mainFeatureMustNotBeIntrinsic(d);
          }
        if (!d.generics.list.isEmpty())
          {
            FeErrors.mainFeatureMustNotHaveArguments(d);
          }
      }
    return new MIR(d);
  }



  /**
   * NYI: Under development: load universe from sys/universe.fz.
   */
  Feature loadUniverse()
  {
    Feature result = parseFile(FUSION_HOME.resolve("sys").resolve("universe.fz"));
    result.findDeclarations(null);
    new Resolution(_options, result, (r, f) -> loadInnerFeatures(r, f));
    return result;
  }


  /**
   * Check if a sub-directory corresponding to the given feature exists in the
   * source directory with the given root.
   *
   * @param root the top-level directory of the source directory
   *
   * @param f a feature
   *
   * @return a path from root, via the base names of f's outer features to a
   * directory wtih f's base name, null if this does not exist.
   */
  private Dir dirExists(Dir root, Feature f) throws IOException, UncheckedIOException
  {
    var o = f.outer();
    if (o == null)
      {
        return root;
      }
    else
      {
        var d = dirExists(root, o);
        return d == null ? null : d.dir(f.featureName().baseName());
      }
  }


  /**
   * During resolution, load all inner classes of this that are
   * defined in separate files.
   */
  private void loadInnerFeatures(Resolution res, Feature f)
  {
    for (Dir root : SOURCE_DIRS)
      {
        try
          {
            var d = dirExists(root, f);
            if (d != null)
              {
                Files.list(d._dir)
                  .forEach(p ->
                           {
                             if (p.getFileName().toString().endsWith(".fz") && Files.isReadable(p))
                               {
                                 Feature inner = parseFile(p);
                                 check
                                   (inner != null || Errors.count() > 0);
                                 if (inner != null)
                                   {
                                     inner.findDeclarations(f);
                                     inner.scheduleForResolution(res);
                                   }
                               }
                           });
              }
          }
        catch (IOException | UncheckedIOException e)
          {
            Errors.warning("Problem when listing source directory '" + root + "': " + e);
          }
      }
  }


  /**
   * Load and parse the fusion source file for the feature with the
   * given file name
   *
   * @param name a qualified name, e.g. "fusion.std.out"
   *
   * @return the parsed source file or null in case of an error.
   */
  Feature parseFile(Path fname)
  {
    if (_options.verbose(2))
      {
        System.out.println(" - "+fname);
      }
    return new Parser(fname).unit();
  }



  /* NYI: Cleanup: move this directory handling to dev.flang.util.Dir or similar: */
  private static final Path FUSION_HOME;
  private static final Path CURRENT_DIRECTORY;
  private static final Path[] SOURCE_PATHS;
  private static final Dir[] SOURCE_DIRS;
  static {
    CURRENT_DIRECTORY = Path.of(".");
    Class<Feature> cl = Feature.class;
    String clname = cl.getName().replace(".",File.separator)+ ".class";
    String p = Feature.class.getClassLoader().getResource(clname).getPath();
    p = p.substring(0, p.length() - clname.length());
    FUSION_HOME = Path.of(p).getParent();
    SOURCE_PATHS = new Path[] { FUSION_HOME.resolve("lib"), CURRENT_DIRECTORY };
    SOURCE_DIRS = new Dir[SOURCE_PATHS.length];
    for (int i = 0; i < SOURCE_PATHS.length; i++)
      {
        SOURCE_DIRS[i] = new Dir(SOURCE_PATHS[i]);
      }
  }

  /**
   * Class to cache all the sub-dirs within SOURCE_PATHS for loading inner
   * features.
   */
  static class Dir
  {
    Path _dir;
    TreeMap<String, Dir> _subDirs = null;

    /*
     * Create an entry for the given (sub-) directory
     */
    Dir(Path d)
    {
      _dir = d;
    }

    /**
     * If this contains a sub-directory with given name, return it.
     */
    Dir dir(String name) throws IOException, UncheckedIOException
    {
      if (_subDirs == null)
        { // on first call, create dir listing and cache it:
          _subDirs = new TreeMap<>();
          Files.list(_dir)
            .forEach(p ->
                     { if (Files.isDirectory(p))
                         {
                           _subDirs.put(p.getFileName().toString(), new Dir(p));
                         }
                     });
        }
      return _subDirs.get(name);
    }
  }


}

/* end of file */
