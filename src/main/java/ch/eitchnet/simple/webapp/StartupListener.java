package ch.eitchnet.simple.webapp;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebListener
public class StartupListener implements ServletContextListener {

	private static final Logger logger = LoggerFactory.getLogger(StartupListener.class);

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		DbPool.getInstance().configure();
		logger.info("Started");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		logger.info("Destroyed");
	}
}
