package pw.yumc.MiaoNashorn;

import sun.misc.Unsafe;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 喵式脚本
 *
 * @author 喵♂呜
 * @since 2016年8月29日 上午7:50:39
 */
public class MiaoNashorn {
    private static String MavenRepo = "https://maven.aliyun.com/repository/public";
    private static final Object ucp;
    private static final MethodHandle addURLMethodHandle;

    static {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe unsafe = (Unsafe) theUnsafe.get(null);
            Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            MethodHandles.Lookup lookup = (MethodHandles.Lookup) unsafe.getObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field));
            Field ucpField;
            try {
                ucpField = loader.getClass().getDeclaredField("ucp");
            } catch (NoSuchFieldException e) {
                ucpField = loader.getClass().getSuperclass().getDeclaredField("ucp");
            }
            long offset = unsafe.objectFieldOffset(ucpField);
            ucp = unsafe.getObject(loader, offset);
            Method method = ucp.getClass().getDeclaredMethod("addURL", URL.class);
            addURLMethodHandle = lookup.unreflect(method);
            createEngine();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void log(String format, Object... args) {
        System.out.println("[MiaoNashorn] " + String.format(format, args));
    }

    private static void createEngine() throws Throwable {
        String extDirs = System.getProperty("java.ext.dirs");
        if (extDirs != null) {
            String[] dirs = extDirs.split(File.pathSeparator);
            for (String dir : dirs) {
                File nashorn = new File(dir, "nashorn.jar");
                if (nashorn.exists()) {
                    loadJar(nashorn);
                    System.out.println("扩展目录发现 Nashorn 已加载完成!");
                }
            }
        } else {
            loadLocalNashorn();
        }
    }

    private static void loadLocalNashorn() throws Throwable {
        File libRootFile = new File("plugins/MiaoNashorn", "libs");
        libRootFile.mkdirs();
        log("从云端加载 Nashorn 请稍候...");
        String libRoot = libRootFile.getCanonicalPath();
        downloadJar(libRoot, "org.openjdk.nashorn", "nashorn-core", "15.2");
        downloadJar(libRoot, "org.ow2.asm", "asm", "9.1");
        downloadJar(libRoot, "org.ow2.asm", "asm-commons", "9.1");
        downloadJar(libRoot, "org.ow2.asm", "asm-tree", "9.1");
        downloadJar(libRoot, "org.ow2.asm", "asm-util", "9.1");
        log("云端 Nashorn 已加载完成!");
    }

    private static void loadJar(File file) throws Throwable {
        addURLMethodHandle.invoke(ucp, file.toURI().toURL());
    }

    private static void downloadJar(String engineRoot, String groupId, String artifactId, String version) throws Throwable {
        File lib = new File(engineRoot, artifactId + ".jar");
        if (!lib.exists()) {
            log("正在下载类库 %s 版本 %s 请稍候...", artifactId, version);
            Files.copy(new URL(MavenRepo +
                            String.format("/%1$s/%2$s/%3$s/%2$s-%3$s.jar",
                                    groupId.replace(".", "/"),
                                    artifactId,
                                    version)
                    ).openStream(),
                    lib.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        loadJar(lib);
    }
}
