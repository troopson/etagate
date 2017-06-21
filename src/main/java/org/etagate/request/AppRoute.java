/**
 * 
 */
package org.etagate.request;

import java.util.Set;

import org.etagate.app.AppContain;
import org.etagate.app.App;
import org.etagate.helper.S;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;

/**
 * @author 瞿建军       Email: troopson@163.com
 * 2017年5月17日
 */
public class AppRoute {
	
	
	public static void addAppRoute(AppContain appInfo, WebClient webclient, Router router){		
		
		//1. 是否要加auth router
						
		//给每个app加上一个默认的匹配路径，app名作为第一级路径名称
		appInfo.foreach((k,json)->{			
			
			App appobj = appInfo.getAppInfo(k);
			if(appobj==null)
				return;
			
			//所有的app，都加载一个以name为contextPath的默认路径
			addRoute(webclient,router,appobj, "/"+k+"/*");

			//加载route表里面定义的路径匹配
			Set<String> routepath = appobj.getRoutePath();
			if(routepath==null)
				return;
			routepath.forEach( s ->{
				addRoute(webclient,router,appobj,s);
			});			
			
		});			
	
		
	}
	
	
	private static void addRoute(WebClient client, Router router, App appobj, String urlpatten){

		
		if(S.isBlank(urlpatten))
			return;		
		
		//System.out.println("build route "+ urlpatten+"  "+appName+"   "+appobj.toString());
		
		RequestHandler appHandler = new RequestHandler(client ,appobj);
		
		router.route(urlpatten).handler(appHandler);
		
		
	}


	
}
