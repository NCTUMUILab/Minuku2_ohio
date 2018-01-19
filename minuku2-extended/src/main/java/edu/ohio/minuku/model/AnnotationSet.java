package edu.ohio.minuku.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AnnotationSet {

    //the id of the annotationSet for raw data is 0
	private int mId=0;
	private String mName="";
	private ArrayList<Annotation> mAnnotations;
	
	public AnnotationSet(){
		
	}
	
	public int getId(){
		return mId;
	}
	
	public void setId(int id){
		mId = id; 
	}
	
	public String getName () {
		return mName;
	}
	
	public void setName(String name) {
		mName = name;		
	}
	
	public ArrayList<Annotation> getAnnotations() {
		return mAnnotations;
	}
	
	public void setAnnotations(ArrayList<Annotation> annotations) {
		
		mAnnotations = annotations;
	}

    public void addAnnotation(Annotation annotation){

        if (mAnnotations==null){
            mAnnotations = new ArrayList<Annotation>();
        }

        mAnnotations.add(annotation);

    }

	public JSONObject toJSONObject(){

		JSONObject obj  = new JSONObject();

		try{

			if (mAnnotations!=null && mAnnotations.size()>0){

				obj.put("", mId);
				obj.put("", (Object) getAnnotationsInJSONArray());

			}

		}catch(JSONException e){

		}
		return obj;


	}

	public JSONArray getAnnotationsInJSONArray() {

		JSONArray array = new JSONArray() ;

		//get all the annotaitons and put their JSONObject format into the array
		for (int i=0; i<mAnnotations.size(); i++){
//			array.put(mAnnotations.get(i).toJSONObject());
		}

		return array;

	}

    /*public JSONObject toJSONObject(){

        JSONObject obj  = new JSONObject();

        try{

            if (mAnnotations!=null && mAnnotations.size()>0){

                if (!mName.equals(""))
                    obj.put(RecordingAndAnnotateManager.ANNOTATION_PROPERTIES_NAME, mName);
                obj.put(RecordingAndAnnotateManager.ANNOTATION_PROPERTIES_ID, mId);
                obj.put(RecordingAndAnnotateManager.ANNOTATION_PROPERTIES_ANNOTATION, (Object) getAnnotationsInJSONArray());

            }

        }catch(JSONException e){

        }
        return obj;


    }*/

    /*public JSONArray getAnnotationsInJSONArray() {

        JSONArray array = new JSONArray() ;

        //get all the annotaitons and put their JSONObject format into the array
        for (int i=0; i<mAnnotations.size(); i++){
            array.put(mAnnotations.get(i).toJSONObject());
        }

        return array;

    }*/



    @Override
    public String toString(){

        String s = null;




        return s;

    }
	
	
}
