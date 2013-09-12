package org.github.rzymek.maven;

import com.google.common.base.Objects;
import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;
import java.util.List;
import java.util.Map;
import org.apache.maven.Maven;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.prefix.DefaultPluginPrefixRequest;
import org.apache.maven.plugin.prefix.PluginPrefixResolver;
import org.apache.maven.plugin.prefix.PluginPrefixResult;
import org.apache.maven.plugin.version.DefaultPluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.plugin.version.PluginVersionResult;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.github.rzymek.maven.Watch;

@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@SuppressWarnings("all")
public class Watcher extends AbstractMojo {
  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  private MavenSession session;
  
  @Parameter(required = true)
  private List<Watch> watch;
  
  @Component
  private PluginPrefixResolver pluginPrefixResolver;
  
  @Component
  private PluginVersionResolver pluginVersionResolver;
  
  @Component
  private Maven maven;
  
  public void execute() throws MojoExecutionException, MojoFailureException {
    for (final Watch w : this.watch) {
      this.register(w.on);
    }
    final Map<String,List<String>> watchMap = CollectionLiterals.<String, List<String>>newHashMap();
    for (final Watch w_1 : this.watch) {
      {
        String[] _split = w_1.run.split(" ");
        final Function1<String,String> _function = new Function1<String,String>() {
            public String apply(final String it) {
              String _trim = it.trim();
              return _trim;
            }
          };
        final List<String> goal = ListExtensions.<String, String>map(((List<String>)Conversions.doWrapArray(_split)), _function);
        String _absolutePath = w_1.on.getAbsolutePath();
        watchMap.put(_absolutePath, goal);
      }
    }
    Log _log = this.getLog();
    final Function1<Watch,File> _function = new Function1<Watch,File>() {
        public File apply(final Watch it) {
          return it.on;
        }
      };
    List<File> _map = ListExtensions.<Watch, File>map(this.watch, _function);
    String _plus = ("Waiting: " + _map);
    _log.info(_plus);
    final Procedure1<File> _function_1 = new Procedure1<File>() {
        public void apply(final File it) {
          String _absolutePath = it.getAbsolutePath();
          final List<String> goals = watchMap.get(_absolutePath);
          Log _log = Watcher.this.getLog();
          String _plus = (it + " -> ");
          String _plus_1 = (_plus + goals);
          _log.debug(_plus_1);
          boolean _notEquals = (!Objects.equal(goals, null));
          if (_notEquals) {
            Log _log_1 = Watcher.this.getLog();
            String _plus_2 = (it + " changed -> [");
            String _join = IterableExtensions.join(goals, " ");
            String _plus_3 = (_plus_2 + _join);
            String _plus_4 = (_plus_3 + "]");
            _log_1.info(_plus_4);
            MavenExecutionRequest _request = Watcher.this.session.getRequest();
            final MavenExecutionRequest request = DefaultMavenExecutionRequest.copy(_request);
            request.setGoals(goals);
            Watcher.this.maven.execute(request);
          }
        }
      };
    this.watchLoop(_function_1);
  }
  
  public Plugin resolve(final String prefix) {
    try {
      DefaultPluginPrefixRequest _defaultPluginPrefixRequest = new DefaultPluginPrefixRequest(prefix, this.session);
      PluginPrefixResult pluginResult = this.pluginPrefixResolver.resolve(_defaultPluginPrefixRequest);
      Plugin _plugin = new Plugin();
      final Plugin plugin = _plugin;
      String _groupId = pluginResult.getGroupId();
      plugin.setGroupId(_groupId);
      String _artifactId = pluginResult.getArtifactId();
      plugin.setArtifactId(_artifactId);
      DefaultPluginVersionRequest _defaultPluginVersionRequest = new DefaultPluginVersionRequest(plugin, 
        this.session);
      DefaultPluginVersionRequest versionRequest = _defaultPluginVersionRequest;
      PluginVersionResult _resolve = this.pluginVersionResolver.resolve(versionRequest);
      String _version = _resolve.getVersion();
      plugin.setVersion(_version);
      return plugin;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  private final WatchService watchService = new Function0<WatchService>() {
    public WatchService apply() {
      try {
        FileSystem _default = FileSystems.getDefault();
        WatchService _newWatchService = _default.newWatchService();
        return _newWatchService;
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    }
  }.apply();
  
  public void watch(final File file, final Procedure1<? super File> run) {
    this.register(file);
    this.watchLoop(run);
  }
  
  public void watchLoop(final Procedure1<? super File> run) {
    try {
      boolean valid = true;
      boolean _while = valid;
      while (_while) {
        {
          Log _log = this.getLog();
          _log.info("Waiting for modifications...");
          final WatchKey key = this.watchService.take();
          List<WatchEvent<? extends Object>> _pollEvents = key.pollEvents();
          final Function1<WatchEvent<? extends Object>,WatchEvent<? extends Object>> _function = new Function1<WatchEvent<? extends Object>,WatchEvent<? extends Object>>() {
              public WatchEvent<? extends Object> apply(final WatchEvent<? extends Object> it) {
                WatchEvent<? extends Object> _xblockexpression = null;
                {
                  Log _log = Watcher.this.getLog();
                  Kind<? extends Object> _kind = it.kind();
                  String _plus = ("WatchService event: " + _kind);
                  String _plus_1 = (_plus + ":");
                  Object _context = it.context();
                  String _plus_2 = (_plus_1 + _context);
                  _log.debug(_plus_2);
                  _xblockexpression = (it);
                }
                return _xblockexpression;
              }
            };
          List<WatchEvent<? extends Object>> _map = ListExtensions.<WatchEvent<? extends Object>, WatchEvent<? extends Object>>map(_pollEvents, _function);
          final Function1<WatchEvent<? extends Object>,WatchEvent<Path>> _function_1 = new Function1<WatchEvent<? extends Object>,WatchEvent<Path>>() {
              public WatchEvent<Path> apply(final WatchEvent<? extends Object> it) {
                return ((WatchEvent<Path>) it);
              }
            };
          List<WatchEvent<Path>> _map_1 = ListExtensions.<WatchEvent<? extends Object>, WatchEvent<Path>>map(_map, _function_1);
          final Function1<WatchEvent<Path>,Boolean> _function_2 = new Function1<WatchEvent<Path>,Boolean>() {
              public Boolean apply(final WatchEvent<Path> it) {
                Kind<Path> _kind = it.kind();
                boolean _notEquals = (!Objects.equal(_kind, StandardWatchEventKinds.OVERFLOW));
                return Boolean.valueOf(_notEquals);
              }
            };
          Iterable<WatchEvent<Path>> _filter = IterableExtensions.<WatchEvent<Path>>filter(_map_1, _function_2);
          final Function1<WatchEvent<Path>,Path> _function_3 = new Function1<WatchEvent<Path>,Path>() {
              public Path apply(final WatchEvent<Path> it) {
                Watchable _watchable = key.watchable();
                Path _context = it.context();
                Path _resolve = ((Path) _watchable).resolve(_context);
                return _resolve;
              }
            };
          Iterable<Path> _map_2 = IterableExtensions.<WatchEvent<Path>, Path>map(_filter, _function_3);
          final Function1<Path,File> _function_4 = new Function1<Path,File>() {
              public File apply(final Path it) {
                File _file = it.toFile();
                return _file;
              }
            };
          Iterable<File> _map_3 = IterableExtensions.<Path, File>map(_map_2, _function_4);
          final Procedure1<File> _function_5 = new Procedure1<File>() {
              public void apply(final File it) {
                run.apply(it);
              }
            };
          IterableExtensions.<File>forEach(_map_3, _function_5);
          boolean _reset = key.reset();
          valid = _reset;
        }
        _while = valid;
      }
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public WatchKey register(final File file) {
    try {
      WatchKey _xblockexpression = null;
      {
        File _absoluteFile = file.getAbsoluteFile();
        String _parent = _absoluteFile.getParent();
        final Path path = Paths.get(_parent);
        Log _log = this.getLog();
        String _plus = ("register: " + path);
        String _plus_1 = (_plus + "\t");
        String _plus_2 = (_plus_1 + file);
        String _plus_3 = (_plus_2 + "\n");
        boolean _isAbsolute = path.isAbsolute();
        String _plus_4 = (_plus_3 + Boolean.valueOf(_isAbsolute));
        _log.debug(_plus_4);
        WatchKey _register = path.register(this.watchService, StandardWatchEventKinds.ENTRY_MODIFY);
        _xblockexpression = (_register);
      }
      return _xblockexpression;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}
