package edu.ohio.minuku_2.model;

import java.util.Date;

import edu.ohio.minukucore.model.DataRecord;

/**
 * Created by Lawrence on 2017/6/5.
 */

public class CheckFamiliarOrNotDataRecord implements DataRecord {

    public long creationTime;


    /*familiar neighbor non-familiar*/
    private int home;
    private int neighbor;
    private int outside;
    private int staticornot;

    public CheckFamiliarOrNotDataRecord(){}

    public CheckFamiliarOrNotDataRecord(int home,int neighbor,int outside){
        this.creationTime = new Date().getTime();
        this.home = home;
        this.neighbor = neighbor;
        this.outside = outside;
    }

    public CheckFamiliarOrNotDataRecord(int staticornot,int home,int neighbor,int outside){
        this.creationTime = new Date().getTime();
        this.staticornot = staticornot;
        this.home = home;
        this.neighbor = neighbor;
        this.outside = outside;

    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    public int getHome(){return home;}

    public int getneighbor(){return neighbor;}

    public int getoutside(){return outside;}


}
