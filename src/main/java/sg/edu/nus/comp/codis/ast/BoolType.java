package sg.edu.nus.comp.codis.ast;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public class BoolType implements Type {
    public static final BoolType TYPE = new BoolType();

    private BoolType() {}

    @Override
    public int hashCode() {
        return 0;
    }
}
