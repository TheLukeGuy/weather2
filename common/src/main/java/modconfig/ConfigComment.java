package modconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be applied to a value in any class that implements IConfigCategory to associate
 * a comment/description with a particular value in a config file.
 * <p>
 * Example usage:
 *
 * @author Cojomax99 (cojomax99@gmail.com)
 * @ConfigComment("Should chunks get cached in the overworld only?")
 * public static boolean chunkCacheOverworldOnly = false;
 * <p>
 * Or if you'd rather not have a comment, simply don't use this annotation!
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigComment {
    /**
     * The comment/description associated with the variable it is annotating
     *
     * @return The comment/description to be associated with a variable
     */
    String[] value() default "";
}
