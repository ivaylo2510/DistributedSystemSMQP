package dslab.util.grading.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for GitHub Classroom Workflow Generator.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GitHubClassroomGrading {

    /**
     * Name of the test. If not set, the generator will use the test methods name as testName
     */
    String testName() default "";

    /**
     * Command to setup the test. Should be a valid shell command.
     */
    String setupCommand() default "";

    /**
     * Command to execute the test. Should be a valid shell command.
     */
    String command() default "";

    /**
     * Timeout for the test in minutes.
     */
    int timeout() default 1;

    /**
     * Score / points for the test.
     */
    int maxScore() default 0;

}
