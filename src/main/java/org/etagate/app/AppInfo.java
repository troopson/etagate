/**
 * 
 */
package org.etagate.app;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.etagate.helper.S;

/**
 * @author 瞿建军 Email: jianjun.qu@istuary.com 2017年4月12日
 */
public class AppInfo {

	private  Map<String, AppObject> app;
	
	
	public void addAppObject(AppObject a){
		if(a==null)
			return;
		if(app==null)
			app = new HashMap<>();
		this.app.put(a.name, a);
	}

	public AppObject getAppInfo(String appName) {
		if (app == null || S.isBlank(appName))
			return null;
		return app.get(appName);
	}

	public void foreach(BiConsumer<String, AppObject> func) {
		if (app == null)
			return;
		app.forEach(func);
	}
	


}
