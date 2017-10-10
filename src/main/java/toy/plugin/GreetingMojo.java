package toy.plugin;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.surefire.util.DefaultScanResult;
import org.apache.maven.plugin.surefire.AbstractSurefireMojo;
import org.apache.maven.plugin.surefire.SurefirePlugin;
import edu.illinois.starts.helpers.PomUtil;
import edu.illinois.starts.helpers.Writer;
import org.apache.maven.model.Plugin;


/**
 * Says "Hi" to the user.
 *
 */
@Mojo( name = "sayhi", requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class GreetingMojo extends SurefirePlugin
{
    public void execute() throws MojoExecutionException
    {
        getLog().info( "Hello, world. " );

        DefaultScanResult defaultScanResult = null;
        try {
            Method scanMethod = AbstractSurefireMojo.class.getDeclaredMethod("scanForTestClasses", null);
            scanMethod.setAccessible(true);
            defaultScanResult = (DefaultScanResult) scanMethod.invoke(this, null);
        } catch (NoSuchMethodException nsme) {
            nsme.printStackTrace();
        } catch (InvocationTargetException ite) {
            ite.printStackTrace();
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
        }
        List<String> testFiles =  (List<String>) defaultScanResult.getFiles();

        Plugin sfPlugin = PomUtil.getSfPlugin(getProject());
        List<String> excludePaths = Writer.fqnsToExcludePath(testFiles);
        appendExcludesListToExcludesFile(sfPlugin, getExcludes(), excludePaths, getWorkingDirectory());
        //CONFUSED: get "cannot find symbol" for
        //PomUtil.appendExcludesListToExcludesFile(sfPlugin, getExcludes(), excludePaths, getWorkingDirectory());
        //So I copied the method below.
    }

    public static void appendExcludesListToExcludesFile(Plugin plugin, List<String> originalExcludes, List<String> excludes,
                                                        File baseDir) throws MojoExecutionException {
        String excludesParam = PomUtil.extractParamValue(plugin, "excludesFile");
        File excludesFile = new File(baseDir.getAbsolutePath() + File.separator + excludesParam);
        PrintWriter writer = null;

        try {
            writer = new PrintWriter(new FileOutputStream(excludesFile, true), true);
            for (String e : excludes) {
                writer.println(e);
                writer.println("STARTS_EXCLUDE_MARKER");
            }
            //TODO: we need to confirm that this is really expected behavior from surefire
            //If surefire already declares <excludes>, then we should *not* use the default excludes regex.
            if (originalExcludes.isEmpty()) {
                writer.println("**/*$*");
            }
        } catch (IOException ioe) {
            throw new MojoExecutionException("Could not access excludesFile", ioe);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}