package vsue.faults;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
@Retention(RetentionPolicy.RUNTIME)
public @interface VSRPCSemantic {
    VSRPCSemanticType value();
}
