package ch.eitchnet.simple.webapp.rest;

import static ch.eitchnet.simple.webapp.app.AppConstants.PRIVILEGE_CONTEXT;
import static ch.eitchnet.simple.webapp.app.AppConstants.REQUEST_SOURCE;
import static li.strolch.utils.helper.StringHelper.isEmpty;
import static li.strolch.utils.helper.StringHelper.isNotEmpty;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Provider;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.eitchnet.simple.webapp.app.App;
import li.strolch.privilege.base.AccessDeniedException;
import li.strolch.privilege.base.PrivilegeException;
import li.strolch.privilege.model.PrivilegeContext;
import li.strolch.privilege.model.Usage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationRequestFilter implements ContainerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(AuthenticationRequestFilter.class);

	@Context
	private HttpServletRequest request;

	private Set<String> unsecuredPaths;

	protected Set<String> getUnsecuredPaths() {
		Set<String> paths = new HashSet<>();
		paths.add("authentication");
		return paths;
	}

	protected boolean isUnsecuredPath(ContainerRequestContext requestContext) {

		// we have to allow OPTIONS for CORS
		if (requestContext.getMethod().equals("OPTIONS"))
			return true;

		List<String> matchedURIs = requestContext.getUriInfo().getMatchedURIs();

		// we allow unauthorized access to the authentication service
		if (this.unsecuredPaths == null)
			this.unsecuredPaths = getUnsecuredPaths();

		return matchedURIs.stream().anyMatch(s -> this.unsecuredPaths.contains(s));
	}

	@Override
	public void filter(ContainerRequestContext requestContext) {
		String remoteIp = getRemoteIp(this.request);
		logger.info("Remote IP: " + remoteIp + ": " + requestContext.getMethod() + " " + requestContext.getUriInfo()
				.getRequestUri());

		if (isUnsecuredPath(requestContext))
			return;

		try {

			validateSession(requestContext, remoteIp);

		} catch (AccessDeniedException e) {
			logger.error(e.getMessage());
			requestContext.abortWith(
					Response.status(Response.Status.FORBIDDEN).header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
							.entity("User is not authorized!").build()); //$NON-NLS-1$
		} catch (PrivilegeException e) {
			logger.error(e.getMessage());
			requestContext.abortWith(
					Response.status(Response.Status.UNAUTHORIZED).header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
							.entity("User is not authenticated!").build()); //$NON-NLS-1$
		} catch (Exception e) {
			logger.error(e.getMessage());
			requestContext.abortWith(
					Response.status(Response.Status.FORBIDDEN).header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
							.entity("User cannot access the resource.").build()); //$NON-NLS-1$
		}
	}

	protected void validateSession(ContainerRequestContext requestContext, String remoteIp) {

		String authorization = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
		authorization = authorization == null ? "" : authorization.trim();

		if (isEmpty(authorization) || (authorization.startsWith("Basic "))) {
			validateCookie(requestContext, remoteIp);
			return;
		}

		boolean basicAuth = authorization.startsWith("Basic ");
		if (basicAuth) {
			authenticateBasic(requestContext, authorization, remoteIp);
			return;
		}

		validateCertificate(requestContext, authorization, remoteIp);
	}

	private String getSessionIdFromCookie(ContainerRequestContext requestContext) {
		Cookie cookie = requestContext.getCookies().get(HttpHeaders.AUTHORIZATION);
		if (cookie == null)
			return "";

		String sessionId = cookie.getValue();
		if (sessionId == null)
			return "";

		return sessionId.trim();
	}

	private void validateCookie(ContainerRequestContext requestContext, String remoteIp) {
		String sessionId = getSessionIdFromCookie(requestContext);
		if (isEmpty(sessionId)) {
			logger.error(
					"No Authorization header or cookie on request to URL " + requestContext.getUriInfo().getPath());
			requestContext.abortWith(
					Response.status(Response.Status.FORBIDDEN).header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
							.entity("Missing Authorization!") //$NON-NLS-1$
							.build());
			return;
		}

		validateCertificate(requestContext, sessionId, remoteIp);
	}

	private void authenticateBasic(ContainerRequestContext requestContext, String authorization, String remoteIp) {
		String basicAuth = authorization.substring("Basic ".length());
		basicAuth = new String(Base64.getDecoder().decode(basicAuth.getBytes()), StandardCharsets.UTF_8);
		String[] parts = basicAuth.split(":");
		if (parts.length != 2) {
			requestContext.abortWith(
					Response.status(Response.Status.BAD_REQUEST).header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
							.entity("Invalid Basic Authorization!") //$NON-NLS-1$
							.build());
			return;
		}

		logger.info("Performing basic auth for user " + parts[0] + "...");
		PrivilegeContext privilegeContext = App.getInstance().getUserHandler()
				.authenticate(parts[0], parts[1].toCharArray(), remoteIp, Usage.SINGLE, false);

		requestContext.setProperty(PRIVILEGE_CONTEXT, privilegeContext);
		requestContext.setProperty(REQUEST_SOURCE, remoteIp);
	}

	private void validateCertificate(ContainerRequestContext requestContext, String sessionId, String remoteIp) {
		PrivilegeContext privilegeContext = App.getInstance().getUserHandler().validate(sessionId, remoteIp);
		requestContext.setProperty(PRIVILEGE_CONTEXT, privilegeContext);
		requestContext.setProperty(REQUEST_SOURCE, remoteIp);
	}

	public static String getRemoteIp(HttpServletRequest request) {

		String remoteHost = request.getRemoteHost();
		String remoteAddr = request.getRemoteAddr();

		StringBuilder sb = new StringBuilder();
		if (remoteHost.equals(remoteAddr))
			sb.append(remoteAddr);
		else {
			sb.append(remoteHost).append(": (").append(remoteAddr).append(")");
		}

		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (isNotEmpty(xForwardedFor))
			sb.append(" (fwd)=> ").append(xForwardedFor);

		return sb.toString();
	}
}
