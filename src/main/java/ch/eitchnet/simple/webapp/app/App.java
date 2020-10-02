package ch.eitchnet.simple.webapp.app;

import java.io.File;

import li.strolch.privilege.handler.PrivilegeHandler;
import li.strolch.privilege.helper.PrivilegeInitializationHelper;

public class App {

	private static final App instance;

	static {
		instance = new App();
	}

	private UserHandler userHandler;

	public static App getInstance() {
		return instance;
	}

	public void configure(String realPath) {
		File privilegeConfigFile = new File(realPath, "config/PrivilegeConfig.xml");
		PrivilegeHandler privilegeHandler = PrivilegeInitializationHelper.initializeFromXml(privilegeConfigFile);

		this.userHandler = new UserHandler(privilegeHandler);
	}

	public UserHandler getUserHandler() {
		return this.userHandler;
	}
}
