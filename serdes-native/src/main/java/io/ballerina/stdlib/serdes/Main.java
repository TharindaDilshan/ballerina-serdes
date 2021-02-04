package io.ballerina.stdlib.serdes;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;

/**
 * Main.
 *
 */
public class Main {
    public static BString generateSchema(BString name) {
        return StringUtils.fromString("Hello");
    }
}
