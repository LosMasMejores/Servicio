package services;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/servicio")
@Singleton
public class Servicio {
	
	Map<Integer, Proceso> procesos = new HashMap<>();
	
	@Path("/arrancar")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String arrancar(@DefaultValue("0") @QueryParam(value="id") int id) {
		
		if (id <= 0) {
			return "ERROR";
		}
		
		Proceso proceso = procesos.get(id);
		
		if (proceso == null) {
			proceso = new Proceso(id);
			procesos.put(id, proceso);
			proceso.arrancar();
			proceso.run();
			
			return "OK";
		}
		
		if (proceso.getEstado() == Proceso.Estado.PARADO) {
			proceso.arrancar();
			proceso.run();
			
			return "OK";
		}
		
		return "ERROR";
		
	}
	
	@Path("/parar")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String parar(@DefaultValue("0") @QueryParam(value="id") int id) {
		
		Proceso proceso = procesos.get(id);
		
		if (proceso == null) {
			return "ERROR";
		}
		
		if (proceso.getEstado() == Proceso.Estado.CORRIENDO) {
			proceso.parar();
			
			return "OK";
		}
		
		return "ERROR";
		
	}

}

