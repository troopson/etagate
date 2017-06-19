/**
 * 
 */
package org.etagate.auth;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;

/**
 * @author 瞿建军 Email: jianjun.qu@istuary.com 2017年5月17日
 */
public class GateAuthHandler extends AuthHandlerImpl {
	
	private final AuthMgr authMgr;

	public GateAuthHandler(AuthMgr authMgr) {
		super(authMgr.getAuthProvider());
		this.authMgr = authMgr;	
	}
	
	


	@Override
	public void handle(RoutingContext rc) {

		HttpServerRequest request = rc.request();
		
		String uri = request.uri();
		if(authMgr.isMatchNoAuthPath(uri)){
			//不需要权限控制的uri
			rc.next();
		
		}else if(authMgr.isAuthenticateAction(uri)){
			
			JsonObject p = new JsonObject();
			request.params().forEach(entry->{
				p.put(entry.getKey(), entry.getValue());
			});
			request.formAttributes().forEach(entry->{
				p.put(entry.getKey(), entry.getValue());
			});
			
			AuthProvider ap = authMgr.getAuthProvider();
			ap.authenticate(p, res->{
				if(res.succeeded()){
					User u = res.result();
					//如果校验成功，返回200，否则返回400
					if(u==null)
						rc.fail(401);
					else{
						//这里是设置到了rc里面，并没有放到session，
						//session中是通过一个监听putHeader的endHandler放入的
						//在redis中，保存一份登录成功的用户的principal
						rc.setUser(res.result());
						HttpServerResponse clientResponse = rc.response();

						String s = u.principal().getString("mainpage");
						if(s==null)
							s=this.authMgr.getMainPage();
						if(s!=null){
							String mainpage=request.absoluteURI().replace(request.uri(), "")+s;
							System.out.println(request.absoluteURI()+"   "+request.uri()+"   "+request.path()+"   "+mainpage);
							clientResponse.putHeader("Content-Type", "text/html;charset=utf-8");
							clientResponse.putHeader("Location", mainpage);
							clientResponse.setStatusCode(302);
						}else{
							clientResponse.setStatusCode(200);
						}						
						clientResponse.end("ok");
						
					}
				}else{
					res.cause().printStackTrace();
					rc.fail(500);
				}
			});			
						
		}else{
			User u = rc.user();
			if(u==null){
				rc.fail(403);
			}else
				this.authorise(u, rc);
			
		}	

	}

	
	@Override
	protected void authorise(User user, RoutingContext context) {
//		System.out.println("authorise =>"+user.principal().toString());
		((GateUser)user).isAuthorised(authMgr.getAuthProvider(),context.request().uri(),  res -> {
	        if (res.succeeded()) {
	           if (res.result()) {
	              context.next();
	           } else {
	              context.fail(403);
	           }
	        } else {
	           context.fail(res.cause());
	        }
	    });
		
	}

}
