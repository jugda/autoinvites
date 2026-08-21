///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS org.junit.jupiter:junit-jupiter:6.1.3
//DEPS org.junit.platform:junit-platform-console-standalone:6.1.3
//DEPS com.icegreen:greenmail:2.1.12
//SOURCES ../src/*.java
//SOURCES *Test.java

import org.junit.platform.console.ConsoleLauncher;
import java.nio.file.Path;

/**
 * Runs the whole suite: {@code jbang test/Tests.java}
 *
 * <p>The production sources come in via //SOURCES, which also brings their //DEPS and the
 * //FILES templates, so there is a single dependency list to maintain.
 *
 * <p>JBang has no {@code test} command, and a bare {@code --scan-classpath} finds nothing
 * because the compiled classes live in a jar in JBang's cache. This class locates that jar
 * from its own code source and points the launcher at it, so test classes are discovered
 * automatically and adding one needs no registration.
 */
public class Tests {

  public static void main(String[] args) throws Exception {
    var jar = Path.of(Tests.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    // ConsoleLauncher.main exits non-zero when anything fails, which is what CI needs.
    ConsoleLauncher.main(args.length > 0
        ? args
        : new String[] {"execute", "--scan-classpath=" + jar, "--details=tree"});
  }
}
