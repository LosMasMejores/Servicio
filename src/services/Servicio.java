package services;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/servicio")
@Singleton
public class Servicio {
	
	final static String LOCALHOST = "localhost:8080";
	
	Map<Integer, Proceso> procesos = new HashMap<>();
	Map<Integer, Thread> threads = new HashMap<>();
	Map<Integer, String> informacion = new HashMap<>();
	
	
	@Path("/arrancar")
	@POST
	public Response arrancar(@DefaultValue("0") @QueryParam(value="id") int id) {
				
		if (id <= 0) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		Proceso proceso = procesos.get(id);
		Thread thread = threads.get(id);
		
		if (proceso == null) {
			proceso = new Proceso(id, informacion);
			thread = new Thread(proceso);
			
			procesos.put(id, proceso);
			threads.put(id, thread);
			informacion.put(id, LOCALHOST);
			
			proceso.arrancar();
			thread.start();
			
			return Response.ok().build();
		}
		
		if (thread == null) {
			thread = new Thread(proceso);
			threads.put(id, thread);
			
			proceso.arrancar();
			thread.start();
			
			return Response.ok().build();
		}
		
		if (!thread.isAlive()) {
			thread = new Thread(proceso);
			threads.put(id, thread);
			
			proceso.arrancar();
			thread.start();
			
			return Response.ok().build();
		}
		
		return Response.status(Response.Status.BAD_REQUEST).build();
		
	}
	
	
	@Path("/parar")
	@POST
	public Response parar(@DefaultValue("0") @QueryParam(value="id") int id) {
		
		Proceso proceso = procesos.get(id);
		Thread thread = threads.get(id);
		
		if (proceso == null || thread == null) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		if (thread.isAlive()) {
			proceso.parar();
			return Response.ok().build();
		}
		
		return Response.status(Response.Status.BAD_REQUEST).build();
		
	}
	

	@Path("/informar")
	@POST
	public Response actualizar(@DefaultValue("0") @QueryParam(value="id") int id, 
			@DefaultValue("") @QueryParam(value="server") String server) {
		
		if (server.contains(" ")) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		String[] string = server.split(":");
				
		if (id <= 0 || string.length != 2) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		if (string[0].toUpperCase().equals(LOCALHOST)) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		try {
			if (Integer.parseInt(string[1]) <= 0) {
				return Response.status(Response.Status.BAD_REQUEST).build();
			}
		} catch(NumberFormatException e) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		informacion.put(id, server);
		
		for (Map.Entry<Integer, String> entry : informacion.entrySet()) {
			System.out.println("Id: " + entry.getKey() + ", Server: " + entry.getValue());
		}
		
		return Response.ok().build();
		
	}
	
	
	@Path("/computar")
	@GET
	public Response computar(@DefaultValue("0") @QueryParam(value="id") int id) {
		
		Proceso proceso = procesos.get(id);
		
		if (proceso == null) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		return Response.ok().entity(proceso.computar()).build();
		
	}
	
	
	@Path("/ok")
	@POST
	public Response ok(@DefaultValue("0") @QueryParam(value="id") int id) {
		
		Proceso proceso = procesos.get(id);
		Thread thread = threads.get(id);
		
		if (proceso == null) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		if (!thread.isAlive()) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		synchronized (proceso) {
					
			proceso.ok();
			proceso.notifyAll();

		}
		
		return Response.ok().build();
		
	}
	
	
	@Path("/eleccion")
	@POST
	public Response eleccion(@DefaultValue("0") @QueryParam(value="id") int id, 
			@DefaultValue("0") @QueryParam(value="candidato") int candidato) {
		
		Proceso proceso = procesos.get(id);
		Thread thread = threads.get(id);
		String server = informacion.get(candidato);
		
		if (proceso == null) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		if (server == null) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		if (!thread.isAlive()) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		proceso.eleccion(candidato);
		
		return Response.ok().build();
	}
	
	
	@Path("/coordinador")
	@POST
	public Response coordinador(@DefaultValue("0") @QueryParam(value="id") int id, 
			@DefaultValue("0") @QueryParam(value="coordinador") int coordinador) {
		
		Proceso proceso = procesos.get(id);
		Thread thread = threads.get(id);
		String servidor = informacion.get(coordinador);
		
		if (proceso == null) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		if (servidor == null) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		if (!thread.isAlive()) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		synchronized (proceso) {
		
			proceso.coordinador(coordinador);
			proceso.notifyAll();
			
		}
		
		return Response.ok().build();
		
	}
}

