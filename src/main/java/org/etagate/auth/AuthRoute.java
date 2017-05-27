/**
 * 
 */
package org.etagate.auth;

import io.vertx.ext.web.Router;

/**
 * @author 瞿建军       Email: jianjun.qu@istuary.com
 * 2017年5月19日
 */
public class AuthRoute {

	public static void addAuthRoute(Router router,AuthMgr authMgr){
		
		GateAuthHandler handler = new GateAuthHandler(authMgr);
		router.route().handler(handler);
		
	}
	
}
