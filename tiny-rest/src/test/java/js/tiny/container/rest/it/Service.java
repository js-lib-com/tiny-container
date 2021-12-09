package js.tiny.container.rest.it;

import javax.ejb.Remote;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

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
