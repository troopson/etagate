/**
 * 
 */
package org.etagate.auth;

import java.util.HashSet;
import java.util.Set;

import org.etagate.app.App;
import org.etagate.app.AppContain;
import org.etagate.helper.S;

import io.vertx.core.json.JsonObject;

/**
 * @author 瞿建军       Email: troopson@163.com
 * 2017年5月18日
 */
public class AuthMgr {

	private String authenticationUrl;
	
	private String authorisationUrl;
	
	private String mainpage;
	
	private String loginpage;
	
	
	private String successFiled="userid";
	
	private static Set<String> no_AuthPath;
	
	private String not_auth_sufix=null;
	
	public final App authApp;
		
	public final GateAuthProvider authProvider;
	
	
	/*
	 * "not.auth.sufix": "*.bmp|*.gif|*.jpg|*.png|*.woff|*.css|*.js",
    "authentication.url": "/waweb/a/login",
    "authorisation.url": "/waweb/a/check",
    "auth.app": "waweb"
	 */
	public AuthMgr(JsonObject conf,AppContain app){
		
		this.authenticationUrl = conf.getString("authentication");
		this.authorisationUrl = conf.getString("authorisation");
		this.mainpage = conf.getString("mainpage");
		this.successFiled = conf.getString("successfield","userid");
		this.loginpage = conf.getString("loginpage");
		
		String authapp = conf.getString("app");
				
		String sufix = conf.getString("exclude.end");		
		String noauthpath = conf.getString("exclude.start");	
		this.setNotAuthSufix(sufix);
		this.setNoAuthPaths(noauthpath);
		if(S.isNotBlank(this.loginpage))
			this.addNoAuthPath(this.loginpage);
		
		this.authApp = app.getAppInfo(authapp);		
		
		this.authenticationUrl = this.authApp.offsetUrl(this.authenticationUrl);
		this.authorisationUrl = this.authApp.offsetUrl(this.authorisationUrl);

		this.authProvider = new GateAuthProvider(this);
		
		
	}
	
	public void addNoAuthPath(String path){
		if(no_AuthPath==null)
			no_AuthPath = new HashSet<>();
		
		no_AuthPath.add(path.trim());
	}
	
	private void setNoAuthPaths(String noauth){
		if(S.isBlank(noauth))
			return;
			
		String[] s = noauth.split(",");
		
		for(String a : s)
			this.addNoAuthPath(a);
		
	}
	
	private void setNotAuthSufix(String sufix){
		//*.bmp|*.gif|*.jpg|*.png|*.woff|*.css|*.js
		//[a-zA-Z0-9:/\.]*(\.bmp|\.gif|\.jpg|\.png|\.woff|\.css|\.js)($|\?.*)
		
		if(S.isBlank(sufix))
			return;
		
		String trans = sufix.replace(',', '|').replace("*","").replace(".", "\\.");
		this.not_auth_sufix="[^\\?&#]*("+ trans +")($|\\?.*)";
		
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




	public String getAuthenticationUrl() {
		return authenticationUrl;
	}
	
	
	public String getSuccessField(){
		return this.successFiled;
	}
	
	public String getMainPage(){
		return this.mainpage;
	}



	public String getAuthorisationUrl() {
		return authorisationUrl;
	}

	public String getLoginpage() {
		return loginpage;
	}





	


}
