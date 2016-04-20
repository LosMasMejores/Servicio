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
	Map<Integer, Thread> threads = new HashMap<>();
	
	@Path("/arrancar")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String arrancar(@DefaultValue("0") @QueryParam(value="id") int id) {
		
		if (id <= 0) {
			return "ERROR";
		}
		
		Proceso proceso = procesos.get(id);
		Thread thread = threads.get(id);
		
		if (proceso == null && thread == null) {
			proceso = new Proceso(id);
			procesos.put(id, proceso);
			thread = new Thread(proceso);
			threads.put(id, thread);
			
			proceso.arrancar();
			thread.start();
			
			return "OK";
		}
		
		if (proceso.getEstado() == Proceso.Estado.PARADO && !thread.isAlive()) {
			thread = new Thread(proceso);
			threads.put(id, thread);
			
			proceso.arrancar();
			thread.start();
			
			return "OK";
		}
		
		return "ERROR";
		
	}
	
	@Path("/parar")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String parar(@DefaultValue("0") @QueryParam(value="id") int id) {
		
		Proceso proceso = procesos.get(id);
		Thread thread = threads.get(id);
		
		if (proceso == null || thread == null) {
			return "ERROR";
		}
		
		if (proceso.getEstado() == Proceso.Estado.CORRIENDO && thread.isAlive()) {
			proceso.parar();
			
			return "OK";
		}
		
		return "ERROR";
		
	}

}

