package ch.eitchnet.simple.webapp.app;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import li.strolch.privilege.base.PrivilegeException;
import li.strolch.privilege.handler.PrivilegeHandler;
import li.strolch.privilege.model.Certificate;
import li.strolch.privilege.model.PrivilegeContext;
import li.strolch.privilege.model.Usage;

public class UserHandler {

	private PrivilegeHandler privilegeHandler;

	private Map<String, PrivilegeContext> sessions;

	public UserHandler(PrivilegeHandler privilegeHandler) {
		this.privilegeHandler = privilegeHandler;
		this.sessions = Collections.synchronizedMap(new HashMap<>());
	}

	public PrivilegeContext authenticate(String username, char[] password, String source, Usage usage,
			boolean keepAlive) {
		Certificate certificate = this.privilegeHandler.authenticate(username, password, source, usage, keepAlive);
		PrivilegeContext privilegeContext = this.privilegeHandler.validate(certificate, source);
		this.sessions.put(certificate.getAuthToken(), privilegeContext);
		return privilegeContext;
	}

	public PrivilegeContext validate(String sessionId, String source) {
		PrivilegeContext privilegeContext = this.sessions.get(sessionId);
		if (privilegeContext == null)
			throw new PrivilegeException("No privilegeContext available for " + sessionId);
		this.privilegeHandler.validate(privilegeContext.getCertificate(), source);
		return privilegeContext;
	}

	public void invalidate(Certificate certificate) {
		this.privilegeHandler.invalidate(certificate);
	}

	public PrivilegeContext refreshSession(Certificate certificate, String source) {
		Certificate refreshedCert = this.privilegeHandler.refresh(certificate, source);
		PrivilegeContext privilegeContext = this.privilegeHandler.validate(refreshedCert, source);
		this.sessions.put(certificate.getAuthToken(), privilegeContext);
		return privilegeContext;
	}

	public boolean isRefreshAllowed() {
		return this.privilegeHandler.isRefreshAllowed();
	}

	public int getSessionMaxKeepAliveMinutes() {
		return 24 * 60;
	}

	public int getCookieMaxAge() {
		return (int) TimeUnit.DAYS.toSeconds(1);
	}
}
