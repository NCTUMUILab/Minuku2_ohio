package edu.ohio.minuku.Utilities;

import org.javatuples.Tuple;

/**
 * Created by Lawrence on 2018/3/25.
 */

public class TupleHelper {

    public static String toPythonTuple(Tuple tuple){

        String pythontuple = "("+tuple.toString().replace("[","").replace("]","")+")";

        return pythontuple;
    }

}
