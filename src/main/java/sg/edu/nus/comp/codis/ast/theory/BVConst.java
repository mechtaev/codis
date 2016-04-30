package sg.edu.nus.comp.codis.ast.theory;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import sg.edu.nus.comp.codis.ast.BVType;
import sg.edu.nus.comp.codis.ast.BottomUpVisitor;
import sg.edu.nus.comp.codis.ast.Constant;
import sg.edu.nus.comp.codis.ast.TopDownVisitor;

/**
 * Created by Sergey Mechtaev on 30/4/2016.
 */
public class BVConst extends Constant {

    private long value;

    private BVType type;

    private BVConst(long value, int size) {
        this.value = value;
        this.type = new BVType(size);
    }

    public static BVConst ofLong(long value, int size) {
        return new BVConst(value, size);
    }

    public static BVConst ofBoolean(boolean value, int size) {
        return new BVConst(value ? 1 : 0, size);
    }

    public BVType getType() {
        return type;
    }

    public long getLong() {
        return value;
    }

    public boolean getBoolean() {
        return value != 0;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof BVConst))
            return false;
        if (obj == this)
            return true;

        BVConst rhs = (BVConst) obj;
        return new EqualsBuilder().
                append(type, rhs.type).
                append(value, rhs.value).
                isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).
                append(type).
                append(value).
                toHashCode();
    }

    @Override
    public void accept(BottomUpVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(TopDownVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }

}
