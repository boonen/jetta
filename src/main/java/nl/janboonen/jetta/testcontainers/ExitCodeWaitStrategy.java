package nl.janboonen.jetta.testcontainers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ExitCodeWaitStrategy extends AbstractWaitStrategy {

    private final Set<Integer> allowedExitCodes = new HashSet<>(Collections.singleton(0));
    private Duration pollInterval = Duration.ofMillis(200);
    private Duration maxRunningTime = null; // null => unlimited

    public ExitCodeWaitStrategy withAllowedExitCodes(int... codes) {
        Arrays.stream(codes).forEach(allowedExitCodes::add);
        return this;
    }

    public ExitCodeWaitStrategy withPollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
        return this;
    }

    public ExitCodeWaitStrategy withMaxRunningTime(Duration maxRunningTime) {
        this.maxRunningTime = maxRunningTime;
        return this;
    }

    @Override
    protected void waitUntilReady() {
        final WaitStrategyTarget target = this.waitStrategyTarget;
        final DockerClient client = DockerClientFactory.instance().client();
        final String containerId = target.getContainerId();

        final long startNanos = System.nanoTime();

        while (true) {
            long elapsedNanos = System.nanoTime() - startNanos;

            // Fail if max running time exceeded
            if (maxRunningTime != null && elapsedNanos > maxRunningTime.toNanos()) {
                throw new ContainerLaunchException(
                        "Container " + containerId + " exceeded max running time of " + maxRunningTime);
            }

            InspectContainerResponse.ContainerState state =
                    client.inspectContainerCmd(containerId).exec().getState();

            boolean running = Boolean.TRUE.equals(state.getRunning());
            Integer exitCode = getExitCodeCompat(state);

            if (!running && exitCode != null) {
                if (!allowedExitCodes.contains(exitCode)) {
                    throw new ContainerLaunchException(
                            "Container exited with disallowed code " + exitCode +
                                    " (allowed: " + allowedExitCodes + ")");
                }
                // Success
                return;
            }

            try {
                Thread.sleep(Math.max(1L, pollInterval.toMillis()));
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                throw new ContainerLaunchException("Interrupted while waiting for container to exit");
            }
        }
    }

    private static Integer getExitCodeCompat(InspectContainerResponse.ContainerState state) {
        try {
            return extractExitCode(state);
        } catch (ReflectiveOperationException _) {
            return null;
        }
    }

    private static Integer extractExitCode(InspectContainerResponse.ContainerState state) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        try {
            return (Integer) state.getClass().getMethod("getExitCode").invoke(state);
        } catch (NoSuchMethodException _) {
            Long val = (Long) state.getClass().getMethod("getExitCodeLong").invoke(state);
            return val == null ? null : val.intValue();
        }
    }
}
