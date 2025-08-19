package nl.janboonen.jetta.ijhttp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface IntellijHttpClientFile {

    /**
     * Path of the IntelliJ HTTP client file that contains the HTTP requests and tests. The path is relative to the
     * project root directory.
     *
     * @return relative path to the HTTP client file
     */
    String value();

    /**
     * Path to the environment file.
     *
     * @return path to environment file.
     */
    String environmentFile() default "";

    /**
     * Path to the private environment file.
     *
     * @return path to private environment file.
     */
    String privateEnvironmentFile() default "";

    /**
     * The environment to use.
     *
     * @return the environment.
     */
    String environment() default "dev";

    /**
     * The port of the service under test.
     *
     * @return the port.
     */
    int servicePort() default 8080;

    /**
     * The maximum running time of the test in seconds.
     *
     * @return the maximum running time in seconds.
     */
    long maxRunningTimeInSeconds() default 60;

    /**
     * The docker image to use.
     *
     * @return the docker image.
     */
    String dockerImage() default "jetbrains/intellij-http-client:latest";

}
