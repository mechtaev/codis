package sg.edu.nus.comp.codis.ast;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Created by Sergey Mechtaev on 30/4/2016.
 */
public class BVType implements Type {
    private int size;

    public int getSize() {
        return size;
    }

    public BVType(int size) {
        this.size = size;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof BVType))
            return false;
        if (obj == this)
            return true;

        BVType rhs = (BVType) obj;
        return new EqualsBuilder().
                append(size, rhs.size).
                isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).
                append(size).
                toHashCode();
    }

    @Override
    public String toString() {
        return "BV" + size;
    }

}
