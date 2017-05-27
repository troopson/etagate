package org.etagate;

import org.etagate.app.AppInfo;
import org.etagate.auth.AuthMgr;
import org.etagate.auth.AuthRoute;
import org.etagate.conf.GateSetting;
import org.etagate.request.AppRoute;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.UserSessionHandler;
import io.vertx.ext.web.sstore.ClusteredSessionStore;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;

/**
 * @author 瞿建军 Email: jianjun.qu@istuary.com 2017年4月11日
 */
public class GateVerticle extends AbstractVerticle {

	public static final Logger log = LoggerFactory.getLogger(GateVerticle.class);

	private AuthMgr authMgr;

	private String webroot;

	private String upload_dir;

	private Router router;
	
	public void start() throws Exception {

		HttpServerOptions options = new HttpServerOptions();
		options.setCompressionSupported(true);
		HttpServer server = vertx.createHttpServer(options);

		JsonObject conf = vertx.getOrCreateContext().config();
		
		int port = Integer.parseInt(conf.getString("port", "80"));

		// ============初始化======================

		this.webroot = conf.getString("static.file.dir");
		this.upload_dir = conf.getString("upload.dir");

		AppInfo app = GateSetting.appInfo;
		
		authMgr = new AuthMgr(GateSetting.authSetting);
		authMgr.init(getWebClient(), app);

		router = Router.router(this.vertx);
		
		this.initRoutes(server);

		server.listen(port, ar -> {
			if (ar.succeeded()) {
				log.info("Server listen on " + port);
			} else {
				log.error("Failed to start!", ar.cause());
			}
		});		

	}

	private void initRoutes(HttpServer server) {

		this.addBasicRoute();

		AuthRoute.addAuthRoute(router, authMgr);

		AppRoute.addAppRoute(this);

		server.requestHandler(router::accept);

	}

	private void addBasicRoute() {
		router.route().handler(CookieHandler.create());

		BodyHandler bh = BodyHandler.create();
		bh.setMergeFormAttributes(false);
		bh.setUploadsDirectory(this.upload_dir);
		// bh.setDeleteUploadedFilesOnEnd(false);
		router.route().handler(bh);

		SessionStore sessionStore = null;
		if (vertx.isClustered())
			sessionStore = ClusteredSessionStore.create(vertx);
		else
			sessionStore = LocalSessionStore.create(vertx);
		SessionHandler sessionHandler = SessionHandler.create(sessionStore);
		sessionHandler.setNagHttps(false);
		router.route().handler(sessionHandler);

		router.route().handler(UserSessionHandler.create(authMgr.getAuthProvider()));

	}

	// =================================================

	public AppInfo getApp() {
		return GateSetting.appInfo;
	}

	public WebClient getWebClient() {
		return WebClient.create(vertx);
	}

	public String getWebroot() {
		return webroot;
	}

	public Router getRouter() {
		return router;
	}

	public AuthMgr getAuthMgr() {
		return authMgr;
	}

}
