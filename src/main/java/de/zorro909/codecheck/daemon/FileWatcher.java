package de.zorro909.codecheck.daemon;

import de.zorro909.codecheck.RepositoryPathProvider;
import de.zorro909.codecheck.RequiresCliOption;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.nio.file.StandardWatchEventKinds.*;


/**
 * Monitors a directory and its subdirectories for file events (creation, deletion, modification).
 * When a file event occurs, it updates the files in the DaemonServer.
 * This class implements the Runnable interface, so it can be run in a separate thread.
 */
@RequiresCliOption("--watch")
@Requires(beans = DaemonServer.class)
@Singleton
public class FileWatcher implements Runnable {

    private static final String IDEA_DIRECTORY = ".idea";
    private static final String GIT_DIRECTORY = ".git";
    private static final String TARGET_DIRECTORY = "target/";
    private static final String FILE_CHANGE_INDICATOR = "~";
    private static final long SLEEP_DURATION_MS = 5000L;

    private final DaemonServer daemonServer;
    private final RepositoryPathProvider repositoryPathProvider;
    private final Map<WatchKey, Path> watchKeys = new HashMap<>();
    private final ArrayList<Path> filesToUpdate = new ArrayList<>();

    private Thread watchThread;
    private Future<?> watchUpdateTask;
    private WatchService watchService;

    private final Executor taskExecutor = Executors.newSingleThreadExecutor();

    public FileWatcher(DaemonServer daemonServer, RepositoryPathProvider repositoryPathProvider) {
        this.daemonServer = daemonServer;
        this.repositoryPathProvider = repositoryPathProvider;
    }


    /**
     * Watches the repository directory and its subdirectories for file events (creation, deletion, modification).
     *
     * @throws IOException if an I/O error occurs while watching the directory
     */
    public void watch() throws IOException {
        if (watchService != null) {
            watchThread.interrupt();
            watchService.close();
        }

        Path path = repositoryPathProvider.repositoryDirectory().toAbsolutePath();
        watchService = path.getFileSystem().newWatchService();

        path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

        Files.walk(path)
             .filter(Files::isDirectory)
             .filter(dir -> !dir.toString().contains(IDEA_DIRECTORY))
             .filter(dir -> !dir.toString().contains(GIT_DIRECTORY))
             .filter(dir -> !dir.toString().contains(TARGET_DIRECTORY))
             .forEach(this::registerDirectory);

        watchThread = new Thread(this);
        watchThread.start();
    }

    private void registerDirectory(Path directory) {
        WatchKey key = null;
        try {
            key = directory.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        watchKeys.put(key, directory);
        System.out.println("Registered directory: " + directory);
    }


    /**
     * Runs the file watcher in an infinite loop to listen for file events.
     * This method will keep running until it is interrupted or an error occurs.
     */
    @Override
    public void run() {
        while (true) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException ex) {
                System.out.println("Directory watching interrupted");
                return;
            }

            processKeyEvents(key);

            if (!key.reset()) {
                break;
            }
        }
    }

    private void processKeyEvents(WatchKey key) {
        key.pollEvents().forEach(event -> processEvent(key, event));
    }

    private void processEvent(WatchKey key, WatchEvent<?> event) {
        WatchEvent.Kind<?> kind = event.kind();
        WatchEvent<Path> ev = (WatchEvent<Path>) event;
        Path fileName = fetchFilePath(key, ev);
        String fileNameString = fileName.toString();

        if (isUnwantedFileChange(fileNameString)) {
            return;
        }

        System.out.println(kind.name() + ": " + fileName);

        if (Files.isDirectory(fileName)) {
            if (kind == ENTRY_CREATE) {
                registerDirectory(fileName);
            }
            return;
        }

        filesToUpdate.add(fileName);

        if (watchUpdateTask == null || watchUpdateTask.isDone()) {
            initFileUpdate();
        }
    }

    private Path fetchFilePath(WatchKey key, WatchEvent<Path> ev) {
        Path fileName = ev.context();
        return watchKeys.get(key).resolve(fileName);
    }

    private boolean isUnwantedFileChange(String fileName) {
        return fileName.endsWith(FILE_CHANGE_INDICATOR) || fileName.contains(
                IDEA_DIRECTORY) || fileName.contains(GIT_DIRECTORY) || fileName.contains(
                TARGET_DIRECTORY);
    }

    private void initFileUpdate() {
        watchUpdateTask = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(SLEEP_DURATION_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            List<Path> paths = (List<Path>) filesToUpdate.clone();
            filesToUpdate.clear();
            paths.stream().distinct().forEach(daemonServer::updateFile);
        }, taskExecutor);
    }
}
