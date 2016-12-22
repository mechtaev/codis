package sg.edu.nus.comp.codis.ast;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public class IntType implements Type {
    public static final IntType TYPE = new IntType();

    private IntType() {}

    @Override
    public int hashCode() {
        return 0;
    }
}
