package nl.janboonen.jetta.testcontainers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ExitCodeWaitStrategyTest {

    private WaitStrategyTarget target;
    private InspectContainerResponse.ContainerState state;
    private DockerClientFactory instance;

    @BeforeEach
    void setUp() {
        target = mock(WaitStrategyTarget.class);
        final DockerClient client = mock(DockerClient.class);
        final InspectContainerCmd inspectCmd = mock(InspectContainerCmd.class);
        final InspectContainerResponse response = mock(InspectContainerResponse.class);
        state = mock(InspectContainerResponse.ContainerState.class);
        instance = mock(DockerClientFactory.class);

        when(target.getContainerId()).thenReturn("container123");
        when(client.inspectContainerCmd("container123")).thenReturn(inspectCmd);
        when(inspectCmd.exec()).thenReturn(response);
        when(response.getState()).thenReturn(state);
        when(instance.client()).thenReturn(client);
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 42, 255})
    void containerExitsWithVariousAllowedCodes(long exitCode) {
        ExitCodeWaitStrategy strategy = new ExitCodeWaitStrategy().withAllowedExitCodes(1, 2, 42, 255);
        when(state.getRunning()).thenReturn(false);
        when(state.getExitCodeLong()).thenReturn(exitCode);

        executeWithMockedFactory(() -> strategy.waitUntilReady(target));
    }

    @ParameterizedTest
    @ValueSource(longs = {3, 127, 255})
    void containerExitsWithDisallowedCodesThrowsException(long exitCode) {
        ExitCodeWaitStrategy strategy = new ExitCodeWaitStrategy();
        when(state.getRunning()).thenReturn(false);
        when(state.getExitCodeLong()).thenReturn(exitCode);

        executeWithMockedFactory(() ->
                assertThrows(ContainerLaunchException.class, () -> strategy.waitUntilReady(target))
        );
    }

    @Test
    void containerNeverStopsRunningAndNoMaxTimeThrowsInterruptedException() {
        ExitCodeWaitStrategy strategy = new ExitCodeWaitStrategy().withPollInterval(Duration.ofMillis(1));
        when(state.getRunning()).thenReturn(true);
        when(state.getExitCodeLong()).thenReturn(null);

        Thread testThread = Thread.currentThread();
        Thread interrupter = new Thread(() -> {
            await().atMost(Duration.ofMillis(50));
            testThread.interrupt();
        });
        interrupter.start();

        executeWithMockedFactory(() ->
                assertThrows(ContainerLaunchException.class, () -> strategy.waitUntilReady(target))
        );
    }

    @Test
    void containerExitsWithNullExitCodeAndNotRunningWaitsForValidState() {
        ExitCodeWaitStrategy strategy = new ExitCodeWaitStrategy().withPollInterval(Duration.ofMillis(10));
        when(state.getRunning()).thenReturn(false, false, false);
        when(state.getExitCodeLong()).thenReturn(null, null, 0L);

        executeWithMockedFactory(() -> strategy.waitUntilReady(target));
    }

    @Test
    void containerStillRunningWithExitCodeWaitsUntilNotRunning() {
        ExitCodeWaitStrategy strategy = new ExitCodeWaitStrategy().withPollInterval(Duration.ofMillis(10));
        when(state.getRunning()).thenReturn(true, true, false);
        when(state.getExitCodeLong()).thenReturn(0L, 0L, 0L);

        executeWithMockedFactory(() -> strategy.waitUntilReady(target));
    }

    @Test
    void containerExceedsMaxRunningTimeThrowsException() {
        ExitCodeWaitStrategy strategy = new ExitCodeWaitStrategy()
                .withMaxRunningTime(Duration.ofMillis(50))
                .withPollInterval(Duration.ofMillis(5));
        when(state.getRunning()).thenReturn(true);
        when(state.getExitCodeLong()).thenReturn(null);

        executeWithMockedFactory(() ->
                assertThrows(ContainerLaunchException.class, () -> strategy.waitUntilReady(target))
        );
    }

    private void executeWithMockedFactory(Runnable test) {
        assertDoesNotThrow(() -> {
            try (MockedStatic<DockerClientFactory> factory = mockStatic(DockerClientFactory.class)) {
                factory.when(DockerClientFactory::instance).thenReturn(instance);
                test.run();
            }
        });
    }
}