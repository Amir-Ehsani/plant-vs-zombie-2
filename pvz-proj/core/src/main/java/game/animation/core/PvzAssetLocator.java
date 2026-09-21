package game.animation.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Resolves the pvz-assets folder for both Gradle runs (files on disk) and a
 * packaged JAR (assets are unpacked once to a cache under the user home).
 */
public final class PvzAssetLocator {
    private static final String ASSET_ROOT = "pvz-assets";
    private static final String CACHE_FOLDER = ".plants-vs-zombies-2";
    private static final String READY_MARKER = "pvz-assets.ready";
    private static File resolvedDirectory;

    private PvzAssetLocator() {
    }

    public static void prepare() {
        resolvedDirectory = resolveDirectory();
    }

    public static FileHandle locate() {
        if (resolvedDirectory == null) {
            resolvedDirectory = resolveDirectory();
        }
        if (resolvedDirectory == null) {
            return null;
        }
        return Gdx.files.absolute(resolvedDirectory.getAbsolutePath());
    }

    private static File resolveDirectory() {
        File currentDirectory = new File(System.getProperty("user.dir"));
        File[] diskCandidates = {
            new File(currentDirectory, ASSET_ROOT),
            new File(currentDirectory, "assets/" + ASSET_ROOT),
            parentAssets(currentDirectory),
            grandParentAssets(currentDirectory)
        };
        for (File candidate : diskCandidates) {
            if (isValidAssetsDirectory(candidate)) {
                return candidate;
            }
        }

        File jar = runningJar();
        File cacheRoot = new File(System.getProperty("user.home"), CACHE_FOLDER);
        File extracted = new File(cacheRoot, ASSET_ROOT);
        File marker = new File(cacheRoot, READY_MARKER);
        if (isValidAssetsDirectory(extracted) && isFresh(marker, jar)) {
            return extracted;
        }
        if (jar != null && extractFromJar(jar, cacheRoot, extracted, marker)) {
            return extracted;
        }
        return isValidAssetsDirectory(extracted) ? extracted : null;
    }

    private static File parentAssets(File currentDirectory) {
        File parent = currentDirectory.getParentFile();
        return parent == null ? null : new File(parent, "assets/" + ASSET_ROOT);
    }

    private static File grandParentAssets(File currentDirectory) {
        File parent = currentDirectory.getParentFile();
        if (parent == null || parent.getParentFile() == null) {
            return null;
        }
        return new File(parent.getParentFile(), "assets/" + ASSET_ROOT);
    }

    private static boolean isValidAssetsDirectory(File root) {
        if (root == null || !root.isDirectory()) {
            return false;
        }
        File resources = firstExisting(root, "RESOURCES.json", "Resources.json", "resources.json");
        File animations = new File(root, "animations.json");
        File atlases = firstExisting(root, "ATLASES", "atlases");
        File images = firstExisting(root, "IMAGES", "images", "pam");
        return resources != null && resources.isFile()
                && animations.isFile()
                && atlases != null && atlases.isDirectory()
                && images != null && images.isDirectory();
    }

    private static File firstExisting(File root, String... names) {
        for (String name : names) {
            File child = new File(root, name);
            if (child.exists()) {
                return child;
            }
        }
        return null;
    }

    private static boolean isFresh(File marker, File jar) {
        if (marker == null || !marker.isFile()) {
            return false;
        }
        return jar == null || marker.lastModified() >= jar.lastModified();
    }

    private static File runningJar() {
        try {
            URL location = PvzAssetLocator.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) {
                return null;
            }
            File file = new File(location.toURI());
            if (file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                return file;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static boolean extractFromJar(File jar, File cacheRoot, File extracted, File marker) {
        System.out.println("Unpacking game assets (first launch may take a minute)...");
        deleteTree(extracted);
        if (marker.exists() && !marker.delete()) {
            return false;
        }
        if (!cacheRoot.exists() && !cacheRoot.mkdirs()) {
            return false;
        }
        String prefix = ASSET_ROOT + "/";
        try (ZipFile zip = new ZipFile(jar)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            byte[] buffer = new byte[64 * 1024];
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName().replace('\\', '/');
                if (!name.startsWith(prefix)) {
                    continue;
                }
                File out = new File(cacheRoot, name);
                if (!out.getCanonicalPath().startsWith(cacheRoot.getCanonicalPath() + File.separator)
                        && !out.getCanonicalFile().equals(cacheRoot.getCanonicalFile())) {
                    continue;
                }
                if (entry.isDirectory() || name.endsWith("/")) {
                    out.mkdirs();
                    continue;
                }
                File parent = out.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    return false;
                }
                try (InputStream input = zip.getInputStream(entry);
                        OutputStream output = new FileOutputStream(out)) {
                    int read;
                    while ((read = input.read(buffer)) >= 0) {
                        output.write(buffer, 0, read);
                    }
                }
            }
        } catch (Exception exception) {
            deleteTree(extracted);
            exception.printStackTrace();
            return false;
        }
        if (!isValidAssetsDirectory(extracted)) {
            deleteTree(extracted);
            return false;
        }
        try (OutputStream markerOut = new FileOutputStream(marker)) {
            markerOut.write(Long.toString(jar.lastModified()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception exception) {
            return isValidAssetsDirectory(extracted);
        }
        System.out.println("Game assets are ready.");
        return true;
    }

    private static void deleteTree(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteTree(child);
            }
        }
        file.delete();
    }
}
