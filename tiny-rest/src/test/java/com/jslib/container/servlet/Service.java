package com.jslib.container.servlet;

import jakarta.ejb.Remote;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@Remote
@Path("/")
@Produces("application/json")
public class Service {
	@POST
	@Path("name")
	public void postName(String name) {
	}

	@GET
	@Path("name")
	public String getName() {
		return "Jane Doe";
	}

	@POST
	@Path("call/{phone}")
	public void call(@PathParam("phone") String phone) {
	}
}
