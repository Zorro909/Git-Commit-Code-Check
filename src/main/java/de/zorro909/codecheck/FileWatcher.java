package de.zorro909.codecheck;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;


@RequiresCliOption("--watch")
@Requires(beans = DaemonServer.class)
@Singleton
public class FileWatcher implements Runnable {

    private final DaemonServer daemonServer;
    private final RepositoryPathProvider repositoryPathProvider;
    private final Map<WatchKey, Path> watchKeys = new HashMap<>();

    private Thread watchThread;
    private WatchService watchService;

    public FileWatcher(DaemonServer daemonServer, RepositoryPathProvider repositoryPathProvider) {
        this.daemonServer = daemonServer;
        this.repositoryPathProvider = repositoryPathProvider;
    }

    /**
     * Watches for File changes in the root Repository Directory
     * @throws IOException if it can't watch a directory
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
             .filter(dir -> !dir.toString().endsWith(".idea"))
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


    @Override
    public void run() {
        while (true) {
            WatchKey key;
            try {
                key = watchService.take(); // Wait for a watch key to be available
            } catch (InterruptedException ex) {
                System.out.println("Directory watching interrupted");
                return;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path fileName = ev.context();

                if (fileName.toString().endsWith("~")) {
                    continue;
                }
                if (fileName.toString().contains(".idea/")) {
                    continue;
                }

                System.out.println(kind.name() + ": " + fileName);

                // Implement specific actions based on event type
                if (kind == ENTRY_CREATE) {
                    if (Files.isDirectory(fileName)) {
                        try {
                            fileName.register(watchService, ENTRY_CREATE, ENTRY_DELETE,
                                              ENTRY_MODIFY);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        continue;
                    }
                }
                if (Files.isDirectory(fileName)) {
                    continue;
                }

                fileName = watchKeys.get(key).resolve(fileName);
                daemonServer.updateFile(fileName);
            }

            // Reset the key -- this step is critical to receive further watch events
            boolean valid = key.reset();//Test
            if (!valid) {
                break;
            }
        }
    }
}
