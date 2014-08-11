package de.tuberlin.dima.schubotz.common.mappers;

import de.tuberlin.dima.schubotz.common.types.OutputSimpleTuple;
import de.tuberlin.dima.schubotz.fse.types.ResultTuple;
import eu.stratosphere.api.java.functions.GroupReduceFunction;
import eu.stratosphere.util.Collector;

import java.util.Iterator;

/**
 * Adds rank and runtag. Outputs 1000 results per query.
 */
@SuppressWarnings("serial")
public class OutputSimple extends GroupReduceFunction<ResultTuple, OutputSimpleTuple> {
	/**
	 * See {@link de.tuberlin.dima.schubotz.wiki.WikiProgram#MaxResultsPerQuery}
	 */
	private final int queryLimit;
	/**
	 * See {@link de.tuberlin.dima.schubotz.wiki.WikiProgram#RUNTAG} 
	 */
	private final String runtag;
	
	/**
	 * @param queryLimit {@link de.tuberlin.dima.schubotz.wiki.WikiProgram#MaxResultsPerQuery} passed in for serializability
	 * @param runtag {@link de.tuberlin.dima.schubotz.wiki.WikiProgram#RUNTAG} passed in for serializability
	 */
	@SuppressWarnings("hiding")
	public OutputSimple(int queryLimit, String runtag) {
		this.queryLimit = queryLimit;
		this.runtag = runtag;
	}

    /**
     * takes in {@link ResultTuple} per query, maps to {@link OutputSimple#queryLimit} number of
     * {@link OutputSimpleTuple}OutputSimpleTuple
     * @param in
     * @param out
     */
	@Override
	public void reduce(Iterator<ResultTuple> in, Collector<OutputSimpleTuple> out) {
		int current = 0;
		// for each element in group
		while(in.hasNext() && current < queryLimit) {
			final ResultTuple curTup = in.next();
			out.collect(new OutputSimpleTuple(curTup.f0,curTup.f1,current+1,curTup.f2,runtag));
			current++;
		}
	}
}
