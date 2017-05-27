/**
 * 
 */
package org.etagate.auth;

import java.util.HashSet;
import java.util.Set;

import org.etagate.app.AppInfo;
import org.etagate.app.AppObject;
import org.etagate.helper.S;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

/**
 * @author 瞿建军       Email: jianjun.qu@istuary.com
 * 2017年5月18日
 */
public class AuthMgr {

	private String authenticationUrl="/login";
	
	private String authorisationUrl = "/check_right";
	
	private String authApp = "waweb";
	private AppObject authAppObj=null;
	
	public static String successFiled="userid";
	
	private static Set<String> no_AuthPath;
	
	private String not_auth_sufix=null;
		
	private GateAuthProvider authProvider;
	
	
	/*
	 * "not.auth.sufix": "*.bmp|*.gif|*.jpg|*.png|*.woff|*.css|*.js",
    "authentication.url": "/waweb/a/login",
    "authorisation.url": "/waweb/a/check",
    "auth.app": "waweb"
	 */
	public AuthMgr(JsonObject conf){
		
		this.authenticationUrl = conf.getString("authentication");
		this.authorisationUrl = conf.getString("authorisation");
		this.authApp = conf.getString("app");
		AuthMgr.successFiled = conf.getString("successfield");
				
		String sufix = conf.getString("exclude.end");		
		String noauthpath = conf.getString("exclude.start");	
		this.setNotAuthSufix(sufix);
		this.setNoAuthPath(noauthpath);
		
	}
	
	
	public void setNoAuthPath(String noauth){
		if(S.isBlank(noauth))
			return;
			
		String[] s = noauth.split(",");
		
		no_AuthPath = new HashSet<>();
		for(String a : s)
			no_AuthPath.add(a.trim());
		
	}
	
	public void setNotAuthSufix(String sufix){
		//*.bmp|*.gif|*.jpg|*.png|*.woff|*.css|*.js
		//[a-zA-Z0-9:/\.]*(\.bmp|\.gif|\.jpg|\.png|\.woff|\.css|\.js)($|\?.*)
		
		if(S.isBlank(sufix))
			return;
		
		String trans = sufix.replace(',', '|').replace("*","").replace(".", "\\.");
		this.not_auth_sufix="[a-zA-Z0-9:/\\.]*("+ trans +")($|\\?.*)";
		
	}
	
	public void init(WebClient client,AppInfo app) {

		this.authAppObj = app.getAppInfo(authApp);
		
		this.authProvider = new GateAuthProvider();
		this.authProvider.setWebClient(client);
		this.authProvider.setAuthMgr(this);

	}

		
	
	public boolean isMatchNoAuthPath(String uri){
		//如果匹配到指定的后缀，那么不进行权限控制
		if(uri.matches(this.not_auth_sufix))
			return true;
		
		for(String s : no_AuthPath){
			if(s.equals(uri) || uri.startsWith(s))
				return true;
		}
		return false;
		
	}
	
	public boolean isAuthenticateAction(String uri){
		if(uri.startsWith(this.authenticationUrl))
			return true;
		else
			return false;
	}
	
	//=====================================================

	public GateAuthProvider getAuthProvider() {
		return authProvider;
	}

	public void setAuthProvider(GateAuthProvider authProvider) {
		this.authProvider = authProvider;
	}

	public String getAuthenticationUrl() {
		return authenticationUrl;
	}



	public String getAuthorisationUrl() {
		return authorisationUrl;
	}




	public String getAuthApp() {
		return authApp;
	}
	
	public AppObject getAuthAppObj(){
		return this.authAppObj;
	}



	


}
