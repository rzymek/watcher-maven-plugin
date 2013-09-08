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
import java.util.List;
import java.util.Map;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
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
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;
import org.eclipse.xtext.xbase.lib.Pair;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.github.rzymek.maven.PluginGoal;
import org.github.rzymek.maven.Watch;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;
import org.twdata.maven.mojoexecutor.MojoExecutor;
import org.twdata.maven.mojoexecutor.MojoExecutor.ExecutionEnvironment;

@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@SuppressWarnings("all")
public class Watcher extends AbstractMojo {
  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;
  
  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  private MavenSession session;
  
  @Component
  private BuildPluginManager buildPluginManager;
  
  @Parameter(required = true)
  private List<Watch> watch;
  
  @Component
  private PluginPrefixResolver pluginPrefixResolver;
  
  @Component
  private PluginVersionResolver pluginVersionResolver;
  
  public void execute() throws MojoExecutionException, MojoFailureException {
    for (final Watch w : this.watch) {
      this.register(w.on);
    }
    final Map<String,List<PluginGoal>> watchMap = CollectionLiterals.<String, List<PluginGoal>>newHashMap();
    for (final Watch w_1 : this.watch) {
      {
        String[] _split = w_1.run.split(" ");
        final Function1<String,List<String>> _function = new Function1<String,List<String>>() {
          public List<String> apply(final String it) {
            String[] _split = it.split(":");
            final Function1<String,String> _function = new Function1<String,String>() {
              public String apply(final String it) {
                String _trim = it.trim();
                return _trim;
              }
            };
            List<String> _map = ListExtensions.<String, String>map(((List<String>)Conversions.doWrapArray(_split)), _function);
            return _map;
          }
        };
        List<List<String>> _map = ListExtensions.<String, List<String>>map(((List<String>)Conversions.doWrapArray(_split)), _function);
        final Function1<List<String>,Pair<String,String>> _function_1 = new Function1<List<String>,Pair<String,String>>() {
          public Pair<String,String> apply(final List<String> it) {
            String _get = it.get(0);
            String _get_1 = it.get(1);
            Pair<String,String> _mappedTo = Pair.<String, String>of(_get, _get_1);
            return _mappedTo;
          }
        };
        List<Pair<String,String>> _map_1 = ListExtensions.<List<String>, Pair<String,String>>map(_map, _function_1);
        final Function1<Pair<String,String>,PluginGoal> _function_2 = new Function1<Pair<String,String>,PluginGoal>() {
          public PluginGoal apply(final Pair<String,String> it) {
            String _key = it.getKey();
            Plugin _resolve = Watcher.this.resolve(_key);
            String _value = it.getValue();
            PluginGoal _pluginGoal = new PluginGoal(_resolve, _value);
            return _pluginGoal;
          }
        };
        final List<PluginGoal> goal = ListExtensions.<Pair<String,String>, PluginGoal>map(_map_1, _function_2);
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
        List<PluginGoal> _get = watchMap.get(_absolutePath);
        if (_get!=null) {
          final Procedure1<PluginGoal> _function = new Procedure1<PluginGoal>() {
            public void apply(final PluginGoal it) {
              try {
                Plugin _plugin = it.getPlugin();
                String _goal = it.getGoal();
                Xpp3Dom _configuration = MojoExecutor.configuration();
                ExecutionEnvironment _executionEnvironment = MojoExecutor.executionEnvironment(
                  Watcher.this.project, 
                  Watcher.this.session, 
                  Watcher.this.buildPluginManager);
                MojoExecutor.executeMojo(_plugin, _goal, _configuration, _executionEnvironment);
              } catch (Throwable _e) {
                throw Exceptions.sneakyThrow(_e);
              }
            }
          };
          IterableExtensions.<PluginGoal>forEach(_get, _function);
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
      RepositorySystemSession _repositorySession = this.session.getRepositorySession();
      List<RemoteRepository> _remotePluginRepositories = this.project.getRemotePluginRepositories();
      DefaultPluginVersionRequest _defaultPluginVersionRequest = new DefaultPluginVersionRequest(plugin, _repositorySession, _remotePluginRepositories);
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
      WatchKey key = null;
      boolean _dowhile = false;
      do {
        {
          Log _log = this.getLog();
          _log.info("Waiting for modifications...");
          WatchKey _take = this.watchService.take();
          key = _take;
          List<WatchEvent<? extends Object>> _pollEvents = key.pollEvents();
          final Function1<WatchEvent<? extends Object>,Boolean> _function = new Function1<WatchEvent<? extends Object>,Boolean>() {
            public Boolean apply(final WatchEvent<? extends Object> it) {
              Kind<? extends Object> _kind = it.kind();
              boolean _notEquals = (!Objects.equal(_kind, StandardWatchEventKinds.OVERFLOW));
              return Boolean.valueOf(_notEquals);
            }
          };
          Iterable<WatchEvent<? extends Object>> _filter = IterableExtensions.<WatchEvent<? extends Object>>filter(_pollEvents, _function);
          final Function1<WatchEvent<? extends Object>,WatchEvent<Path>> _function_1 = new Function1<WatchEvent<? extends Object>,WatchEvent<Path>>() {
            public WatchEvent<Path> apply(final WatchEvent<? extends Object> it) {
              return ((WatchEvent<Path>) it);
            }
          };
          Iterable<WatchEvent<Path>> _map = IterableExtensions.<WatchEvent<? extends Object>, WatchEvent<Path>>map(_filter, _function_1);
          final Function1<WatchEvent<Path>,File> _function_2 = new Function1<WatchEvent<Path>,File>() {
            public File apply(final WatchEvent<Path> it) {
              Path _context = it.context();
              File _file = _context.toFile();
              return _file;
            }
          };
          Iterable<File> _map_1 = IterableExtensions.<WatchEvent<Path>, File>map(_map, _function_2);
          final Procedure1<File> _function_3 = new Procedure1<File>() {
            public void apply(final File it) {
              run.apply(it);
            }
          };
          IterableExtensions.<File>forEach(_map_1, _function_3);
        }
        boolean _reset = key.reset();
        _dowhile = _reset;
      } while(_dowhile);
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
        WatchKey _register = path.register(this.watchService, StandardWatchEventKinds.ENTRY_MODIFY);
        _xblockexpression = (_register);
      }
      return _xblockexpression;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}
