/**
 * 
 */
package org.etagate.helper;

import java.util.Collection;
import java.util.Map;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * @author 瞿建军       Email: troopson@163.com
 * 2017年3月24日
 */
public class S {

	@SuppressWarnings("rawtypes")
	public static String getString(Map row, String key){
		Object o = row.get(key);
		if(o==null)
			return null;
		return o.toString();
	}
	
	public static boolean isNotBlank(String s){
		return !isBlank(s);
	}
	
	
	public static boolean isBlank(String s){
		if(s==null || s.length()==0)
			return true;
		else
			return false;
	}
	
	public static int getInt(JsonObject j,String key, int defaultV){
		String s = j.getString(key);
		if(isBlank(s))
			return defaultV;
		return Integer.parseInt(s);
	}
	
	
	@SuppressWarnings("rawtypes")
	public static JsonArray toJsonArray(Map row, String[] fields){
		JsonArray j = new JsonArray();
		for(String s: fields){
			Object v = row.get(s);
			if(v==null)
				j.addNull();
			else
				j.add(v);			
		}
		return j;		
	}
	
	@SuppressWarnings("rawtypes")
	public static JsonArray toJsonArray(Map row, String fields){
//		System.out.println(row+"   "+fields);
		String[] fieldsArray = fields.split(",");
		return toJsonArray(row, fieldsArray);
	}
	
	public static JsonObject copy(JsonObject json, String fields){
		String[] fieldsArray = fields.split(",");
		JsonObject j = new JsonObject();
		for(String s: fieldsArray){
			Object v = json.getValue(s);
			if(v!=null)
				j.put(s, v);			
		}
		return j;
	}
	
	
	public static String join(Collection<String> c , String sep){
		if(c==null || c.isEmpty())
			return null;
		
		StringBuilder r=new StringBuilder();
		c.forEach(s->{
			if(r.length()>0)
				r.append(sep);
			r.append(s);
		});
		return r.toString();
	}
}
