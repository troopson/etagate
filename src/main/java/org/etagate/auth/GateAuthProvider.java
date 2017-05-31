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
	
	/**
	 *  身份识别的逻辑： 
	 *  身份识别的请求，要求返回一个json格式的结果，
	 *  如果通过了验证，那么json结果中， 要求successField所对应的字段必须存在，并且值不能为false，
	 *  其他情况，都会视为没有通过验证
	 */

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
				if(S.isNotBlank(result) && !"false".equals(result)){  //成功后，返回用户对象
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
	
	/**
	 * 访问控制的逻辑：
	 * 当某个用户访问某个资源的时候，如果有权限访问，那么直接返回一个true的字符串，其它结果都会认为是无权访问
	 * @param user
	 * @param permission
	 * @param h
	 */
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
