package edu.umich.verdict.relation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;

import edu.umich.verdict.VerdictContext;
import edu.umich.verdict.datatypes.SampleParam;
import edu.umich.verdict.datatypes.TableUniqueName;
import edu.umich.verdict.exceptions.VerdictException;
import edu.umich.verdict.relation.condition.Cond;
import edu.umich.verdict.relation.expr.ColNameExpr;
import edu.umich.verdict.relation.expr.Expr;

public class GroupedRelation extends ExactRelation {

    protected ExactRelation source;

    protected List<Expr> groupby;

    public GroupedRelation(VerdictContext vc, ExactRelation source, List<Expr> groupby) {
        super(vc);
        this.source = source;
        this.groupby = groupby;
        this.alias = source.alias;
    }

    public ExactRelation getSource() {
        return source;
    }

    public List<Expr> getGroupby() {
        return groupby;
    }

    @Override
    protected String getSourceName() {
        return getAlias();
    }

    /*
     * Approx
     */

    @Override
    public ApproxRelation approx() throws VerdictException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected ApproxRelation approxWith(Map<TableUniqueName, SampleParam> replace) {
        ApproxRelation a = new ApproxGroupedRelation(vc, source.approxWith(replace), groupby);
        a.setAlias(getAlias());
        return a;
    }

    /**
     * We view no actual groupby-aggregate operations are performed until encountering AggregatedRelation.
     * As this relation being a source of AggregatedRelation, propagating the sample information without
     * much modification is convenient.
     */
    @Override
    protected List<ApproxRelation> nBestSamples(Expr elem, int n) throws VerdictException {
        List<ApproxRelation> ofSources = source.nBestSamples(elem, n);
        List<ApproxRelation> grouped = new ArrayList<ApproxRelation>();
        for (ApproxRelation a : ofSources) {
            grouped.add(new ApproxGroupedRelation(vc, a, groupby));
        }
        return grouped;
    }

    /*
     * Sql
     */

    @Override
    public String toSql() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * ");

        Pair<List<Expr>, ExactRelation> groupsAndNextR = allPrecedingGroupbys(this.source);
        String gsql = Joiner.on(", ").join(groupsAndNextR.getLeft());

        Pair<Optional<Cond>, ExactRelation> filtersAndNextR = allPrecedingFilters(groupsAndNextR.getRight());
        String csql = (filtersAndNextR.getLeft().isPresent())? filtersAndNextR.getLeft().get().toString() : "";

        sql.append(String.format(" FROM %s", sourceExpr(source)));
        if (csql.length() > 0) { sql.append(" WHERE "); sql.append(csql); }
        if (gsql.length() > 0) { sql.append(" GROUP BY "); sql.append(gsql); }
        return sql.toString();
    }

    //	@Override
    //	public List<SelectElem> getSelectList() {
    //		return source.getSelectList();
    //	}

    @Override
    public List<ColNameExpr> accumulateSamplingProbColumns() {
        return source.accumulateSamplingProbColumns();
    }

    @Override
    protected String toStringWithIndent(String indent) {
        StringBuilder s = new StringBuilder(1000);
        s.append(indent);
        s.append(String.format("%s(%s) [%s]\n", this.getClass().getSimpleName(), getAlias(), Joiner.on(", ").join(groupby)));
        s.append(source.toStringWithIndent(indent + "  "));
        return s.toString();
    }

    //	@Override
    //	public List<SelectElem> getSelectList() {
    //		return source.getSelectList();
    //	}

    @Override
    public ColNameExpr partitionColumn() {
        ColNameExpr col = source.partitionColumn();
        //		col.setTab(getAliasName());
        return col;
    }

    //    @Override
    //    public Expr distinctCountPartitionColumn() {
    //        return source.distinctCountPartitionColumn();
    //    }

}
