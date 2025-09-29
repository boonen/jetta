package nl.janboonen.jetta.ijhttp;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

final class IntellijHttpClientCommandBuilder {

    private static final String IJHTTP_WORKDIR = "/workdir/";
    private static final String DOCKERHOST_HOSTNAME = "host.testcontainers.internal";

    public String[] generate(IntellijHttpClientFile annotation, GenericContainer<?> ijhttp, String httpFileName, int port) {
        List<String> command = new ArrayList<>();

        if (!annotation.environmentFile().isBlank()) {
            var envFile = Paths.get(annotation.environmentFile()).toAbsolutePath();
            var envFileName = envFile.getFileName().toString();
            ijhttp.withCopyFileToContainer(
                    MountableFile.forHostPath(envFile),
                    IJHTTP_WORKDIR + envFileName
            );
            command.add("--env-file");
            command.add(IJHTTP_WORKDIR + envFileName);
        }

        if (!annotation.privateEnvironmentFile().isBlank()) {
            var privateEnvFile = Paths.get(annotation.privateEnvironmentFile()).toAbsolutePath();
            var privateEnvFileName = privateEnvFile.getFileName().toString();
            ijhttp.withCopyFileToContainer(
                    MountableFile.forHostPath(privateEnvFile),
                    IJHTTP_WORKDIR + privateEnvFileName
            );
            command.add("--private-env-file");
            command.add(IJHTTP_WORKDIR + privateEnvFileName);
        }

        if (!annotation.environment().isBlank()) {
            command.add("--env");
            command.add(annotation.environment());
        }

        command.add("--env-variables");
        command.add("baseUrl=" + DOCKERHOST_HOSTNAME + ":" + port);
        command.add("--report");
        command.add(IJHTTP_WORKDIR);
        command.add("-D");
        command.add(IJHTTP_WORKDIR + httpFileName);

        return command.toArray(new String[0]);
    }
}
