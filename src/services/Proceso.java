package services;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

public class Proceso implements Runnable {

	static public enum Estado {
		CORRIENDO, PARADO
	}
	
	static public enum Eleccion {
		ACUERDO, ELECCION_ACTIVA, ELECCION_PASIVA
	}

	int id, coordinador;
	Estado estado;
	Eleccion eleccion;
	Semaphore sem_ok, sem_coord;
	Map<Integer, String> informacion;
	
	
	public Proceso(int id, Map<Integer, String> informacion) {
		
		this.id = id;
		this.coordinador = 0;
		this.estado = Estado.PARADO;
		this.eleccion = Eleccion.ACUERDO;
		this.sem_ok = new Semaphore(1);
		this.sem_coord = new Semaphore(1);
		this.informacion = informacion;
		
	}
	
	
//	public int getId() {
//		
//		return this.id;
//		
//	}
//	
//	
//	public int getCoordinador() {
//		
//		return this.coordinador;
//		
//	}
//	
//
//	public Estado getEstado() {
//		
//		return this.estado;
//		
//	}
//	
//	
//	public Eleccion getEleccion() {
//		
//		return this.eleccion;
//		
//	}

	
	public void arrancar() {

		this.coordinador = 0;
		this.estado = Estado.CORRIENDO;
		System.out.println(this.id + " arrancado");

	}

	
	public void parar() {

		this.estado = Estado.PARADO;
		System.out.println(this.id + " parado");

	}

	
	public String computar() {
		
		if (this.estado == Estado.PARADO) {
			return "-1";
		} else {
			try {
				Thread.sleep((long) (Math.random() * 100 + 200));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			return "1";
		}

	}
	
	
	public void ok() {
		
		this.sem_ok.release();
		System.out.println(this.id + " ok");
				
		return;
		
	}

	
	public void eleccion() {
		
		boolean has_message = false,
				at_least_one;
		
		while (true) {
			
			this.eleccion = Eleccion.ELECCION_ACTIVA;
			System.out.println(this.id + " eleccion activa");
			
			this.sem_ok.drainPermits();
			this.sem_coord.drainPermits();
			
			at_least_one = false;
									
			for (Map.Entry<Integer, String> entry : this.informacion.entrySet()) {
				
				if (entry.getKey() <= id) {
					continue;
				} else {
					at_least_one = true;
				}
								
				new Thread(new Runnable() {
					public void run() {
												
						Client client = ClientBuilder.newClient();						
						URI uri = UriBuilder.fromUri("http://" + entry.getValue() + "/Servicio").build();
						WebTarget target = client.target(uri);
	
						target.path("servicio")
								.path("eleccion")
								.queryParam("id", entry.getKey())
								.queryParam("candidato", id)
								.request(MediaType.TEXT_PLAIN)
								.post(null);
						
						System.out.println(id + " eleccion() to: " + entry.getKey());
						
						return;
						
					}
				}).start();
								
			}
				
			if (at_least_one) {
			
				try {
					has_message = this.sem_ok.tryAcquire(1000, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				
			} else {
				has_message = false;
			}
										
			if (has_message) {
				
				this.eleccion = Eleccion.ELECCION_PASIVA;
				System.out.println(this.id + " eleccion pasiva");
				
				try {
					has_message = this.sem_coord.tryAcquire(1000, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
								
				if (has_message) {
					this.eleccion = Eleccion.ACUERDO;
					System.out.println(this.id + " acuerdo");
					
					return;
				} else {
					continue;
				}
				
			} else {
				
				for (Map.Entry<Integer, String> entry : this.informacion.entrySet()) {
										
					new Thread(new Runnable() {
						public void run() {
														
							Client client = ClientBuilder.newClient();
							URI uri = UriBuilder.fromUri("http://" + entry.getValue() + "/Servicio").build();
							WebTarget target = client.target(uri);
		
							target.path("servicio")
								.path("coordinador")
								.queryParam("id", entry.getKey())
								.queryParam("coordinador", id)
								.request(MediaType.TEXT_PLAIN)
								.post(null);
							
							System.out.println(id + " coordinador() to: " + entry.getKey());

							return;
							
						}
					}).start();
					
					
					this.eleccion = Eleccion.ACUERDO;
					System.out.println(this.id + " acuerdo");
					
				}
				
				return;
				
			}
		}
		
	}
	
	
	public void eleccion(int candidato) {
		
		Client client = ClientBuilder.newClient();
		URI uri = UriBuilder.fromUri("http://" + informacion.get(candidato) + "/Servicio").build();
		WebTarget target = client.target(uri);

		target.path("servicio")
			.path("ok")
			.queryParam("id", candidato)
			.request(MediaType.TEXT_PLAIN)
			.post(null);
		
		System.out.println(id + " eleccion() from: " + candidato);
		
		this.eleccion();
		
		return;
		
	}
	
	
	public void coordinador(int coordinador) {
		
		this.coordinador = coordinador;
		this.sem_coord.release();
		System.out.println(id + " coordinador() from: " + coordinador);
		
		return;
		
	}

	
	public void run() {

		while (true) {
			
			if (this.estado == Estado.PARADO) {
				return;
			}
			
			try {
				Thread.sleep((long) (Math.random() * 500 + 500));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			String string = informacion.get(coordinador);
			
			if (string == null) {
				System.out.println(this.id + " run error 1");
				eleccion();
				continue;
			}
			
			Client client = ClientBuilder.newClient();
			
			URI uri = UriBuilder.fromUri("http://" + string + "/Servicio").build();
			WebTarget target = client.target(uri);

			Response response = target.path("servicio")
					.path("computar")
					.queryParam("id", coordinador)
					.request(MediaType.TEXT_PLAIN)
					.get();

			String resultado = response.readEntity(String.class);
			
			if (resultado == null || response.getStatus() != 200) {
				System.out.println(this.id + " run error 2");
				eleccion();
				continue;
			}
			
			if (resultado.contains("-1")) {
				System.out.println(this.id + " run error 3");
				eleccion();
			}
			
		}
		
	}

}
