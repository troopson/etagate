package org.etagate;

import org.etagate.app.AppContain;
import org.etagate.auth.AuthMgr;
import org.etagate.auth.GateAuthHandler;
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
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.UserSessionHandler;
import io.vertx.ext.web.sstore.ClusteredSessionStore;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;

/**
 * @author 瞿建军 Email: troopson@163.com 2017年4月11日
 */
public class GateVerticle extends AbstractVerticle {

	public static final Logger log = LoggerFactory.getLogger(GateVerticle.class);

	private AuthMgr authMgr;

	private String webroot;

	private String upload_dir;

	private Router router;
	
	private WebClient webclient;
	
	public void start() throws Exception {

		HttpServerOptions options = new HttpServerOptions();
		options.setCompressionSupported(true);
		HttpServer server = vertx.createHttpServer(options);

		JsonObject conf = vertx.getOrCreateContext().config();
		
		int port = Integer.parseInt(conf.getString("port", "80"));

		// ============初始化======================

		this.webroot = conf.getString("static.file.dir");
		this.upload_dir = conf.getString("upload.dir");

		AppContain appContain = GateSetting.appContain;
		
		this.createWebClient();
		
		if(GateSetting.hasAuth)
			authMgr = new AuthMgr(GateSetting.authSetting,this.webclient,appContain);
				
		
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

		if(GateSetting.hasAuth){
			GateAuthHandler authHandler = new GateAuthHandler(authMgr);
			router.route().handler(authHandler::handle);
		}
		
		AppRoute.addAppRoute(GateSetting.appContain,webclient,router);

		//如果配置了静态文件目录，那么在没有命中前面的route path时，可以直接映射到静态文件目录中去
		if(webroot!=null){
			StaticHandler sta = StaticHandler.create(webroot);
			router.route().handler(sta);
		}
		
		server.requestHandler(router::accept);

	}

	public static final String SessionName="web.session";
	
	private void addBasicRoute() {
		router.route().handler(CookieHandler.create());

		BodyHandler bh = BodyHandler.create();
		bh.setMergeFormAttributes(false);
		bh.setUploadsDirectory(this.upload_dir);
		// bh.setDeleteUploadedFilesOnEnd(false);
		router.route().handler(bh);

		SessionStore sessionStore = null;
		if (vertx.isClustered())
			sessionStore = ClusteredSessionStore.create(vertx,SessionName);
		else
			sessionStore = LocalSessionStore.create(vertx,SessionName);
		SessionHandler sessionHandler = SessionHandler.create(sessionStore);
		sessionHandler.setNagHttps(false);
		router.route().handler(sessionHandler);

		if(GateSetting.hasAuth){
			router.route().handler(UserSessionHandler.create(authMgr.authProvider));
		}

	}

	// =================================================

	private void createWebClient(){
		WebClientOptions op = new WebClientOptions();
		op.setKeepAlive(true);
		op.setConnectTimeout(3000);
		op.setSsl(false);
		op.setLogActivity(true);
		this.webclient = WebClient.create(vertx,op);
	}



}
