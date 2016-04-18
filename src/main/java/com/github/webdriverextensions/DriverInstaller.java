package com.github.webdriverextensions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.FileUtils;

public class DriverInstaller {
    private final File installationDirectory;
    private final Log log;
    private final DriverVersionHandler versionHandler;

    public DriverInstaller(File installationDirectory, Log log) {
        this.installationDirectory = installationDirectory;
        this.log = log;
        this.versionHandler = new DriverVersionHandler(installationDirectory);
    }

    private static boolean directoryContainsSingleDirectory(Path directory) {
        File[] files = directory.toFile().listFiles();
        return files != null && files.length == 1 && files[0].isDirectory();
    }

    private static boolean directoryContainsSingleFile(Path directory) throws MojoExecutionException {
        File[] files = directory.toFile().listFiles();
        return files != null && files.length == 1 && files[0].isFile();
    }

    private void moveDirectoryInDirectory(Path from, Path to) throws MojoExecutionException {
        assert directoryContainsSingleDirectory(from);
        try {
            List<String> subDirectories = FileUtils.getDirectoryNames(from.toFile(), null, null, true);
            if (to.toFile().exists()){
                org.apache.commons.io.FileUtils.deleteDirectory(to.toFile());
            }

            File singleDirectory = new File(subDirectories.get(1));
            log.info("  Moving " + singleDirectory + " -> " + to);
            org.apache.commons.io.FileUtils.moveDirectory(singleDirectory, to.toFile());
        } catch (IOException ex) {
            throw new MojoExecutionException("Error when moving directory in directory " + Utils.quote(from) + " to " + Utils.quote(to), ex);
        }
    }

    private void moveFileInDirectory(Path from, Path to) throws MojoExecutionException {
        assert directoryContainsSingleFile(from);
        try {
            List<String> files = FileUtils.getFileNames(from.toFile(), null, null, true);
            File singleFile = new File(files.get(0));
            log.info("  Moving " + singleFile + " -> " + to);
            FileUtils.rename(singleFile, to.toFile());
        } catch (IOException ex) {
            throw new MojoExecutionException("Error when moving file in directory " + Utils.quote(from) + " to " + Utils.quote(to), ex);
        }
    }

    // TODO: Investigate if this method does what it should do, should the method name be changed or the method implementation
    private void moveAllFilesInDirectory(Path from, Path to) throws MojoExecutionException {
        try {
            List<String> subDirectories = FileUtils.getDirectoryNames(from.toFile(), null, null, true);
            log.info("  Moving " + subDirectories.get(0) + " -> " + to);
            FileUtils.rename(new File(subDirectories.get(0)), to.toFile());
        } catch (IOException ex) {
            throw new MojoExecutionException("Error when moving directory " + Utils.quote(from) + " to " + Utils.quote(to), ex);
        }
    }

    public void install(Driver driver, Path extractLocation) throws MojoExecutionException {
        if (directoryContainsSingleDirectory(extractLocation)) {
            moveDirectoryInDirectory(extractLocation, Paths.get(installationDirectory.getPath(), driver.getId()));
        } else if (directoryContainsSingleFile(extractLocation)) {
            moveFileInDirectory(extractLocation, Paths.get(installationDirectory.getPath(), driver.getFileName()));
            makeExecutable(installationDirectory + "/" + driver.getFileName());
        } else {
            moveAllFilesInDirectory(extractLocation, Paths.get(installationDirectory.getPath(), driver.getId()));
        }

        versionHandler.writeVersionFile(driver);
    }


    private static void makeExecutable(String path) {
        File file = new File(path);
        if (file.exists() && !file.canExecute()) {
            file.setExecutable(true);
        }
    }

    private boolean isInstalled(Driver driver) {
        Path path = Paths.get(installationDirectory.getPath(), driver.getFileName());
        return path.toFile().exists();
    }

    public boolean needInstallation(Driver driver) throws MojoExecutionException {
        return !isInstalled(driver) || !versionHandler.isSameVersion(driver);
    }
}
