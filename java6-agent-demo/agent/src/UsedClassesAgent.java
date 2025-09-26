import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Java 6 compatible Java Agent that records actually loaded application classes
 * and their originating JARs (via ProtectionDomain CodeSource location).
 *
 * Usage (after building the agent JAR with proper MANIFEST):
 *   java -javaagent:/path/used-classes-agent.jar=out=/tmp/used-classes.csv -jar app.jar
 */
public class UsedClassesAgent {

    private static final Set<String> loadedJarUrls = Collections.newSetFromMap(
            new ConcurrentHashMap<String, Boolean>());

    private static final Set<String> loadedClassNames = Collections.newSetFromMap(
            new ConcurrentHashMap<String, Boolean>());

    private static volatile String outputFilePath = System.getProperty("user.home")
            + File.separator + "used-classes.csv";

    public static void premain(String agentArgs, Instrumentation inst) {
        init(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        init(agentArgs, inst);
    }

    private static void init(String agentArgs, Instrumentation inst) {
        parseArgs(agentArgs);

        // Record already loaded classes (useful for dynamic attach)
        try {
            Class<?>[] all = inst.getAllLoadedClasses();
            if (all != null) {
                for (int i = 0; i < all.length; i++) {
                    recordClass(all[i]);
                }
            }
        } catch (Throwable t) {
            // best-effort, avoid failing the target app
            t.printStackTrace();
        }

        inst.addTransformer(new ClassFileTransformer() {
            public byte[] transform(
                    ClassLoader loader,
                    String className,
                    Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain,
                    byte[] classfileBuffer) throws IllegalClassFormatException {
                if (className != null) {
                    recordClass(className, loader, protectionDomain);
                }
                return null; // no transformation
            }
        }, false);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                dumpToFile();
            }
        }, "used-classes-agent-dump"));
    }

    private static void parseArgs(String agentArgs) {
        if (agentArgs == null || agentArgs.length() == 0) {
            return;
        }
        // very simple parser: out=/path, other args ignored
        String[] parts = split(agentArgs, ',');
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p == null) continue;
            p = p.trim();
            if (p.startsWith("out=")) {
                String val = p.substring("out=".length());
                if (val.length() > 0) {
                    outputFilePath = val;
                }
            }
        }
    }

    private static String[] split(String s, char sep) {
        // Avoid using String.split regex for JDK 6 performance and allocation reasons
        if (s == null) return new String[0];
        int count = 1;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == sep) count++;
        }
        String[] arr = new String[count];
        int idx = 0;
        int last = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == sep) {
                arr[idx++] = s.substring(last, i);
                last = i + 1;
            }
        }
        arr[idx] = s.substring(last);
        return arr;
    }

    private static void recordClass(Class<?> klass) {
        if (klass == null) return;
        String name = klass.getName().replace('.', '/');
        ClassLoader loader = klass.getClassLoader();
        ProtectionDomain pd = klass.getProtectionDomain();
        recordClass(name, loader, pd);
    }

    private static void recordClass(String internalName,
                                    ClassLoader loader,
                                    ProtectionDomain protectionDomain) {
        // exclude bootstrap-loaded and JDK classes
        if (internalName == null) return;
        String dotted = internalName.replace('/', '.');
        if (loader == null) return;
        if (startsWithAny(dotted, new String[]{
                "java.", "javax.", "sun.", "com.sun."
        })) return;

        loadedClassNames.add(dotted);

        String src = null;
        try {
            if (protectionDomain != null && protectionDomain.getCodeSource() != null) {
                URL loc = protectionDomain.getCodeSource().getLocation();
                if (loc != null) {
                    src = loc.toString();
                }
            }
        } catch (Throwable t) {
            // ignore and keep src as null
        }

        if (src == null) return;
        // ignore obvious JRE locations (rt.jar, jre/lib, Apple classes.jar)
        if (containsAny(src, new String[]{
                "/jre/lib/", "/lib/rt.jar", "classes.jar"
        })) return;

        loadedJarUrls.add(src);
    }

    private static boolean startsWithAny(String s, String[] prefixes) {
        for (int i = 0; i < prefixes.length; i++) {
            if (s.startsWith(prefixes[i])) return true;
        }
        return false;
    }

    private static boolean containsAny(String s, String[] tokens) {
        for (int i = 0; i < tokens.length; i++) {
            if (s.indexOf(tokens[i]) >= 0) return true;
        }
        return false;
    }

    private static void dumpToFile() {
        PrintWriter pw = null;
        try {
            File out = new File(outputFilePath);
            File parent = out.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            pw = new PrintWriter(new FileWriter(out));
            pw.println("# jars");
            for (String j : loadedJarUrls) {
                pw.println(j);
            }
            pw.println("# classes");
            for (String c : loadedClassNames) {
                pw.println(c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pw != null) {
                try { pw.close(); } catch (Throwable t) { /* ignore */ }
            }
        }
    }
}
