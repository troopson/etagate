/**
 * 
 */
package org.etagate.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AbstractUser;
import io.vertx.ext.auth.AuthProvider;

/**
 * @author 瞿建军 Email: jianjun.qu@istuary.com 2017年2月23日
 */
public class GateUser extends AbstractUser {

	private JsonObject principal = null;
	private GateAuthProvider authProvider = null;

	public GateUser(JsonObject json) {
		
		this.principal = json;
				
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
	
	
	public String getAttr(String key){
		return this.principal.getString(key);
	}

	@Override
	public void setAuthProvider(AuthProvider authProvider) {
		this.authProvider = (GateAuthProvider) authProvider;
	}

	@Override
	protected void doIsPermitted(String permission, Handler<AsyncResult<Boolean>> resultHandler) {
		this.authProvider.authorise(this, permission, resultHandler);
	}

}
