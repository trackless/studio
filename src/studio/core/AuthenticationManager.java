package studio.core;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AuthenticationManager {
    private static AuthenticationManager instance;
    private final Map classMap = new HashMap();

    public Class lookup(String autheticationMethod) {
        return (Class) classMap.get(autheticationMethod);
    }

    public String[] getAuthenticationMechanisms() {
        Set s = classMap.keySet();
        return (String[]) s.toArray(new String[0]);
    }

    public synchronized static AuthenticationManager getInstance() {
        if (instance == null)
            instance = new AuthenticationManager();

        return instance;
    }

    private AuthenticationManager() {
        DefaultAuthenticationMechanism dam = new DefaultAuthenticationMechanism();
        classMap.put(dam.getMechanismName(), dam.getClass());

        String curDir = System.getProperty("user.dir");
        curDir = curDir + "/plugins";

        //   System.out.println("Looking for plugins at " + curDir);

        File dir = new File(curDir);
        if (!dir.exists())
            return;

        FilenameFilter filter = (dir1, name) -> name.endsWith(".jar");

        String[] children = dir.list(filter);
        if (children != null)
            for (String s : children) {
                String filename = dir.getAbsolutePath() + "/" + s;
                try {
                    URL url = new URI("jar:file" + filename + "/!;").toURL();
                    JarURLConnection conn = (JarURLConnection) url.openConnection();
                    JarFile jarFile = conn.getJarFile();

                    Enumeration e = jarFile.entries();
                    while (e.hasMoreElements()) {
                        JarEntry entry = (JarEntry) e.nextElement();
                        String name = entry.getName();
                        if (!entry.isDirectory() && name.endsWith(".class")) {
                            URLClassLoader loader = new URLClassLoader(new URL[]{url});
                            String externalName = name.substring(0, name.indexOf('.')).replace('/', '.');
                            try {
                                Class c = loader.loadClass(externalName);
                                if (IAuthenticationMechanism.class.isAssignableFrom(c)) {
                                    IAuthenticationMechanism am = (IAuthenticationMechanism) c.getDeclaredConstructor().newInstance();
                                    classMap.put(am.getMechanismName(), c);
                                }
                            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException |
                                     Error ignored) {
                            } catch (InvocationTargetException | NoSuchMethodException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error loading plugin: " + filename);
                    e.printStackTrace(System.err);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
    }
}
