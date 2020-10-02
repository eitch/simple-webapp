package ch.eitchnet.simple.webapp;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import ch.eitchnet.simple.webapp.app.App;
import ch.eitchnet.simple.webapp.app.DbPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebListener
public class StartupListener implements ServletContextListener {

	private static final Logger logger = LoggerFactory.getLogger(StartupListener.class);

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		DbPool.getInstance().configure();
		App.getInstance().configure(sce.getServletContext().getRealPath("/WEB-INF"));
		logger.info("Started");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		logger.info("Destroyed");
	}
}
