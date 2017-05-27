package org.etagate.auth;

import com.jrzn.icestone.sql.Redis;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.auth.PRNG;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.impl.ClusteredSessionStoreImpl;
import io.vertx.ext.web.sstore.impl.SessionImpl;

/**
 * @author 瞿建军 Email: jianjun.qu@istuary.com 2017年5月23日
 */
public class RedisSessionStore extends ClusteredSessionStoreImpl {

	public static String KEY_PRE = "SESSION_OBJECT:";

	public static String SIZE_KEY = "ETAGATE_SESSION_SIZE:";

	private Redis redis;

	private long expire = 1000 * 60 * 60 * 2;
	
	private final PRNG random;
	
	
	public static RedisSessionStore create(Vertx vertx,Redis redis){
		return new RedisSessionStore(vertx,redis);
	}

	public RedisSessionStore(Vertx vertx,  Redis redis) {
		super(vertx, "redis",5000);
		this.redis = redis;
		this.random = new PRNG(vertx);
	}

	public void setExpireTime(long t) {
		this.expire = t;
	}

	private String key(String id) {
		return KEY_PRE + id;
	}

	@Override
	public void get(String id, Handler<AsyncResult<Session>> resultHandler) {
		redis.getString(this.key(id), r -> {
			if (r.succeeded()) {
				String s = r.result();
				Buffer buff = Buffer.buffer(s);
				SessionImpl sess = (SessionImpl) createSession(this.expire);
				sess.readFromBuffer(0, buff);
				resultHandler.handle(Future.succeededFuture(sess));
			} else {
				resultHandler.handle(Future.failedFuture(r.cause()));
			}
		});

	}

	@Override
	public void delete(String id, Handler<AsyncResult<Boolean>> resultHandler) {
		this.redis.del(key(id), r->{
			if(r.succeeded())
				resultHandler.handle(Future.succeededFuture(true));
			else
				resultHandler.handle(Future.failedFuture(r.cause()));
		});

	}

	@Override
	public void put(Session session, Handler<AsyncResult<Boolean>> resultHandler) {
		Buffer buff = Buffer.buffer();
		((SessionImpl)session).writeToBuffer(buff);
		this.redis.putString(key(session.id()), buff.toString(), this.expire, r->{
			if(r.succeeded())
				resultHandler.handle(Future.succeededFuture(true));
			else
				resultHandler.handle(Future.failedFuture(r.cause()));
		});

	}

	@Override
	public void clear(Handler<AsyncResult<Boolean>> resultHandler) {
		this.redis.delWithPattern(KEY_PRE+"*", re->{});

	}

	@Override
	public void size(Handler<AsyncResult<Integer>> resultHandler) {
		this.redis.keys(KEY_PRE+"*", r->{
			if(r.succeeded())
				resultHandler.handle(Future.succeededFuture(r.result().intValue()));
			else
				resultHandler.handle(Future.failedFuture(r.cause()));
		});
	}

}
