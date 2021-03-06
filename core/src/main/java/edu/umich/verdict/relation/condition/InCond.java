package edu.umich.verdict.relation.condition;

import java.util.ArrayList;
import java.util.List;

import edu.umich.verdict.VerdictContext;
import edu.umich.verdict.parser.VerdictSQLParser;
import edu.umich.verdict.parser.VerdictSQLParser.ExpressionContext;
import edu.umich.verdict.relation.expr.Expr;
import edu.umich.verdict.util.VerdictLogger;

public class InCond extends Cond {
    
    Expr left;
    
    boolean not;
    
    List<Expr> expressionList;
    
    public InCond(Expr left, boolean not, List<Expr> expressionList) {
        this.left = left;
        this.not = not;
        this.expressionList = expressionList;
    }
    
    public Expr getLeft() {
        return left;
    }

    public void setLeft(Expr left) {
        this.left = left;
    }

    public boolean isNot() {
        return not;
    }

    public void setNot(boolean not) {
        this.not = not;
    }

    public List<Expr> getExpressionList() {
        return expressionList;
    }

    public void setExpressionList(List<Expr> expressionList) {
        this.expressionList = expressionList;
    }

    public static InCond from(VerdictContext vc, VerdictSQLParser.In_predicateContext ctx) {
        if (ctx.subquery() != null) {
            VerdictLogger.error("Verdict currently does not support IN + subquery condition.");
        }
        
        Expr left = Expr.from(vc, ctx.expression());
        boolean not = (ctx.NOT() != null)? true : false;
        List<Expr> expressionList = new ArrayList<Expr>();
        
        for (ExpressionContext ectx : ctx.expression_list().expression()) {
            expressionList.add(Expr.from(vc, ectx));
        }
        
        return new InCond(left, not, expressionList);
    }

    @Override
    public Cond withTableSubstituted(String newTab) {
        List<Expr> newExpressions = new ArrayList<Expr>();
        for (Expr expr : expressionList) {
            newExpressions.add(expr.withTableSubstituted(newTab));
        }
        return this;
    }
    
    @Override
    public String toString() {
        return toSql();
    }

    @Override
    public String toSql() {
        StringBuilder sql = new StringBuilder();
        sql.append(left.toSql());
        if (not) sql.append(" NOT");
        sql.append(" IN (");
        
        for (int i = 0; i < expressionList.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(expressionList.get(i).toSql());
        }
        
        sql.append(")");
        return sql.toString();
    }

    @Override
    public boolean equals(Cond o) {
        if (o instanceof InCond) {
            return getLeft().equals(((InCond) o).getLeft())
                && (isNot() == ((InCond) o).isNot())
                && getExpressionList().equals(((InCond) o).getExpressionList());
        }
        return false;
    }

}
