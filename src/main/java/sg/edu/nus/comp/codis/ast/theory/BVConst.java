package sg.edu.nus.comp.codis.ast.theory;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import sg.edu.nus.comp.codis.ast.*;

import java.math.BigInteger;

/**
 * Created by Sergey Mechtaev on 30/4/2016.
 */
public class BVConst extends Constant {

    private BigInteger value;

    private BVType type;

    public BVConst(BigInteger value, int size) {
        this.value = value;
        this.type = new BVType(size);
    }

    public static BVConst ofLong(long value, int size) {
        return new BVConst(BigInteger.valueOf(value), size);
    }

    public static BVConst ofBoolean(boolean value, int size) {
        return new BVConst(BigInteger.valueOf(value ? 1 : 0), size);
    }

    /**
     * From HEX
     */
    public static BVConst ofHEXString(String repr, int size) {
        return new BVConst(new BigInteger(repr, 16), size);
    }

    public BVType getType() {
        return type;
    }

    public BigInteger getValue() {
        return value;
    }

    public long getLong() {
        return value.longValue();
    }

    public String getString() {
        return value.toString();
    }

    public boolean getBoolean() {
        return !value.equals(BigInteger.ZERO);
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
    public void accept(BottomUpMemoVisitor visitor) {
        if (visitor.alreadyVisited(this)) {
            visitor.visitAgain(this);
        } else {
            visitor.visit(this);
        }
    }


    @Override
    public String toString() {
        return value.toString();
    }

}
