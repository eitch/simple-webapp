package ch.eitchnet.simple.webapp.rest;

import static ch.eitchnet.simple.webapp.app.AppConstants.AUTHORIZATION_EXPIRATION_DATE;
import static ch.eitchnet.simple.webapp.rest.AuthenticationRequestFilter.getRemoteIp;
import static javax.ws.rs.core.Response.*;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import ch.eitchnet.simple.webapp.app.App;
import com.google.gson.*;
import li.strolch.privilege.base.AccessDeniedException;
import li.strolch.privilege.base.InvalidCredentialsException;
import li.strolch.privilege.base.PrivilegeException;
import li.strolch.privilege.model.Certificate;
import li.strolch.privilege.model.IPrivilege;
import li.strolch.privilege.model.PrivilegeContext;
import li.strolch.privilege.model.Usage;
import li.strolch.utils.iso8601.ISO8601;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("authentication")
public class AuthenticationResource {

	private static final Logger logger = LoggerFactory.getLogger(AuthenticationResource.class);

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response authenticate(@Context HttpServletRequest request, @Context HttpHeaders headers, String data) {

		JsonObject login = JsonParser.parseString(data).getAsJsonObject();

		try {

			if (!login.has("username") || login.get("username").getAsString().length() < 2) {
				logger.error("Authentication failed: Username was not given or is too short!");
				JsonObject loginResult = new JsonObject();
				loginResult.addProperty("msg", MessageFormat.format("Could not log in due to: {0}",
						"Username was not given or is too short!")); //$NON-NLS-2$
				return status(Status.BAD_REQUEST).entity(loginResult.toString()).build();
			}

			if (!login.has("password") || login.get("password").getAsString().length() < 3) {
				logger.error("Authentication failed: Password was not given or is too short!");
				JsonObject loginResult = new JsonObject();
				loginResult.addProperty("msg", MessageFormat.format("Could not log in due to: {0}",
						"Password was not given or is too short!")); //$NON-NLS-2$
				return status(Status.BAD_REQUEST).entity(loginResult.toString()).build();
			}

			String username = login.get("username").getAsString();
			String passwordEncoded = login.get("password").getAsString();
			boolean keepAlive = login.has("keepAlive") && login.get("keepAlive").getAsBoolean();

			byte[] decode = Base64.getDecoder().decode(passwordEncoded);
			char[] password = new String(decode).toCharArray();

			if (password.length < 3) {
				logger.error("Authentication failed: Password was not given or is too short!");
				JsonObject loginResult = new JsonObject();
				loginResult.addProperty("msg", MessageFormat.format("Could not log in due to: {0}",
						"Password was not given or is too short!")); //$NON-NLS-2$
				return status(Status.BAD_REQUEST).entity(loginResult.toString()).build();
			}

			String source = getRemoteIp(request);
			PrivilegeContext privilegeContext = App.getInstance().getUserHandler()
					.authenticate(username, password, source, Usage.ANY, keepAlive);

			return getAuthenticationResponse(request, privilegeContext, true);

		} catch (InvalidCredentialsException e) {
			logger.error("Authentication failed due to: " + e.getMessage());
			JsonObject loginResult = new JsonObject();
			loginResult.addProperty("msg", "Could not log in as the given credentials are invalid"); //$NON-NLS-1$
			return status(Status.UNAUTHORIZED).entity(loginResult.toString()).build();
		} catch (AccessDeniedException e) {
			logger.error("Authentication failed due to: " + e.getMessage());
			JsonObject loginResult = new JsonObject();
			loginResult.addProperty("msg",
					MessageFormat.format("Could not log in due to: {0}", e.getMessage())); //$NON-NLS-2$
			return status(Status.UNAUTHORIZED).entity(loginResult.toString()).build();
		} catch (PrivilegeException e) {
			logger.error(e.getMessage(), e);
			JsonObject loginResult = new JsonObject();
			loginResult.addProperty("msg",
					MessageFormat.format("Could not log in due to: {0}", e.getMessage())); //$NON-NLS-2$
			return status(Status.FORBIDDEN).entity(loginResult.toString()).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			String msg = e.getMessage();
			JsonObject loginResult = new JsonObject();
			loginResult.addProperty("msg", MessageFormat.format("{0}: {1}", e.getClass().getName(), msg)); //$NON-NLS-1$
			return serverError().entity(loginResult.toString()).build();
		}
	}

	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{authToken}")
	public Response invalidateSession(@Context HttpServletRequest request, @PathParam("authToken") String authToken) {

		JsonObject logoutResult = new JsonObject();

		try {

			String source = getRemoteIp(request);
			PrivilegeContext privilegeContext = App.getInstance().getUserHandler().validate(authToken, source);
			Certificate certificate = privilegeContext.getCertificate();
			App.getInstance().getUserHandler().invalidate(certificate);

			logoutResult.addProperty("username", certificate.getUsername());
			logoutResult.addProperty("authToken", authToken);
			logoutResult.addProperty("msg", //$NON-NLS-1$
					MessageFormat.format("{0} has been logged out.", certificate.getUsername()));
			return ok().entity(logoutResult.toString()).build();

		} catch (PrivilegeException e) {
			logger.error("Failed to invalidate session due to: " + e.getMessage());
			logoutResult.addProperty("msg",
					MessageFormat.format("Could not logout due to: {0}", e.getMessage())); //$NON-NLS-2$
			return status(Status.UNAUTHORIZED).entity(logoutResult.toString()).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			String msg = e.getMessage();
			logoutResult
					.addProperty("msg", MessageFormat.format("{0}: {1}", e.getClass().getName(), msg)); //$NON-NLS-1$
			return serverError().entity(logoutResult.toString()).build();
		}
	}

	@HEAD
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{authToken}")
	public Response validateSession(@Context HttpServletRequest request, @PathParam("authToken") String authToken) {

		try {

			String source = getRemoteIp(request);
			App.getInstance().getUserHandler().validate(authToken, source);

			return ok().build();

		} catch (PrivilegeException e) {
			logger.error("Session validation failed: " + e.getMessage());
			JsonObject root = new JsonObject();
			root.addProperty("msg", MessageFormat.format("Session invalid: {0}", e.getMessage()));
			String json = new Gson().toJson(root);
			return status(Status.UNAUTHORIZED).entity(json).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			String msg = e.getMessage();
			JsonObject root = new JsonObject();
			root.addProperty("msg", MessageFormat.format("Session invalid: {0}: {1}", e.getClass().getName(), msg));
			String json = new Gson().toJson(root);
			return serverError().entity(json).build();
		}
	}

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{authToken}")
	public Response getValidatedSession(@Context HttpServletRequest request, @PathParam("authToken") String authToken) {

		try {

			String source = getRemoteIp(request);
			PrivilegeContext privilegeContext = App.getInstance().getUserHandler().validate(authToken, source);
			return getAuthenticationResponse(request, privilegeContext, false);

		} catch (PrivilegeException e) {
			logger.error("Session validation failed: " + e.getMessage());
			JsonObject root = new JsonObject();
			root.addProperty("msg", MessageFormat.format("Session invalid: {0}", e.getMessage()));
			String json = new Gson().toJson(root);
			return status(Status.UNAUTHORIZED).entity(json).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			String msg = e.getMessage();
			JsonObject root = new JsonObject();
			root.addProperty("msg", MessageFormat.format("Session invalid: {0}: {1}", e.getClass().getName(), msg));
			String json = new Gson().toJson(root);
			return serverError().entity(json).build();
		}
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{authToken}")
	public Response refreshSession(@Context HttpServletRequest request, @PathParam("authToken") String authToken) {

		try {

			String source = getRemoteIp(request);
			PrivilegeContext privilegeContext = App.getInstance().getUserHandler().validate(authToken, source);
			PrivilegeContext refreshedCtx = App.getInstance().getUserHandler()
					.refreshSession(privilegeContext.getCertificate(), source);

			return getAuthenticationResponse(request, refreshedCtx, true);

		} catch (PrivilegeException e) {
			logger.error("Session validation failed: " + e.getMessage());
			JsonObject root = new JsonObject();
			root.addProperty("msg", MessageFormat.format("Session invalid: {0}", e.getMessage()));
			String json = new Gson().toJson(root);
			return status(Status.UNAUTHORIZED).entity(json).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			String msg = e.getMessage();
			JsonObject root = new JsonObject();
			root.addProperty("msg", MessageFormat.format("Session invalid: {0}: {1}", e.getClass().getName(), msg));
			String json = new Gson().toJson(root);
			return serverError().entity(json).build();
		}
	}

	private Response getAuthenticationResponse(HttpServletRequest request, PrivilegeContext privilegeContext,
			boolean setCookies) {

		int sessionMaxKeepAliveMinutes = App.getInstance().getUserHandler().getSessionMaxKeepAliveMinutes();
		int cookieMaxAge;
		Certificate certificate = privilegeContext.getCertificate();
		if (certificate.isKeepAlive()) {
			cookieMaxAge = (int) TimeUnit.MINUTES.toSeconds(sessionMaxKeepAliveMinutes);
		} else {
			cookieMaxAge = App.getInstance().getUserHandler().getCookieMaxAge();
		}

		LocalDateTime expirationDate = LocalDateTime.now().plusSeconds(cookieMaxAge);
		String expirationDateS = ISO8601.toString(expirationDate);

		JsonObject loginResult = new JsonObject();

		loginResult.addProperty("sessionId", certificate.getSessionId());
		String authToken = certificate.getAuthToken();
		loginResult.addProperty("authToken", authToken);
		loginResult.addProperty("username", certificate.getUsername());
		loginResult.addProperty("firstname", certificate.getFirstname());
		loginResult.addProperty("lastname", certificate.getLastname());
		loginResult.addProperty("locale", certificate.getLocale().toLanguageTag());
		loginResult.addProperty("keepAlive", certificate.isKeepAlive());
		loginResult.addProperty("keepAliveMinutes", sessionMaxKeepAliveMinutes);
		loginResult.addProperty("cookieMaxAge", cookieMaxAge);
		loginResult.addProperty("authorizationExpiration", expirationDateS);
		loginResult.addProperty("refreshAllowed", App.getInstance().getUserHandler().isRefreshAllowed());

		if (!certificate.getPropertyMap().isEmpty()) {
			JsonObject propObj = new JsonObject();
			loginResult.add("properties", propObj);
			for (String propKey : certificate.getPropertyMap().keySet()) {
				propObj.addProperty(propKey, certificate.getPropertyMap().get(propKey));
			}
		}

		if (!certificate.getUserRoles().isEmpty()) {
			JsonArray rolesArr = new JsonArray();
			loginResult.add("roles", rolesArr);
			for (String role : certificate.getUserRoles()) {
				rolesArr.add(new JsonPrimitive(role));
			}
		}

		if (!privilegeContext.getPrivilegeNames().isEmpty()) {
			JsonArray privArr = new JsonArray();
			loginResult.add("privileges", privArr);

			for (String name : privilegeContext.getPrivilegeNames()) {
				IPrivilege privilege = privilegeContext.getPrivilege(name);

				JsonObject privObj = new JsonObject();
				privArr.add(privObj);

				privObj.addProperty("name", name);
				privObj.addProperty("allAllowed", privilege.isAllAllowed());

				Set<String> allowSet = privilege.getAllowList();
				if (!allowSet.isEmpty()) {
					JsonArray allowArr = new JsonArray();
					privObj.add("allowList", allowArr);
					for (String allow : allowSet) {
						allowArr.add(new JsonPrimitive(allow));
					}
				}
			}
		}

		boolean secureCookie = request.getScheme().equals("https");

		if (setCookies) {
			NewCookie authCookie = new NewCookie(HttpHeaders.AUTHORIZATION, authToken, "/", null,
					"Authorization header", cookieMaxAge, secureCookie);
			NewCookie authExpirationCookie = new NewCookie(AUTHORIZATION_EXPIRATION_DATE, expirationDateS, "/", null,
					"Authorization Expiration Date", cookieMaxAge, secureCookie);

			return ok().entity(loginResult.toString()) //
					.header(HttpHeaders.AUTHORIZATION, authToken) //
					.cookie(authCookie) //
					.cookie(authExpirationCookie) //
					.build();
		}

		return ok().entity(loginResult.toString()).header(HttpHeaders.AUTHORIZATION, authToken).build();
	}
}
