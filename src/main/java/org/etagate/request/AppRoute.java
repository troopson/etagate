/**
 * 
 */
package org.etagate.request;

import java.util.Set;

import org.etagate.GateVerticle;
import org.etagate.app.AppInfo;
import org.etagate.app.AppObject;
import org.etagate.helper.S;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * @author 瞿建军       Email: jianjun.qu@istuary.com
 * 2017年5月17日
 */
public class AppRoute {
	
	
	public static void addAppRoute(GateVerticle gvert){		
		
		//1. 是否要加auth router
						
		//给每个app加上一个默认的匹配路径，app名作为第一级路径名称
		AppInfo appinfo=gvert.getApp();
		appinfo.foreach((k,json)->{			
			
			AppObject appobj = appinfo.getAppInfo(k);
			if(appobj==null)
				return;
			
			//所有的app，都加载一个以name为contextPath的默认路径
			addRoute(gvert.getWebClient(),gvert.getRouter(),appobj, "/"+k+"/*");

			//加载route表里面定义的路径匹配
			Set<String> routepath = appobj.getRoutePath();
			if(routepath==null)
				return;
			routepath.stream().peek( s ->{
				addRoute(gvert.getWebClient(),gvert.getRouter(),appobj,s);
			});			
			
		});
				
		
		//如果配置了静态文件目录，那么在没有命中前面的route path时，可以直接映射到静态文件目录中去
		String webroot = gvert.getWebroot();
		if(webroot!=null){
			StaticHandler sta = StaticHandler.create(webroot);
			gvert.getRouter().route().handler(sta);
		}
		
	}
	
	
	private static void addRoute(WebClient client, Router router, AppObject appobj, String urlpatten){

		
		if(S.isBlank(urlpatten))
			return;		
		
		//System.out.println("build route "+ urlpatten+"  "+appName+"   "+appobj.toString());
		
		RequestHandler appHandler = new RequestHandler(client ,appobj);
		
		router.route(urlpatten).handler(appHandler);
		
		
	}
	
}
