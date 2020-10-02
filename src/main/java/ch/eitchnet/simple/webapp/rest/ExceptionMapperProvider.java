/*
 * Copyright 2015 Robert von Burg <eitch@eitchnet.ch>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.eitchnet.simple.webapp.rest;

import static li.strolch.utils.helper.ExceptionHelper.formatExceptionMessage;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.text.MessageFormat;

import com.google.gson.JsonObject;
import li.strolch.privilege.base.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class ExceptionMapperProvider implements ExceptionMapper<Exception> {

	private static final Logger logger = LoggerFactory.getLogger(ExceptionMapperProvider.class);

	@Override
	public Response toResponse(Exception ex) {

		logger.error(MessageFormat.format("Handling exception {0}", ex.getClass()), ex); //$NON-NLS-1$

		if (ex instanceof NotFoundException)
			return Response.status(Status.NOT_FOUND).entity(buildJson("Not Found"))
					.type(MediaType.APPLICATION_JSON_TYPE).build();

		if (ex instanceof AccessDeniedException) {
			return Response.status(Status.UNAUTHORIZED).entity(buildJson("Access Denied"))
					.type(MediaType.APPLICATION_JSON_TYPE).build();
		}

		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(buildJson(formatExceptionMessage(ex)))
				.type(MediaType.APPLICATION_JSON_TYPE).build();
	}

	private String buildJson(String message) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("msg", message);
		return jsonObject.toString();
	}
}