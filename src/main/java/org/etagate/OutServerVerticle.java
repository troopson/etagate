package org.etagate;

import org.etagate.app.AppContain;
import org.etagate.auth.AuthMgr;
import org.etagate.auth.GateAuthHandler;
import org.etagate.conf.GateSetting;
import org.etagate.helper.RequestHelper;
import org.etagate.helper.S;
import org.etagate.request.AppRoute;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
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
public class OutServerVerticle extends AbstractVerticle {

	public static final Logger log = LoggerFactory.getLogger(OutServerVerticle.class);

	private AuthMgr authMgr;

	private String webroot;

	private String upload_dir;
	
	private Router router;
	
	private WebClient webclient;
	
	private GateSetting gsetting;
	
	private AppContain appContain;
	
	

	public void start() throws Exception {

		JsonObject conf = vertx.getOrCreateContext().config();
		
		HttpServerOptions options = this.configSSL(conf);		
		
		HttpServer server = vertx.createHttpServer(options);
		
		String host = conf.getString("host","0.0.0.0");
		int port = Integer.parseInt(conf.getString("port", "80"));
		
		
		
		// ============初始化======================

		this.webroot = conf.getString("static.file.dir");
		this.upload_dir = conf.getString("upload.dir");

		this.createWebClient(conf);

		this.appContain = gsetting.getAppContain(vertx,this.webclient);
		
		
		if(gsetting.hasAuth() && !this.appContain.isEmpty())
			authMgr = new AuthMgr(gsetting.getAuthSetting(),appContain);

		String index_page = conf.getString("index.page");
		router = Router.router(this.vertx);		
		this.initRoutes(conf,server,appContain,index_page);

		server.listen(port,host, ar -> {
			if (ar.succeeded()) {
				log.info("OutServer listen on " + port);
			} else {
				log.error("OutServer Failed to start!", ar.cause());
			}
		});		

	}

	private void initRoutes(JsonObject conf,HttpServer server,AppContain appContain, String indexPage) {

		if(S.isNotBlank(indexPage))
			router.route("/").handler(r->{RequestHelper.redirect(r.request(), indexPage).end();});
		
		this.addBasicRoute(conf);

		
		if(!appContain.isEmpty())
			AppRoute.addAppRoute(appContain,router);

		//如果配置了静态文件目录，那么在没有命中前面的route path时，可以直接映射到静态文件目录中去
		if(webroot!=null){
			StaticHandler sta = StaticHandler.create(webroot);
			router.route().handler(sta);
		}
		
		
		
		server.requestHandler(router::accept);

	}

	public static final String SessionName="_SESSID";
	
	private void addBasicRoute(JsonObject conf) {
		router.route().handler(CookieHandler.create());

		BodyHandler bh = BodyHandler.create();
		bh.setMergeFormAttributes(false);
		bh.setUploadsDirectory(this.upload_dir);
		// bh.setDeleteUploadedFilesOnEnd(false);
		router.route().handler(bh);

		String hasSession = conf.getString("session","true");
		if("true".equals(hasSession)){		
			SessionStore sessionStore = null;
			if (vertx.isClustered())
				sessionStore = ClusteredSessionStore.create(vertx,SessionName);
			else
				sessionStore = LocalSessionStore.create(vertx,SessionName);		
			
			SessionHandler sessionHandler = SessionHandler.create(sessionStore);
			sessionHandler.setNagHttps(false).setCookieHttpOnlyFlag(true);
			Long sessionTimeount = conf.getLong("session.timeout");
			if(sessionTimeount!=null && sessionTimeount>0)
				sessionHandler.setSessionTimeout(sessionTimeount);
			
			router.route().handler(sessionHandler);
	
			if(gsetting.hasAuth()  && !this.appContain.isEmpty()){
				router.route().handler(UserSessionHandler.create(authMgr.authProvider));
				
				GateAuthHandler authHandler = new GateAuthHandler(authMgr);
				router.route().handler(authHandler::handle);
				
			}
		}
		
		router.route().failureHandler(rc->{
			
			HttpServerResponse response = rc.response();
			
			if(response.ended())
				return;
			
			int statusCode = rc.statusCode() == -1 ? 500 : rc.statusCode();

            log.error("Error,status code: {}. ",statusCode);
            response.setStatusCode(statusCode).end("status code:"+statusCode);
		});

	}

	// =================================================

	private void createWebClient(JsonObject conf){
		
		String app_maxWaitQueueSize =  conf.getString("app.maxWaitQueueSize","600");
		String app_maxPoolSize = conf.getString("app.maxPoolSize","10");
		
		log.info("upstream connection pool size:{}, max wait queue size: {}", app_maxPoolSize,app_maxWaitQueueSize);
		WebClientOptions op = new WebClientOptions();
		op.setMaxWaitQueueSize(Integer.parseInt(app_maxWaitQueueSize))		
		  .setIdleTimeout(2)
		  .setConnectTimeout(500)
		  .setSsl(false)
		  .setMaxPoolSize(Integer.parseInt(app_maxPoolSize))
		  .setLogActivity(true);
		this.webclient = WebClient.create(vertx,op);
	}

	public void setGsetting(GateSetting gsetting) {
		this.gsetting = gsetting;
	}
	

	private HttpServerOptions configSSL(JsonObject conf) {
		String ssl_keystore= conf.getString("ssl.keystore");
		String keystore_pass = conf.getString("ssl.keystore.pass");

		String ssl_client_keystore= conf.getString("ssl.client.keystore");
		String client_keystore_pass = conf.getString("ssl.client.keystore.pass");		
		
		HttpServerOptions options = new HttpServerOptions();
		options.setCompressionSupported(true);
		
		if(S.isNotBlank(ssl_keystore) && S.isNotBlank(keystore_pass)){
			options.setSsl(true).setKeyStoreOptions(
					new JksOptions().setPath(ssl_keystore).setPassword(keystore_pass));
			
			if(S.isNotBlank(ssl_client_keystore) && S.isNotBlank(client_keystore_pass))
				options.setClientAuth(ClientAuth.REQUIRED).setTrustStoreOptions(
				    new JksOptions().setPath(ssl_client_keystore).setPassword(client_keystore_pass));
		}
		return options;
	}


}
