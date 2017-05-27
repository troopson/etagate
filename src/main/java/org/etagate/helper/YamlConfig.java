/**
 * 
 */
package org.etagate.helper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;

import org.yaml.snakeyaml.Yaml;

import io.vertx.core.json.JsonObject;

/**
 * @author 瞿建军 Email: jianjun.qu@istuary.com 2017年5月8日
 */
public class YamlConfig {

	private JsonObject items=new JsonObject();
	
	public YamlConfig(){
	}
	
	public YamlConfig(InputStream f){
		this.loadYaml(f);
	}
		
	public void clear(){
		items.clear();
	}
		
	public void loadYaml(URL uri){
		try {
			InputStream is = uri.openStream();
			this.loadYaml(is);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void loadYaml(InputStream f){
		
		Yaml yaml = new Yaml();
		HashMap config = (HashMap) yaml.loadAs(f,HashMap.class);
		
		config.forEach( (t,u)-> items.put(t.toString(), u));
		
	}
	
	
	
	public Object get(String key){
		Object v = items.getValue(key);
		if(v!=null)
			return v;
		
		if(key.indexOf('.')>0){
			String[] spkey = key.split("\\.");
			return this.getLevel(items,spkey, 0);
		}
		return v;
	}
	
	public String getString(String key){
		Object o = this.get(key);
		if(o==null)
			return null;
		return o.toString();
	}
	
	public int getInt(String key,int def){
		String s = this.getString(key);
		if(s==null)
			return def;
		
		return Integer.parseInt(s);		
	}
	
	public boolean getBoolean(String key,boolean def){
		String s = this.getString(key);
		if(s==null)
			return def;
		return Boolean.parseBoolean(s);
	}
	
	public Properties getProperties(String key){
		Properties ps =new Properties();
		Object o = this.get(key);
		if(o instanceof JsonObject)
			((JsonObject) o).forEach( entry ->  ps.setProperty(entry.getKey(), entry.getValue()+"") );
		
		return ps;
	}

	
	private Object getLevel(JsonObject cont, String[] spkey,int i){
		Object v = cont.getValue(spkey[i]);
		if(i>=spkey.length)
			return v;
		
		if(v==null || !(v instanceof JsonObject) )
			return v;
		
		return getLevel((JsonObject)v,spkey, i+1);
		
	}
	
	public String toString(){
		return this.items.toString();
	}


}
