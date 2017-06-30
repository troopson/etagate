/**
 * 
 */
package org.etagate.helper;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

/**
 * @author 瞿建军       Email: troopson@163.com
 * 2017年6月29日
 */
public class RequestHelper {
	
	public static HttpServerResponse redirect(HttpServerRequest request,String page){
		return redirect(request, request.response(), page);
	}

	public static HttpServerResponse redirect(HttpServerRequest request,HttpServerResponse response,String page){
		String uri = request.uri();
		String mainpage= null;
//		System.out.println("--->"+uri);
		if(!"/".equals(uri))
		    mainpage=request.absoluteURI().replace(request.uri(), "")+page;
		else{
			if(page.startsWith("/"))
				page = page.substring(1);
			mainpage=request.absoluteURI()+page;
		}
		response.putHeader("Content-Type", "text/html;charset=utf-8");
		response.putHeader("Location", mainpage);
		response.setStatusCode(302);
		return response;
	}
	
}
