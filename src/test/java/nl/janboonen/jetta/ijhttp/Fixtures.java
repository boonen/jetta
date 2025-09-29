package nl.janboonen.jetta.ijhttp;

final class Fixtures {

    static class UnannotatedIT extends IntellijHttpClientTestSupport {
        @Override
        public int getPort() { return 0; }
    }

    @IntellijHttpClientFile(value = "unused", environment = "FOO=BAR")
    static class DefaultIntelliJHttpClientIT extends IntellijHttpClientTestSupport {
    }

    @IntellijHttpClientFile(value = "unused", environment = "", privateEnvironmentFile = "src/test/resources/ijhttp/http-client.env.json")
    static class IntellijHttpClientWithPrivateEnvIT extends IntellijHttpClientTestSupport {
        @Override
        public int getPort() { return 1111; }
    }

    // new helper annotated class for environmentFile tests
    @IntellijHttpClientFile(value = "unused", environmentFile = "src/test/resources/ijhttp/http-client.env.json")
    static class IntellijHttpClientWithEnvFileIT extends IntellijHttpClientTestSupport {
        @Override
        public int getPort() {
            return 2222;
        }
    }

}
