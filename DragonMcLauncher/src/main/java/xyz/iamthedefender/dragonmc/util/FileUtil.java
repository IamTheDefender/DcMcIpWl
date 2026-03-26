package xyz.iamthedefender.dragonmc.util;

import xyz.iamthedefender.dragonmc.Main;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public class FileUtil {

    public static File setupYamlFile(String file, @Nullable String resourceStreamId) {
        String currentWorkingDir = getCurrentDir().getAbsolutePath();
        File fileObj = new File(currentWorkingDir + "/launcher", file + ".yml");

        if (!fileObj.exists()) {
            if (resourceStreamId != null) {
                try (InputStream input = Main.class.getResourceAsStream("/" + resourceStreamId)) {
                    Path outputPath = fileObj.toPath();
                    Files.createDirectories(outputPath.getParent());
                    Files.copy(Objects.requireNonNull(input), outputPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else {
                try {
                    fileObj.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return fileObj;
    }

    public static File getCurrentDir() {
        return new File(System.getProperty("user.dir"));
    }

    public static File getLauncherDir() {
        return new File(getCurrentDir(), "launcher");
    }

}
