/**
 * 
 */
package org.etagate.helper;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 瞿建军       Email: jianjun.qu@istuary.com
 * 2017年5月8日
 */
public class Args {
	
	private Map<String,String> argsMap;
	
	public Args(String[] args){
		this.argsMap=new HashMap<>();
		this.parse(args);
	}
	
	private void parse(String[] args){
		String lastKey=null;
		for(String s : args){
			if(s.startsWith("--") || s.startsWith("-D")){
				lastKey=s.substring(2);
			}else if(s.startsWith("-")){
				lastKey=s.substring(1);
			}else if(lastKey!=null){
				argsMap.put(lastKey, s);
				lastKey=null;
			}
		}
	}
	
	public String get(String key){
		return this.argsMap.get(key);
	}
	
	public String toString(){
		return this.argsMap==null?"":this.argsMap.toString();
	}

}
