package sg.edu.nus.comp.codis;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;

/**
 * Created by Sergey Mechtaev on 1/4/2016.
 */
public class Main {

    public static void main(String[] args) {
        Context ctx = new Context();
        Solver z3 = ctx.mkSolver();

        ArithExpr x = ctx.mkIntConst("x");
        ArithExpr y = ctx.mkIntConst("y");

        z3.add(ctx.mkAnd(ctx.mkLt(ctx.mkSub(x, y), ctx.mkInt(10))));

        z3.check();
        System.out.println(z3.getModel().eval(x, false));
        System.out.println(z3.getModel().eval(y, false));
    }
}
