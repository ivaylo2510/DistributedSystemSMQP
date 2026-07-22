package dslab.util.grading;

import dslab.util.grading.annotations.GitHubClassroomGrading;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.reflect.Method;

public class LocalGradingExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback, AfterAllCallback {

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        Method method = context.getRequiredTestMethod();
        if (method.isAnnotationPresent(GitHubClassroomGrading.class)) {
            String methodId = getMethodId(method);
            String className = method.getDeclaringClass().getSimpleName(); // Simple class name
            String methodName = method.getName();
            int score = method.getAnnotation(GitHubClassroomGrading.class).maxScore();

            GradingData.getInstance().addTestMethod(methodId, className, methodName, score);
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        GitHubClassroomGrading gradedTest = getGradedTestAnnotation(context);
        if (gradedTest != null) {
            Method method = context.getRequiredTestMethod();
            String methodId = getMethodId(method);

            boolean testPassed = context.getExecutionException().isEmpty();

            if (!testPassed) {
                GradingData.getInstance().markTestMethodFailed(methodId);
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        GradingData.getInstance().outputSummary();
    }

    private GitHubClassroomGrading getGradedTestAnnotation(ExtensionContext context) {
        return context.getTestMethod()
                .flatMap(method -> AnnotationSupport.findAnnotation(method, GitHubClassroomGrading.class))
                .orElse(null);
    }

    private String getMethodId(Method method) {
        return method.getDeclaringClass().getName() + "." + method.getName();
    }

}
