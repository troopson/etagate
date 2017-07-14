/**
 * 
 */
package org.etagate.auth;

import java.io.UnsupportedEncodingException;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AbstractUser;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;

/**
 * @author 瞿建军 Email: troopson@163.com 2017年2月23日
 */
public class GateUser extends AbstractUser {

	private JsonObject principal = null;
	
	private transient String encodedPrincipal=null;

	public GateUser(JsonObject json) {

		this.principal = json;
		try {
			this.encodedPrincipal = new String(json.encode().getBytes("UTF-8"),"ISO8859-1");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

	}

	public User isAuthorised(GateAuthProvider authProvider,String authority, Handler<AsyncResult<Boolean>> resultHandler) {
		if (cachedPermissions.contains(authority)) {
			resultHandler.handle(Future.succeededFuture(true));
		} else {
			doIsPermitted(authProvider,authority, res -> {
				if (res.succeeded()) {
					if (res.result()) {
						cachedPermissions.add(authority);
					}
				}
				resultHandler.handle(res);
			});
		}
		return this;
	}

	protected void doIsPermitted(GateAuthProvider authProvider,String permission, Handler<AsyncResult<Boolean>> resultHandler) {
		authProvider.authorise(this, permission, resultHandler);
	}

	@Override
	protected void doIsPermitted(String permission, Handler<AsyncResult<Boolean>> resultHandler) {
	}
	@Override
	public void setAuthProvider(AuthProvider authProvider) {
	
	}
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see io.vertx.ext.auth.User#principal()
	 */
	@Override
	public JsonObject principal() {
		return principal;
	}
	
	public String encodePrincipal(){
		return this.encodedPrincipal;
	}

	public String getAttr(String key) {
		return this.principal.getString(key);
	}

}
