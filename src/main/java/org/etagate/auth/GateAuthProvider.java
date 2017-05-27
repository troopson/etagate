/**
 * 
 */
package org.etagate.auth;

import org.etagate.app.AppObject;
import org.etagate.helper.S;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

/**
 * @author 瞿建军       Email: jianjun.qu@istuary.com
 * 2017年2月23日
 */
public class GateAuthProvider implements AuthProvider {
	
	private WebClient client = null;
			
	private AuthMgr authMgr;	
	
	public void setWebClient(WebClient client) {
		this.client = client;
	}


	public void setAuthMgr(AuthMgr authMgr) {
		this.authMgr = authMgr;
	}

	
	public void getJsonResult(AppObject appObj, String uri, JsonObject param, Handler<AsyncResult<HttpResponse<Buffer>>> h) {
		AppObject.Node node = appObj.getNode();
		HttpRequest<Buffer> req = this.client.get(node.port, node.host, uri);
		param.forEach(entry -> {
			req.addQueryParam(entry.getKey(), "" + entry.getValue());
		});

		req.timeout(5000).send(ar -> {
			h.handle(ar);			
		});

	}
	

	@Override
	public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> h) {
		
		String uri = authMgr.getAuthenticationUrl();
		
		this.getJsonResult(authMgr.getAuthAppObj(), uri, authInfo, ar->{
			if(ar.succeeded()){
				//如果用户校验后，需要返回一个json串，其中包括用户名密码校验的结果，
				// 网关会把结果返给前端页面，由页面处理最终跳转
				HttpResponse<Buffer> appResponse = ar.result();
					
				JsonObject u = appResponse.bodyAsJsonObject();
				if(u==null || u.isEmpty()){
					h.handle(Future.succeededFuture());  //这里返回一个空值，表示没有获得用户信息
					return;
				}
				
				String result = u.getString(AuthMgr.successFiled);
				if(S.isNotBlank(result)){  //成功后，返回用户对象
					GateUser gu = new GateUser(u);
					gu.setAuthProvider(this);
					h.handle(Future.succeededFuture(gu));	
				}else{
					h.handle(Future.succeededFuture());			
				}	
			}else{
				h.handle(Future.failedFuture(ar.cause()));
			}
		});
		
		
	}
	
	
	public void authorise(User user,String permission, Handler<AsyncResult<Boolean>> h){
		
		String uri = authMgr.getAuthorisationUrl();
		
		JsonObject pricipal = user.principal().copy();
		pricipal.put("permission", permission);
//		System.out.println("authorise uri: "+uri);
		this.getJsonResult(authMgr.getAuthAppObj(), uri, pricipal, res->{
			if(res.succeeded()){
				HttpResponse<Buffer> appResponse = res.result();
				
				String u = appResponse.bodyAsString();
				
				if("true".equalsIgnoreCase(u))
					h.handle(Future.succeededFuture(true));
				else
					h.handle(Future.succeededFuture(false));
			}else{
				h.handle(Future.failedFuture(res.cause()));
			}
		});
		
		
	}
	
	
	

}
