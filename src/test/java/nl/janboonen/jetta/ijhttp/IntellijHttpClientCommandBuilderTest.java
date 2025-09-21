package nl.janboonen.jetta.ijhttp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntellijHttpClientCommandBuilderTest {

    @Test
    void includesEnvironmentFlagAndValue_whenEnvironmentIsProvided() {
        var annotation = Fixtures.DefaultIntelliJHttpClientIT.class.getAnnotation(IntellijHttpClientFile.class);

        var builder = new IntellijHttpClientCommandBuilder();
        String[] cmd = builder.generate(annotation, null, "test.http", 8080);

        assertThat(cmd)
                .contains("--env", "FOO=BAR");
    }

    @Test
    void includesEnvFileAndReferencesWorkdir_whenEnvironmentFileIsProvided() {
        var annotation = Fixtures.IntellijHttpClientWithEnvFileIT.class.getAnnotation(IntellijHttpClientFile.class);

        var container = new org.testcontainers.containers.GenericContainer<>("alpine:3.18");
        var builder = new IntellijHttpClientCommandBuilder();
        String[] cmd = builder.generate(annotation, container, "test.http", 2222);

        assertThat(cmd).contains("--env-file");
        assertThat(java.util.Arrays.asList(cmd))
                .anySatisfy(part -> assertThat(part)
                        .startsWith("/workdir/")
                        .endsWith("http-client.env.json"));
    }

    @Test
    void includesPrivateEnvFileAndReferencesWorkdir_whenPrivateEnvironmentFileIsProvided() {
        var annotation = Fixtures.IntellijHttpClientWithPrivateEnvIT.class.getAnnotation(IntellijHttpClientFile.class);

        var container = new org.testcontainers.containers.GenericContainer<>("alpine:3.18");
        var builder = new IntellijHttpClientCommandBuilder();
        String[] cmd = builder.generate(annotation, container, "test.http", 1111);

        assertThat(cmd).contains("--private-env-file");
        assertThat(java.util.Arrays.asList(cmd))
                .anySatisfy(part -> assertThat(part)
                        .startsWith("/workdir/")
                        .endsWith("http-client.env.json"));
    }

    @Test
    void generatesBaseUrlContainingProvidedPort_and_includesReportAndHttpFilename() {
        var annotation = Fixtures.DefaultIntelliJHttpClientIT.class.getAnnotation(IntellijHttpClientFile.class);

        var builder = new IntellijHttpClientCommandBuilder();
        String[] cmd = builder.generate(annotation, null, "sample.http", 4242);

        assertThat(cmd)
                .contains("--env-variables", "baseUrl=host.testcontainers.internal:4242", "--report", "/workdir/", "-D", "/workdir/sample.http");
    }

    @Test
    void onlyBaseElementsPresent_whenNoOptionalEnvironmentValuesProvided() {
        @IntellijHttpClientFile(value = "unused")
        class MinimalIT extends IntellijHttpClientTestSupport {
            @Override
            public int getPort() {
                return 3333;
            }
        }

        var annotation = MinimalIT.class.getAnnotation(IntellijHttpClientFile.class);

        var builder = new IntellijHttpClientCommandBuilder();
        String[] cmd = builder.generate(annotation, null, "minimal.http", 3333);

        var parts = java.util.Arrays.asList(cmd);
        assertThat(parts)
                .doesNotContain("--env-file", "--private-env-file")
                .contains("--env-variables", "baseUrl=host.testcontainers.internal:3333", "/workdir/minimal.http");
    }

}
