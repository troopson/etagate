/**
 * 
 */
package org.etagate.app;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.etagate.helper.S;

/**
 * @author 瞿建军 Email: troopson@163.com 2017年4月12日
 */
public class AppContain {

	private  Map<String, App> app;
	
	
	public void addAppObject(App a){
		if(a==null)
			return;
		if(app==null)
			app = new HashMap<>();
		this.app.put(a.name, a);
	}

	public App getAppInfo(String appName) {
		if (app == null || S.isBlank(appName))
			return null;
		return app.get(appName);
	}

	public void foreach(BiConsumer<String, App> func) {
		if (app == null)
			return;
		app.forEach(func);
	}
	


}
