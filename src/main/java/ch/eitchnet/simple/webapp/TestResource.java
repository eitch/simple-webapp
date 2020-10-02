package ch.eitchnet.simple.webapp;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("test")
public class TestResource {

	private static final Logger logger = LoggerFactory.getLogger(TestResource.class);

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response query(@Context HttpServletRequest request, @DefaultValue("") @QueryParam("query") String query,
			@DefaultValue("0") @QueryParam("offset") int offset, @DefaultValue("20") @QueryParam("limit") int limit) {

		String sql = "select id, description, created from simple";

		JsonArray array = new JsonArray();

		try (Connection con = DbPool.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(sql)) {

			try (ResultSet result = statement.executeQuery()) {
				while (result.next()) {
					int id = result.getInt("id");
					String description = result.getString("description");
					ZonedDateTime created = ZonedDateTime
							.ofInstant(result.getTimestamp("created").toInstant(), ZoneId.systemDefault());

					JsonObject jsonObject = new JsonObject();
					jsonObject.addProperty("id", id);
					jsonObject.addProperty("description", description);
					jsonObject.addProperty("created", created.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
					array.add(jsonObject);
				}
			}
		} catch (Exception e) {
			logger.error("Failed to read from table simple", e);
			return Response
					.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "SQL failed: " + e.getMessage())
					.build();
		}

		return Response.ok(array.toString(), MediaType.APPLICATION_JSON).build();
	}
}
